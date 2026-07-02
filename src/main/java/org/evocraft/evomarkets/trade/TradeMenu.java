package org.evocraft.evomarkets.trade;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.evocraft.evomarkets.init.MarketMenuTypes;
import org.jetbrains.annotations.NotNull;

public class TradeMenu extends AbstractContainerMenu {
    private final SimpleContainer myItems;
    private final SimpleContainer theirItems;
    private final Player player;
    public final boolean isP1;

    public TradeMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        // FIX: Citim din buffer cine este cine, nu mai fortam pe true pentru toata lumea!
        this(id, inv, new SimpleContainer(9), new SimpleContainer(9), buf != null && buf.readBoolean());
    }

    public TradeMenu(int id, Inventory inv, SimpleContainer myItems, SimpleContainer theirItems, boolean isP1) {
        super(MarketMenuTypes.TRADE_MENU.get(), id);
        this.myItems = myItems;
        this.theirItems = theirItems;
        this.player = inv.player;
        this.isP1 = isP1;

        // Grila pentru TINE (3x3 = 9 Iteme)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                this.addSlot(new Slot(myItems, c + r * 3, 20 + c * 18, 30 + r * 18));
            }
        }

        // Grila pentru EL (3x3 = 9 Iteme)
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                this.addSlot(new Slot(theirItems, c + r * 3, 266 + c * 18, 30 + r * 18) {
                    @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
                    @Override public boolean mayPickup(Player playerIn) { return false; }
                });
            }
        }

        // --- FIX MILIMETRIC: Inventarul Jucatorului aliniat perfect cu chenarul ---
        int invX = 89;
        int invY = 148; // Modificat pentru a centra perfect sloturile în cutia de 84px inaltime
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, invX + j * 18, invY + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, invX + i * 18, invY + 58));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && player instanceof ServerPlayer sp) {
            TradeManager.TradeSession session = TradeManager.get().getSession(sp);
            if (session != null && !session.isFinished) {
                session.cancelTrade("Jucatorul a inchis meniul.");
            }
        }
    }

    @Override
    public boolean stillValid(Player player) { return true; }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack1 = slot.getItem();
            itemstack = stack1.copy();

            if (index < 9) { // Din trade (partea ta) in inventar
                if (!this.moveItemStackTo(stack1, 18, 54, true)) return ItemStack.EMPTY;
            }
            else if (index < 18) { // Din trade (partea lui) -> Interzis
                return ItemStack.EMPTY;
            }
            else { // Din inventar in trade
                if (!this.moveItemStackTo(stack1, 0, 9, false)) return ItemStack.EMPTY;
            }

            if (stack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (stack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack1);
        }
        return itemstack;
    }
}