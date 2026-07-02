package org.evocraft.evomarkets.init;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.evocraft.evomarkets.EvoMarkets;
import org.evocraft.evomarkets.shop.ShopMenu;
import org.evocraft.evomarkets.ah.AuctionMenu;
import org.evocraft.evomarkets.trade.TradeMenu; // Importăm meniul de Trade

public class MarketMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, EvoMarkets.MODID);

    // Meniul pentru Shop
    public static final RegistryObject<MenuType<ShopMenu>> SHOP_MENU =
            MENUS.register("shop_menu", () -> IForgeMenuType.create(ShopMenu::new));

    // Meniul pentru Auction House
    public static final RegistryObject<MenuType<AuctionMenu>> AUCTION_MENU =
            MENUS.register("auction_menu", () -> IForgeMenuType.create(AuctionMenu::new));

    // Meniul pentru Trade
    public static final RegistryObject<MenuType<TradeMenu>> TRADE_MENU =
            MENUS.register("trade_menu", () -> IForgeMenuType.create(TradeMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}