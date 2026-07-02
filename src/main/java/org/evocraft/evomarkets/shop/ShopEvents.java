package org.evocraft.evomarkets.shop;

import org.evocraft.evomarkets.EvoMarkets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = EvoMarkets.MODID)
public class ShopEvents {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ShopConfigManager.get().syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        if (ShopStationManager.get().isStation(level, pos)) {
            if (player instanceof ServerPlayer serverPlayer) {
                MenuProvider container = new SimpleMenuProvider(
                        (id, inventory, p) -> new ShopMenu(id, inventory),
                        Component.literal("Shop")
                );
                NetworkHooks.openScreen(serverPlayer, container, pos);
            }
            event.setCanceled(true);
        }
    }
}