package org.evocraft.evomarkets;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import org.evocraft.evomarkets.entity.SpawnDealerCommand;
import org.evocraft.evomarkets.entity.SpawnItemShopCommand;
import org.evocraft.evomarkets.entity.SpawnAuctionCommand;
import org.evocraft.evomarkets.entity.SpawnPlaneDealerCommand;

import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;
import org.evocraft.evomarkets.init.MarketMenuTypes;
import org.evocraft.evomarkets.shop.*;
import org.evocraft.evomarkets.ah.AuctionMarketManager;
import org.evocraft.evomarkets.ah.AuctionStationManager;
import org.evocraft.evomarkets.ah.AuctionHistoryManager;
import org.evocraft.evomarkets.trade.TradeManager;

import org.evocraft.evomarkets.ah.AuctionCommand;
import org.evocraft.evomarkets.trade.TradeCommand;

import org.evocraft.evomarkets.entity.EvoMarketsEntities;

@Mod(EvoMarkets.MODID)
public class EvoMarkets {

    public static final String MODID = "evomarkets";

    public EvoMarkets() {
        IEventBus modEventBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();

        EvoMarketsEntities.ENTITIES.register(modEventBus);
        MarketMenuTypes.register(modEventBus);

        modEventBus.addListener(this::setup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            EvoMarketsPacketHandler.register();
        });
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(MarketMenuTypes.SHOP_MENU.get(), org.evocraft.evomarkets.shop.ShopScreen::new);
            MenuScreens.register(MarketMenuTypes.AUCTION_MENU.get(), org.evocraft.evomarkets.ah.AuctionScreen::new);
            MenuScreens.register(MarketMenuTypes.TRADE_MENU.get(), org.evocraft.evomarkets.trade.TradeScreen::new);
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CarShopCommand.register(event.getDispatcher());
        CarShopAdminCommand.register(event.getDispatcher());
        SpawnDealerCommand.register(event.getDispatcher());

        org.evocraft.evomarkets.shop.PlaneShopCommand.register(event.getDispatcher());
        org.evocraft.evomarkets.shop.PlaneShopAdminCommand.register(event.getDispatcher());
        org.evocraft.evomarkets.entity.SpawnPlaneDealerCommand.register(event.getDispatcher());

        SpawnItemShopCommand.register(event.getDispatcher());
        SpawnAuctionCommand.register(event.getDispatcher());
        ShopCommand.register(event.getDispatcher());
        AuctionCommand.register(event.getDispatcher());
        TradeCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        CarShopManager.load();
        PlaneShopManager.load();

        ShopConfigManager.initialize();
        ShopStationManager.initialize();
        AuctionMarketManager.initialize();
        AuctionStationManager.initialize();
        AuctionHistoryManager.get();
        TradeManager.get();
    }

    // AM ȘTERS COMPLET BLOCUL "ClientEvents" DE AICI PENTRU CĂ AVEA "NoopRenderer" ȘI NE FĂCEA NPC-URILE INVIZIBILE.
    // ÎNREGISTRAREA CORECTĂ SE FACE DEJA ÎN FIȘIERUL "EntityClientSetup.java".
}