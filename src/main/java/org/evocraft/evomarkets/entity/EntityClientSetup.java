package org.evocraft.evomarkets.entity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.evocraft.evomarkets.EvoMarkets;

@Mod.EventBusSubscriber(modid = EvoMarkets.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EntityClientSetup {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EvoMarketsEntities.CAR_DEALER.get(), CarDealerRenderer::new);
        event.registerEntityRenderer(EvoMarketsEntities.ITEM_SHOP_NPC.get(), ItemShopRenderer::new);

        // AICI ERA LIPSA! L-AM ADĂUGAT ÎNAPOI!
        event.registerEntityRenderer(EvoMarketsEntities.AUCTION_NPC.get(), AuctionNpcRenderer::new);

        event.registerEntityRenderer(EvoMarketsEntities.PLANE_DEALER.get(), PlaneDealerRenderer::new);
    }
}