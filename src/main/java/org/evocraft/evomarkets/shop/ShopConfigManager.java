package org.evocraft.evomarkets.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.evocraft.evomarkets.network.EvoMarketsPacketHandler; // Reteaua noua
import org.slf4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShopConfigManager {
    private static ShopConfigManager INSTANCE;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final File CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("evo_markets_shops").toFile();

    public ShopData data = new ShopData();

    public static class ShopData {
        public List<ShopCategory> categories = new ArrayList<>();
    }

    public static class ShopCategory {
        public String id;
        public String name;
        public String iconItem;
        public List<ShopItem> items = new ArrayList<>();
    }

    public static class ShopItem {
        public transient ItemStack stack;
        public String itemId;
        public int count;
        public double buyPrice;
        public double sellPrice;
        public String nbt;

        public ItemStack getSafeStack() {
            if (stack == null || stack.isEmpty()) {
                return getParsedStackFromData();
            }
            return stack.copy();
        }

        public ItemStack getParsedStackFromData() {
            net.minecraft.world.item.Item mcItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(this.itemId));
            if (mcItem == null) return ItemStack.EMPTY;

            ItemStack newStack = new ItemStack(mcItem, this.count);
            if (this.nbt != null && !this.nbt.isEmpty()) {
                try {
                    CompoundTag tag = TagParser.parseTag(this.nbt);
                    newStack.setTag(tag);
                } catch (Exception e) {
                    LOGGER.error("Eroare la parsarea NBT-ului pentru itemul: {}", this.itemId, e);
                }
            }
            return newStack;
        }
    }

    public static void initialize() {
        if (INSTANCE == null) INSTANCE = new ShopConfigManager();
    }

    public static ShopConfigManager get() {
        if (INSTANCE == null) INSTANCE = new ShopConfigManager();
        return INSTANCE;
    }

    public ShopConfigManager() {
        load();
    }

    public void load() {
        if (!CONFIG_DIR.exists()) {
            if (!CONFIG_DIR.mkdirs()) LOGGER.warn("Nu s-a putut crea folderul pentru magazine: {}", CONFIG_DIR.getAbsolutePath());
            generateDefaults();
            return;
        }

        data.categories.clear();

        File[] files = CONFIG_DIR.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null || files.length == 0) {
            generateDefaults();
            return;
        }

        for (File file : files) {
            try (Reader reader = new FileReader(file)) {
                ShopCategory category = GSON.fromJson(reader, ShopCategory.class);
                if (category != null) {
                    for (ShopItem item : category.items) {
                        item.stack = parseStack(item);
                    }
                    data.categories.add(category);
                }
            } catch (IOException e) {
                LOGGER.error("Eroare la incarcarea fisierului de categorie: {}", file.getName(), e);
            }
        }
    }

    public void loadFromJson(String json) {
        data = GSON.fromJson(json, ShopData.class);
        if (data != null && data.categories != null) {
            for (ShopCategory cat : data.categories) {
                for (ShopItem item : cat.items) {
                    item.stack = parseStack(item);
                }
            }
        }
    }

    public String toJson() {
        return GSON.toJson(data);
    }

    public void save() {
        if (!CONFIG_DIR.exists() && !CONFIG_DIR.mkdirs()) {
            LOGGER.warn("Nu s-a putut crea folderul de magazine la salvare!");
        }

        for (ShopCategory cat : data.categories) {
            File file = new File(CONFIG_DIR, cat.id + ".json");
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(cat, writer);
            } catch (IOException e) {
                LOGGER.error("Eroare la salvarea categoriei: {}", cat.id, e);
            }
        }
    }

    public void syncToAll() {
        String json = toJson();
        EvoMarketsPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new EvoMarketsPacketHandler.S2C_SyncShop(json));
    }

    public void syncToPlayer(ServerPlayer player) {
        String json = toJson();
        EvoMarketsPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new EvoMarketsPacketHandler.S2C_SyncShop(json));
    }

    public void addCategory(String id, String name, String icon) {
        ShopCategory cat = new ShopCategory();
        cat.id = id;
        cat.name = name;
        cat.iconItem = icon;
        data.categories.add(cat);
        save();
        syncToAll();
    }

    public boolean removeCategory(String id) {
        if (data == null || data.categories == null) return false;
        File file = new File(CONFIG_DIR, id + ".json");

        if (file.exists() && !file.delete()) LOGGER.warn("Nu s-a putut sterge fisierul de categorie: {}", file.getName());

        boolean removed = data.categories.removeIf(c -> c.id.equals(id));
        if (removed) {
            save();
            syncToAll();
        }
        return removed;
    }

    public void addItem(String catId, String itemId, double buy, double sell) {
        ShopCategory cat = getCategory(catId);
        if (cat != null) {
            ShopItem item = new ShopItem();
            item.itemId = itemId;
            item.count = 1;
            item.buyPrice = buy;
            item.sellPrice = sell;
            item.nbt = "";
            item.stack = parseStack(item);
            cat.items.add(item);
            save();
            syncToAll();
        }
    }

    public void addItem(String catId, ItemStack stack, double buy, double sell) {
        ShopCategory cat = getCategory(catId);
        if (cat != null) {
            ShopItem item = new ShopItem();

            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            item.itemId = key != null ? key.toString() : "minecraft:air";
            item.count = stack.getCount();
            item.buyPrice = buy;
            item.sellPrice = sell;

            CompoundTag tag = stack.getTag();
            item.nbt = tag != null ? tag.toString() : "";

            item.stack = stack.copy();
            cat.items.add(item);
            save();
            syncToAll();
        }
    }

    public boolean removeItem(String catId, String itemId) {
        ShopCategory cat = getCategory(catId);
        if (cat != null) {
            boolean removed = cat.items.removeIf(item -> item.itemId.equals(itemId));
            if (removed) {
                save();
                syncToAll();
            }
            return removed;
        }
        return false;
    }

    public ShopCategory getCategory(String id) {
        if (data == null || data.categories == null) return null;
        for (ShopCategory cat : data.categories) {
            if (cat.id.equals(id)) return cat;
        }
        return null;
    }

    public void swapItems(String catId, int from, int to) {
        ShopCategory cat = getCategory(catId);
        if (cat == null) return;
        if (from >= 0 && from < cat.items.size() && to >= 0 && to < cat.items.size()) {
            Collections.swap(cat.items, from, to);
            save();
            syncToAll();
        }
    }

    private ItemStack parseStack(ShopItem item) {
        net.minecraft.world.item.Item mcItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(item.itemId));
        if (mcItem == null) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(mcItem, item.count);

        if (item.nbt != null && !item.nbt.isEmpty()) {
            try {
                CompoundTag tag = TagParser.parseTag(item.nbt);
                stack.setTag(tag);
            } catch (Exception e) {
                LOGGER.error("Eroare la parsarea NBT pentru itemul: {}", item.itemId, e);
            }
        }
        return stack;
    }

    private void generateDefaults() {
        ShopCategory blocks = new ShopCategory();
        blocks.id = "blocuri";
        blocks.name = "Blocuri";
        blocks.iconItem = "minecraft:grass_block";

        ShopItem dirt = new ShopItem();
        dirt.itemId = "minecraft:dirt";
        dirt.count = 64;
        dirt.buyPrice = 100.0;
        dirt.sellPrice = 25.0;
        dirt.nbt = "";
        dirt.stack = parseStack(dirt);

        blocks.items.add(dirt);
        data.categories.add(blocks);
        save();
    }
}