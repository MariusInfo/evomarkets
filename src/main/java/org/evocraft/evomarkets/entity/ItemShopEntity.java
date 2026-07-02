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
import org.evocraft.evomarkets.shop.ShopMenu;

public class ItemShopEntity extends PathfinderMob {

    public ItemShopEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true); // Îngheață NPC-ul
        this.setInvulnerable(true); // Nu ia damage
        this.setCustomName(Component.literal("§e§lShop Iteme")); // Numele deasupra capului
        this.setCustomNameVisible(true);
        this.setPersistenceRequired(); // Nu dispare de pe hartă când pleci
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false; // Previne despawn-ul automat
    }

    // Aici deschidem Shop-ul de Iteme când dai click pe el
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide() && hand == InteractionHand.MAIN_HAND) {
            if (player instanceof ServerPlayer serverPlayer) {
                MenuProvider container = new SimpleMenuProvider(
                        (id, inventory, p) -> new ShopMenu(id, inventory),
                        Component.literal("Shop")
                );
                // Deschidem interfața Shop-ului folosind Forge NetworkHooks
                NetworkHooks.openScreen(serverPlayer, container);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
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