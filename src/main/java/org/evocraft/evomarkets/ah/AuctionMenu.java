package org.evocraft.evomarkets.ah;

import org.evocraft.evocore.data.EconomyManager;
import org.evocraft.evomarkets.init.MarketMenuTypes;
import org.evocraft.evocore.network.PacketHandler;
import org.evocraft.evocore.util.EvoCurrencyFormatter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AuctionMenu extends AbstractContainerMenu {

    private final SimpleContainer container;
    private final Player player;

    public static final int ITEMS_PER_PAGE = 20;

    private final DataSlot pageSlot = DataSlot.standalone();
    private final DataSlot maxPagesSlot = DataSlot.standalone();

    public int maxPages = 0;
    public boolean showMyListings = false;
    public int sortOrder = 0;
    public String searchItem = "";
    public String searchSeller = "";

    public AuctionMenu(int id, Inventory inv, net.minecraft.network.FriendlyByteBuf extraData) {
        this(id, inv);
    }

    public AuctionMenu(int id, Inventory inv) {
        this(id, inv, new SimpleContainer(ITEMS_PER_PAGE));
    }

    public AuctionMenu(int id, Inventory inv, SimpleContainer container) {
        super(MarketMenuTypes.AUCTION_MENU.get(), id);
        this.container = container;
        this.player = inv.player;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            addSlot(new Slot(container, i, -10000, -10000) {
                @Override public boolean mayPickup(Player p) { return false; }
                @Override public boolean mayPlace(ItemStack s) { return false; }
            });
        }

        addDataSlot(pageSlot);
        addDataSlot(maxPagesSlot);

        if (!player.level().isClientSide) {
            populateListings();
        }
    }

    public int getMaxPages() {
        return maxPagesSlot.get();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) return true;
        int page = pageSlot.get();

        if (id == 0) {
            showMyListings = !showMyListings;
            pageSlot.set(0);
            populateListings();
            return true;
        }
        if (id == 1 && page < maxPagesSlot.get()) {
            pageSlot.set(page + 1);
            populateListings();
            return true;
        }
        if (id == 2 && page > 0) {
            pageSlot.set(page - 1);
            populateListings();
            return true;
        }

        return super.clickMenuButton(player, id);
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType type, Player player) {
        if (slotId >= 0 && slotId < ITEMS_PER_PAGE) {
            if (!player.level().isClientSide && player instanceof ServerPlayer sp && !sp.isSpectator()) {
                ItemStack stack = container.getItem(slotId);
                if (!stack.isEmpty() && stack.hasTag()) {
                    CompoundTag tag = stack.getTag();
                    if (tag != null && tag.contains("AH_UUID")) {
                        try {
                            UUID id = UUID.fromString(tag.getString("AH_UUID"));
                            handleBuyRequest(sp, id);
                            populateListings();
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
            return;
        }
        super.clicked(slotId, dragType, type, player);
    }

    private boolean canFit(Inventory inv, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        int leftToAdd = stack.getCount();
        int maxStack = Math.max(1, stack.getMaxStackSize());
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

    private void giveStack(ServerPlayer player, ItemStack itemStack) {
        ItemStack copy = itemStack.copy();
        if (!player.getInventory().add(copy) && !copy.isEmpty()) {
            player.drop(copy, false);
        }
    }

    public void handleBuyRequest(ServerPlayer player, UUID id) {
        if (player == null || id == null || player.isSpectator()) return;
        AuctionMarketManager manager = AuctionMarketManager.get();

        AuctionListing listing;
        boolean reclaim;
        synchronized (manager) {
            listing = manager.getListing(id);
            if (listing == null || listing.itemStack == null || listing.itemStack.isEmpty()) return;

            if (!canFit(player.getInventory(), listing.itemStack)) {
                player.sendSystemMessage(Component.literal("§cInventory full! Clear some space first."));
                return;
            }

            reclaim = player.getUUID().equals(listing.sellerUUID);
            if (reclaim) {
                if (!manager.removeListing(id)) return;
            } else {
                if (!Double.isFinite(listing.price) || listing.price <= 0.0D) return;
                if (!EconomyManager.get().tryWithdraw(player.getUUID(), listing.price)) {
                    player.sendSystemMessage(Component.literal("§cInsufficient funds!"));
                    return;
                }
                if (!manager.removeListing(id)) {
                    EconomyManager.get().addBalance(player.getUUID(), listing.price);
                    return;
                }
            }
        }

        if (reclaim) {
            giveStack(player, listing.itemStack);
            player.sendSystemMessage(Component.literal("§eItem recuperat cu succes."));
            return;
        }

        PacketHandler.sendToPlayer(new PacketHandler.S2C_SyncBalance(EconomyManager.get().getBalance(player.getUUID())), player);

        double tax = 0.0D;
        double profit = listing.price;

        try {
            EconomyManager.get().addBalance(listing.sellerUUID, profit);
        } catch (Exception e) {
            System.out.println("[EvoMarkets] Could not add funds for offline player: " + listing.sellerName);
        }

        giveStack(player, listing.itemStack);
        player.sendSystemMessage(Component.literal("§aYou bought the item!"));

        AuctionHistoryManager.get().addTransaction(
                listing.sellerUUID,
                player.getName().getString(),
                listing.itemStack.getHoverName().getString(),
                listing.price,
                tax
        );

        MinecraftServer server = player.getServer();
        if (server != null) {
            ServerPlayer seller = server.getPlayerList().getPlayer(listing.sellerUUID);
            if (seller != null) {
                seller.sendSystemMessage(Component.literal("§a[AH] Item sold! Received: " + EvoCurrencyFormatter.formatWithCurrency(profit)));
                PacketHandler.sendToPlayer(new PacketHandler.S2C_SyncBalance(EconomyManager.get().getBalance(seller.getUUID())), seller);
            }
        }
    }

    private List<AuctionListing> getFilteredListings() {
        String itemFilter = searchItem != null ? searchItem.toLowerCase() : "";
        String sellerFilter = searchSeller != null ? searchSeller.toLowerCase() : "";
        List<AuctionListing> list = AuctionMarketManager.get().getAllListings()
                .stream()
                .filter(l -> {
                    if (l == null || l.itemStack == null || l.itemStack.isEmpty()) return false;
                    if (showMyListings && !player.getUUID().equals(l.sellerUUID)) return false;
                    if (!itemFilter.isEmpty() && !l.itemStack.getHoverName().getString().toLowerCase().contains(itemFilter)) return false;
                    String sellerName = l.sellerName != null ? l.sellerName.toLowerCase() : "";
                    if (!sellerFilter.isEmpty() && !sellerName.contains(sellerFilter)) return false;
                    return true;
                })
                .collect(Collectors.toList());

        if (sortOrder == 1) list.sort(Comparator.comparingDouble(l -> l.price));
        else if (sortOrder == 2) list.sort((a, b) -> Double.compare(b.price, a.price));
        else java.util.Collections.reverse(list);

        return list;
    }

    public void populateListings() {
        if (this.player.level().isClientSide) return;

        for (int i = 0; i < container.getContainerSize(); i++) container.setItem(i, ItemStack.EMPTY);

        List<AuctionListing> listings = getFilteredListings();

        maxPages = Math.max(0, (int) Math.ceil((double) listings.size() / ITEMS_PER_PAGE) - 1);
        maxPagesSlot.set(maxPages);

        int page = Math.min(pageSlot.get(), maxPages);
        pageSlot.set(page);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, listings.size());

        int slot = 0;

        for (int i = start; i < end; i++) {
            AuctionListing l = listings.get(i);
            ItemStack display = l.itemStack.copy();
            CompoundTag tag = display.getOrCreateTag();

            tag.putString("AH_UUID", l.id.toString());
            tag.putDouble("AH_Price", l.price);
            tag.putString("AH_Seller", l.sellerName != null ? l.sellerName : "Unknown");
            tag.putString("AH_SellerUUID", l.sellerUUID.toString());

            container.setItem(slot++, display);
        }
    }

    @Override public ItemStack quickMoveStack(Player p, int i) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player p) { return !p.isSpectator() && p.isAlive(); }

    public void cycleSort() {
        this.sortOrder++;
        if (this.sortOrder > 2) this.sortOrder = 0;
        populateListings();
    }

    public int getPage() { return pageSlot.get(); }

    public void updateFiltersFromServer(String item, String seller, int sort) {
        this.searchItem = item != null ? item : "";
        this.searchSeller = seller != null ? seller : "";
        this.sortOrder = (sort >= 0 && sort <= 2) ? sort : 0;
        pageSlot.set(0);
        populateListings();
    }
}
