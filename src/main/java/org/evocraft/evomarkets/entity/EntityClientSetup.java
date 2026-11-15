package org.evocraft.evomarkets.entity;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.evocraft.evomarkets.EvoMarkets;
import org.evocraft.evomarkets.ah.AuctionScreen;
import org.evocraft.evomarkets.init.MarketMenuTypes;
import org.evocraft.evomarkets.shop.ShopScreen;
import org.evocraft.evomarkets.trade.TradeScreen;

@Mod.EventBusSubscriber(modid = EvoMarkets.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class EntityClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MarketMenuTypes.SHOP_MENU.get(), ShopScreen::new);
            MenuScreens.register(MarketMenuTypes.AUCTION_MENU.get(), AuctionScreen::new);
            MenuScreens.register(MarketMenuTypes.TRADE_MENU.get(), TradeScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EvoMarketsEntities.CAR_DEALER.get(), CarDealerRenderer::new);
        event.registerEntityRenderer(EvoMarketsEntities.ITEM_SHOP_NPC.get(), ItemShopRenderer::new);

        // AICI ERA LIPSA! L-AM ADĂUGAT ÎNAPOI!
        event.registerEntityRenderer(EvoMarketsEntities.AUCTION_NPC.get(), AuctionNpcRenderer::new);

        event.registerEntityRenderer(EvoMarketsEntities.PLANE_DEALER.get(), PlaneDealerRenderer::new);
    }
}