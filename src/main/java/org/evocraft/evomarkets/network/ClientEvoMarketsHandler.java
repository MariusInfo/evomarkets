package org.evocraft.evomarkets.network;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.evocraft.evomarkets.shop.CarShopScreen;
import org.evocraft.evomarkets.shop.PlaneShopScreen;

@OnlyIn(Dist.CLIENT)
public class ClientEvoMarketsHandler {

    // Serverul nu va citi NICIODATĂ metodele de mai jos, doar clientul!

    public static void openCarShop(String jsonData) {
        Minecraft.getInstance().setScreen(new CarShopScreen(jsonData));
    }

    public static void openPlaneShop(String jsonData) {
        Minecraft.getInstance().setScreen(new PlaneShopScreen(jsonData));
    }

    public static void updateAuction() {
        if (Minecraft.getInstance().screen instanceof org.evocraft.evomarkets.ah.AuctionScreen s) {
            s.refreshFromServer();
        }
    }

    public static void showTradeNotification(String title, String desc) {
        org.evocraft.evomarkets.trade.TradeNotificationHandler.showNotification(title, desc);
    }

    public static void syncTradeData(double m1, double m2, boolean a1, boolean a2, int cd, String c, String n1, String n2) {
        org.evocraft.evomarkets.trade.TradeScreen.syncData(m1, m2, a1, a2, cd, c, n1, n2);
    }
}