package org.evocraft.evomarkets.ah;

import org.evocraft.evomarkets.EvoMarkets;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;

@Mod.EventBusSubscriber(modid = EvoMarkets.MODID)
public class AuctionEvents {

    @SubscribeEvent
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();

        // Dacă blocul este stație AH, deschide meniul AH!
        if (AuctionStationManager.get().isStation(level, pos)) {
            if (player instanceof ServerPlayer serverPlayer) {
                MenuProvider container = new SimpleMenuProvider(
                        (id, inventory, p) -> new AuctionMenu(id, inventory),
                        Component.literal("Auction House")
                );
                NetworkHooks.openScreen(serverPlayer, container, pos);
            }
            event.setCanceled(true);
        }
    }
}