package org.evocraft.evomarkets.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import org.evocraft.evomarkets.ah.AuctionMenu;
import org.evocraft.evomarkets.EvoMarkets;

public class AuctionNpcEntity extends PathfinderMob {

    public AuctionNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setCustomName(Component.literal("§6§lAuctions (AH)"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            ensureEnglishName();
            if (player instanceof ServerPlayer serverPlayer) {
                MenuProvider container = new SimpleMenuProvider(
                        (id, inventory, p) -> new AuctionMenu(id, inventory),
                        Component.literal("Auction House")
                );
                NetworkHooks.openScreen(serverPlayer, container);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide() && this.tickCount % 40 == 0) {
            ensureEnglishName();
        }
    }

    private void ensureEnglishName() {
        String current = this.getDisplayName().getString();
        if (!current.contains("Auctions")) {
            this.setCustomName(Component.literal("§6§lAuctions (AH)"));
            this.setCustomNameVisible(true);
        }
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {}

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }
}
