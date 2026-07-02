package org.evocraft.evomarkets.trade;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.evocraft.evomarkets.EvoMarkets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Mod.EventBusSubscriber(modid = EvoMarkets.MODID, value = Dist.CLIENT)
public class TradeNotificationHandler {

    private static class TradeNotif {
        String title;
        String sub;
        String type;
        int ticks;
        int maxTicks;

        public TradeNotif(String title, String sub, String type, int maxTicks) {
            this.title = title;
            this.sub = sub;
            this.type = type;
            this.maxTicks = maxTicks;
            this.ticks = maxTicks;
        }
    }

    private static final List<TradeNotif> activeNotifications = new ArrayList<>();

    public static void showNotification(String type, String data) {
        String title = "";
        String sub = "";
        int time = 200;

        switch (type) {
            case "REQUEST" -> {
                title = "CERERE TRADE";
                sub = "De la: §e" + data + " §f(/trade accept)";
                time = 300;
            }
            case "CANCELLED" -> {
                title = "TRADE ANULAT";
                sub = data;
                time = 100;
            }
            case "SUCCESS" -> {
                title = "TRADE COMPLET";
                sub = "Schimbul a fost efectuat cu succes.";
                time = 100;
            }
        }

        for (TradeNotif existing : activeNotifications) {
            if (existing.type.equals(type) && existing.sub.equals(sub)) {
                existing.ticks = existing.maxTicks;
                return;
            }
        }
        activeNotifications.add(new TradeNotif(title, sub, type, time));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Iterator<TradeNotif> it = activeNotifications.iterator();
            while (it.hasNext()) {
                TradeNotif n = it.next();
                n.ticks--;
                if (n.ticks <= 0) it.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) return;
        if (activeNotifications.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        int yOffset = h / 4;

        for (TradeNotif n : activeNotifications) {
            int boxHeight = 32;
            int boxWidth = Math.max(180, mc.font.width(n.sub) + 40);

            float animProgress;
            if (n.ticks > n.maxTicks - 20) animProgress = (n.maxTicks - n.ticks) / 20f;
            else if (n.ticks < 20) animProgress = n.ticks / 20f;
            else animProgress = 1f;

            float easeProgress = 1f - (float) Math.pow(1f - animProgress, 3);
            float slideOffset = (1f - easeProgress) * boxWidth;
            int x = (int) (w - boxWidth + slideOffset);
            int y = yOffset;

            int alphaHex = (int)(easeProgress * 255) << 24;
            int bgColorStart = 0xAA000000;
            int bgColorEnd   = 0xEE000000;

            int themeColor = 0xFFFFFFFF;
            String icon = "";

            switch (n.type) {
                case "REQUEST" -> { themeColor = 0xFFFFAA00; icon = "§l?"; }
                case "SUCCESS" -> { themeColor = 0xFF00EE55; icon = "§l✔"; }
                case "CANCELLED" -> { themeColor = 0xFFFF3333; icon = "§l✖"; }
            }

            g.fillGradient(x, y, x + boxWidth, y + boxHeight, bgColorStart | (alphaHex & 0xFF000000), bgColorEnd | (alphaHex & 0xFF000000));
            g.fill(x, y, x + 3, y + boxHeight, themeColor | (alphaHex & 0xFF000000));
            g.fill(x + boxWidth - 3, y, x + boxWidth, y + boxHeight, themeColor | (alphaHex & 0xFF000000));
            g.fill(x + 3, y, x + boxWidth - 3, y + 1, themeColor | (alphaHex & 0xFF000000));
            g.fill(x + 3, y + boxHeight - 1, x + boxWidth - 3, y + boxHeight, themeColor | (alphaHex & 0xFF000000));

            int progressWidth = (int) ((boxWidth - 6) * (n.ticks / (float)n.maxTicks));
            g.fill(x + 3, y + boxHeight - 2, x + 3 + progressWidth, y + boxHeight - 1, themeColor | (alphaHex & 0xFF000000));

            RenderSystem.enableBlend();
            g.drawString(mc.font, icon, x + 12, y + 12, themeColor | alphaHex, true);
            g.drawString(mc.font, "§l" + n.title, x + 30, y + 6, themeColor | alphaHex, true);
            g.drawString(mc.font, n.sub, x + 30, y + 18, 0xDDDDDD | alphaHex, true);
            RenderSystem.disableBlend();

            yOffset += boxHeight + 5;
        }
    }
}