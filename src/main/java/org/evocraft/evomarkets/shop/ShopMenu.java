package org.evocraft.evomarkets.shop;

import org.evocraft.evocore.data.EconomyManager;
import org.evocraft.evomarkets.init.MarketMenuTypes;
import org.evocraft.evocore.network.PacketHandler;
import org.evocraft.evocore.util.EvoCurrencyFormatter;
import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class ShopMenu extends AbstractContainerMenu {
    private final SimpleContainer container;
    private final Player player;
    private ShopConfigManager.ShopCategory currentCategory = null;

    private final DataSlot pageSlot = DataSlot.standalone();
    private final DataSlot maxPagesSlot = DataSlot.standalone();
    private final DataSlot categoryIndexSlot = DataSlot.standalone();

    private static final int ITEMS_PER_PAGE = 20;

    public ShopMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(ITEMS_PER_PAGE));
    }

    public ShopMenu(int containerId, Inventory playerInventory, net.minecraft.network.FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(ITEMS_PER_PAGE));
    }

    public ShopMenu(int containerId, Inventory playerInventory, SimpleContainer container) {
        super(MarketMenuTypes.SHOP_MENU.get(), containerId);
        this.container = container;
        this.player = playerInventory.player;

        // Mutam sloturile complet in afara ecranului pentru a preveni bug-uri vizuale
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            this.addSlot(new Slot(container, i, -10000, -10000) {
                @Override public boolean mayPickup(@NotNull Player p) { return false; }
                @Override public boolean mayPlace(@NotNull ItemStack s) { return false; }
            });
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, -10000, -10000));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, -10000, -10000));
        }

        this.addDataSlot(categoryIndexSlot);
        this.addDataSlot(maxPagesSlot);
        this.addDataSlot(pageSlot);

        if (!player.level().isClientSide) {
            loadMainMenu();
            syncMoney();
        }
    }

    public int getPage() { return pageSlot.get(); }
    public int getMaxPages() { return maxPagesSlot.get(); }

    public void syncMoney() {
        if (player instanceof ServerPlayer sp) {
            PacketHandler.sendToPlayer(new PacketHandler.S2C_SyncBalance(EconomyManager.get().getBalance(player.getUUID())), sp);
        }
    }

    public void loadMainMenu() {
        if (currentCategory != null) { pageSlot.set(0); currentCategory = null; }
        categoryIndexSlot.set(-1);
        container.clearContent();

        if (ShopConfigManager.get().data == null) return;
        List<ShopConfigManager.ShopCategory> allCats = ShopConfigManager.get().data.categories;
        if (allCats == null) return;

        allCats.sort(Comparator.comparingInt(c -> ShopConfigManager.get().data.categories.indexOf(c)));
        updatePagination(allCats.size());

        int currentPage = pageSlot.get();
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allCats.size());

        for (int i = start; i < end; i++) {
            ShopConfigManager.ShopCategory cat = allCats.get(i);
            String iconId = (cat.iconItem != null && !cat.iconItem.isEmpty()) ? cat.iconItem : "minecraft:barrier";
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(iconId));
            if (item == null) item = Items.BARRIER;

            ItemStack stack = new ItemStack(item);
            stack.setHoverName(Component.literal(cat.name));
            CompoundTag tag = stack.getOrCreateTag();
            tag.putString("Shop_CatID", cat.id);
            tag.putBoolean("Shop_IsCategoryMenu", true);
            container.setItem(i - start, stack);
        }
    }

    public void loadCategory(ShopConfigManager.ShopCategory cat) {
        if (currentCategory != cat) { pageSlot.set(0); currentCategory = cat; }
        int catIdx = ShopConfigManager.get().data.categories.indexOf(cat);
        categoryIndexSlot.set(catIdx);
        container.clearContent();

        List<ShopConfigManager.ShopItem> allItems = cat.items;
        updatePagination(allItems.size());

        int currentPage = pageSlot.get();
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allItems.size());

        for (int i = start; i < end; i++) {
            ShopConfigManager.ShopItem sItem = allItems.get(i);
            if (sItem.itemId == null) continue;

            ItemStack stack = sItem.getSafeStack();
            if (stack.isEmpty()) stack = new ItemStack(Items.BARRIER);

            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean("Shop_IsItem", true);
            tag.putDouble("Buy_Price", sItem.buyPrice);
            tag.putDouble("Sell_Price", sItem.sellPrice);
            tag.putInt("Real_List_Index", i);

            // Fix antiglont: Punem datele categoriei direct in item
            tag.putString("Shop_CatID", cat.id);
            tag.putString("Shop_CatName", cat.name);

            container.setItem(i - start, stack);
        }
    }

    private void updatePagination(int totalSize) {
        int mPages = (totalSize == 0) ? 0 : (int) Math.ceil((double) totalSize / ITEMS_PER_PAGE) - 1;
        if (mPages < 0) mPages = 0;
        this.maxPagesSlot.set(mPages);
        if (pageSlot.get() > mPages) pageSlot.set(mPages);
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id) {
        int currentMax = maxPagesSlot.get();
        int currentPage = pageSlot.get();

        if (isMainMenu() && id >= 0 && id < ITEMS_PER_PAGE) {
            ItemStack clicked = container.getItem(id);
            if (clicked.hasTag() && clicked.getTag().contains("Shop_CatID")) {
                String catId = clicked.getTag().getString("Shop_CatID");
                ShopConfigManager.ShopCategory cat = ShopConfigManager.get().getCategory(catId);
                if (cat != null) loadCategory(cat);
            }
            return true;
        }

        if (id == 99) { loadMainMenu(); return true; }
        if (id == 101 && currentPage > 0) { pageSlot.set(currentPage - 1); refreshCurrentView(); return true; }
        if (id == 102 && currentPage < currentMax) { pageSlot.set(currentPage + 1); refreshCurrentView(); return true; }
        return false;
    }

    public void refreshCurrentView() {
        if (currentCategory == null) loadMainMenu();
        else loadCategory(currentCategory);
    }

    public void performTransaction(String catId, int itemIndex, boolean isBuy, boolean isSell, int amount) {
        performTransaction(catId, itemIndex, isBuy, isSell, amount, false, false);
    }

    public void performTransaction(String catId, int itemIndex, boolean isBuy, boolean isSell, int amount, boolean includeBackpackItems, boolean skipBackpackPrompt) {
        ShopConfigManager.ShopCategory cat = ShopConfigManager.get().getCategory(catId);
        if (cat == null || itemIndex < 0 || itemIndex >= cat.items.size()) return;

        ShopConfigManager.ShopItem sItem = cat.items.get(itemIndex);
        ItemStack baseStack = sItem.getSafeStack();
        if (baseStack.isEmpty()) return;

        String itemName = baseStack.getHoverName().getString();
        boolean sellAll = amount == -1 && isSell;
        int backpackHas = sellAll ? BackpackSellCompat.countSellableItems(player, baseStack) : 0;

        if (sellAll && backpackHas > 0 && !skipBackpackPrompt && player instanceof ServerPlayer serverPlayer) {
            EvoMarketsPacketHandler.sendToPlayer(new EvoMarketsPacketHandler.S2C_BackpackSellPrompt(catId, itemIndex, backpackHas), serverPlayer);
            return;
        }

        if (amount == -1) {
            if (isBuy) return;
            if (isSell) {
                int totalHas = 0;
                for(ItemStack i : player.getInventory().items) {
                    if(!i.isEmpty() && i.getItem() == baseStack.getItem()) totalHas += i.getCount();
                }
                if (totalHas == 0 && (!includeBackpackItems || backpackHas == 0)) {
                    player.sendSystemMessage(Component.literal("§cYou do not have " + itemName + " in your inventory!"));
                    return;
                }
                amount = totalHas + (includeBackpackItems ? backpackHas : 0);
            }
        }

        if (isBuy) {
            double cost = sItem.buyPrice * amount;
            if (EconomyManager.get().getBalance(player.getUUID()) < cost) {
                player.sendSystemMessage(Component.literal("§cInsufficient funds!"));
                return;
            }

            ItemStack toGiveTest = baseStack.copy();
            toGiveTest.setCount(amount);
            if (!canFit(player.getInventory(), toGiveTest)) {
                player.sendSystemMessage(Component.literal("§cInventory full! You do not have enough space."));
                return;
            }

            EconomyManager.get().removeBalance(player.getUUID(), cost);
            ItemStack toGive = baseStack.copy();
            toGive.setCount(amount);
            player.getInventory().add(toGive);
            player.sendSystemMessage(Component.literal("§aYou bought " + amount + "x " + itemName + " for " + EvoCurrencyFormatter.formatWithCurrency(cost)));
            syncMoney();
        }
        else if (isSell) {
            if (sItem.sellPrice <= 0) return;
            int has = 0;
            for(ItemStack i : player.getInventory().items) {
                if(i.getItem() == baseStack.getItem()) has += i.getCount();
            }
            int inventoryHas = has;
            if (includeBackpackItems) has += backpackHas;

            int toSell = Math.min(has, amount);
            if (toSell > 0) {
                int removed = removeItem(player, baseStack.getItem(), Math.min(inventoryHas, toSell));
                int remaining = toSell - removed;
                if (remaining > 0 && includeBackpackItems) {
                    removed += BackpackSellCompat.removeSellableItems(player, baseStack, remaining);
                }
                if (removed <= 0) {
                    player.sendSystemMessage(Component.literal("§cYou do not have " + itemName + " in your inventory!"));
                    return;
                }
                double profit = sItem.sellPrice * removed;
                EconomyManager.get().addBalance(player.getUUID(), profit);
                player.sendSystemMessage(Component.literal("§aYou sold " + removed + "x " + itemName + " for " + EvoCurrencyFormatter.formatWithCurrency(profit)));
                syncMoney();
            } else {
                player.sendSystemMessage(Component.literal("§cYou do not have " + itemName + " in your inventory!"));
            }
        }
    }

    private boolean canFit(Inventory inv, ItemStack stack) {
        int leftToAdd = stack.getCount();
        int maxStack = stack.getMaxStackSize();
        for (int i = 0; i < 36; i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.isEmpty()) leftToAdd -= maxStack;
            else if (ItemStack.isSameItemSameTags(slot, stack)) {
                int space = maxStack - slot.getCount();
                if (space > 0) leftToAdd -= space;
            }
            if (leftToAdd <= 0) return true;
        }
        return false;
    }

    private int removeItem(Player player, Item item, int count) {
        int left = count;
        for (ItemStack is : player.getInventory().items) {
            if (is.getItem() == item) {
                int take = Math.min(is.getCount(), left);
                is.shrink(take);
                left -= take;
                if (left <= 0) break;
            }
        }
        return count - left;
    }

    @Override public @NotNull ItemStack quickMoveStack(@NotNull Player p, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(@NotNull Player p) { return true; }

    // --- FIX: CITIM CATEGORIA DIRECT DIN ITEM CA SA FIM 100% SIGURI ---
    public boolean isMainMenu() {
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.hasTag()) {
                if (stack.getTag().getBoolean("Shop_IsItem")) return false;
                if (stack.getTag().getBoolean("Shop_IsCategoryMenu")) return true;
            }
        }
        return categoryIndexSlot.get() == -1;
    }

    public String getCategoryName() {
        if (isMainMenu()) return "EVO SHOP";
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.hasTag() && stack.getTag().contains("Shop_CatName")) {
                return stack.getTag().getString("Shop_CatName");
            }
        }
        return "SHOP";
    }

    public String getCurrentCategoryId() {
        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.hasTag() && stack.getTag().contains("Shop_CatID")) {
                return stack.getTag().getString("Shop_CatID");
            }
        }
        return "";
    }
}
