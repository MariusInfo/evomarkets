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
            if (this.itemId == null || this.itemId.isEmpty()) return ItemStack.EMPTY;
            ResourceLocation loc = ResourceLocation.tryParse(this.itemId);
            if (loc == null) return ItemStack.EMPTY;
            net.minecraft.world.item.Item mcItem = ForgeRegistries.ITEMS.getValue(loc);
            if (mcItem == null || mcItem == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;

            ItemStack newStack = new ItemStack(mcItem, Math.max(1, this.count));
            if (this.nbt != null && !this.nbt.isEmpty()) {
                try {
                    CompoundTag tag = TagParser.parseTag(this.nbt);
                    newStack.setTag(tag);
                } catch (Exception e) {
                    LOGGER.error("Error parsing NBT for item: {}", this.itemId, e);
                }
            }
            return newStack;
        }
    }

    public static synchronized void initialize() {
        if (INSTANCE == null) INSTANCE = new ShopConfigManager();
    }

    public static synchronized ShopConfigManager get() {
        if (INSTANCE == null) INSTANCE = new ShopConfigManager();
        return INSTANCE;
    }

    public static boolean isValidCategoryId(String id) {
        return id != null && id.matches("[a-zA-Z0-9_\\-]{1,64}");
    }

    public ShopConfigManager() {
        load();
    }

    public synchronized void load() {
        if (!CONFIG_DIR.exists()) {
            if (!CONFIG_DIR.mkdirs()) LOGGER.warn("Could not create shop folder: {}", CONFIG_DIR.getAbsolutePath());
            generateDefaults();
            return;
        }

        if (data == null) data = new ShopData();
        if (data.categories == null) data.categories = new ArrayList<>();
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
                    if (category.items == null) category.items = new ArrayList<>();
                    for (ShopItem item : category.items) {
                        if (item != null) item.stack = parseStack(item);
                    }
                    data.categories.add(category);
                }
            } catch (Exception e) {
                LOGGER.error("Error loading category file: {}", file.getName(), e);
            }
        }
    }

    public synchronized void loadFromJson(String json) {
        if (json == null || json.isBlank()) return;
        try {
            ShopData loaded = GSON.fromJson(json, ShopData.class);
            if (loaded == null) return;
            if (loaded.categories == null) loaded.categories = new ArrayList<>();
            for (ShopCategory cat : loaded.categories) {
                if (cat == null) continue;
                if (cat.items == null) cat.items = new ArrayList<>();
                for (ShopItem item : cat.items) {
                    if (item != null) item.stack = parseStack(item);
                }
            }
            data = loaded;
        } catch (Exception e) {
            LOGGER.error("Error parsing shop sync JSON", e);
        }
    }

    public synchronized String toJson() {
        return GSON.toJson(data);
    }

    public synchronized void save() {
        if (!CONFIG_DIR.exists() && !CONFIG_DIR.mkdirs()) {
            LOGGER.warn("Could not create shop folder while saving!");
        }
        if (data == null || data.categories == null) return;

        for (ShopCategory cat : data.categories) {
            if (cat == null || !isValidCategoryId(cat.id)) continue;
            File file = new File(CONFIG_DIR, cat.id + ".json");
            try (Writer writer = new FileWriter(file)) {
                GSON.toJson(cat, writer);
            } catch (IOException e) {
                LOGGER.error("Error saving category: {}", cat.id, e);
            }
        }
    }

    public void syncToAll() {
        String json = toJson();
        EvoMarketsPacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new EvoMarketsPacketHandler.S2C_SyncShop(json));
    }

    public void syncToPlayer(ServerPlayer player) {
        if (player == null) return;
        String json = toJson();
        EvoMarketsPacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new EvoMarketsPacketHandler.S2C_SyncShop(json));
    }

    public synchronized boolean addCategory(String id, String name, String icon) {
        if (!isValidCategoryId(id)) return false;
        if (getCategory(id) != null) return false;
        ShopCategory cat = new ShopCategory();
        cat.id = id;
        cat.name = name;
        cat.iconItem = icon;
        data.categories.add(cat);
        save();
        syncToAll();
        return true;
    }

    public synchronized boolean removeCategory(String id) {
        if (data == null || data.categories == null || !isValidCategoryId(id)) return false;
        File file = new File(CONFIG_DIR, id + ".json");

        if (file.exists() && !file.delete()) LOGGER.warn("Could not delete category file: {}", file.getName());

        boolean removed = data.categories.removeIf(c -> c != null && id.equals(c.id));
        if (removed) {
            save();
            syncToAll();
        }
        return removed;
    }

    public synchronized boolean addItem(String catId, String itemId, double buy, double sell) {
        if (!Double.isFinite(buy) || buy < 0 || !Double.isFinite(sell) || sell < 0) return false;
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
            return true;
        }
        return false;
    }

    public synchronized boolean addItem(String catId, ItemStack stack, double buy, double sell) {
        if (stack == null || stack.isEmpty() || !Double.isFinite(buy) || buy < 0 || !Double.isFinite(sell) || sell < 0) return false;
        ShopCategory cat = getCategory(catId);
        if (cat != null) {
            ShopItem item = new ShopItem();

            ResourceLocation key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            item.itemId = key != null ? key.toString() : "minecraft:air";
            item.count = Math.max(1, stack.getCount());
            item.buyPrice = buy;
            item.sellPrice = sell;

            CompoundTag tag = stack.getTag();
            item.nbt = tag != null ? tag.toString() : "";

            item.stack = stack.copy();
            cat.items.add(item);
            save();
            syncToAll();
            return true;
        }
        return false;
    }

    public synchronized boolean removeItem(String catId, String itemId) {
        ShopCategory cat = getCategory(catId);
        if (cat != null && cat.items != null) {
            boolean removed = cat.items.removeIf(item -> item != null && itemId.equals(item.itemId));
            if (removed) {
                save();
                syncToAll();
            }
            return removed;
        }
        return false;
    }

    public synchronized ShopCategory getCategory(String id) {
        if (data == null || data.categories == null || id == null) return null;
        for (ShopCategory cat : data.categories) {
            if (cat != null && id.equals(cat.id)) return cat;
        }
        return null;
    }

    public synchronized void swapItems(String catId, int from, int to) {
        ShopCategory cat = getCategory(catId);
        if (cat == null || cat.items == null) return;
        if (from >= 0 && from < cat.items.size() && to >= 0 && to < cat.items.size()) {
            Collections.swap(cat.items, from, to);
            save();
            syncToAll();
        }
    }

    private ItemStack parseStack(ShopItem item) {
        if (item == null || item.itemId == null || item.itemId.isEmpty()) return ItemStack.EMPTY;
        ResourceLocation loc = ResourceLocation.tryParse(item.itemId);
        if (loc == null) return ItemStack.EMPTY;
        net.minecraft.world.item.Item mcItem = ForgeRegistries.ITEMS.getValue(loc);
        if (mcItem == null || mcItem == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(mcItem, Math.max(1, item.count));

        if (item.nbt != null && !item.nbt.isEmpty()) {
            try {
                CompoundTag tag = TagParser.parseTag(item.nbt);
                stack.setTag(tag);
            } catch (Exception e) {
                LOGGER.error("Error parsing NBT for item: {}", item.itemId, e);
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
