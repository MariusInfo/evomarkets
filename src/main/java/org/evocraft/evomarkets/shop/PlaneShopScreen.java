package org.evocraft.evomarkets.shop;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.evocraft.evocore.client.ClientBalanceData;
import org.evocraft.evocore.util.EvoCurrencyFormatter;
import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.nbt.TagParser;

import java.util.*;
import java.util.Objects;

public class PlaneShopScreen extends Screen {

    private static final int BG_COLOR = 0xEE0D140D;
    private static final int BORDER_COLOR = 0xFF83B755;
    private static final int CARD_BG = 0xAA141C14;
    private static final int CARD_BORDER = 0xFF3A592D;
    private static final int HOVER_COLOR = 0xFF6C9945;
    private static final int TEXT_COLOR = 0xFFDDDDDD;

    private int leftPos, topPos, imageWidth, imageHeight;
    private final List<ItemButton> buttons = new ArrayList<>();
    private final List<CustomButton> navButtons = new ArrayList<>();

    // 3 Categorii
    public enum Tab { BODY, ENGINE, ATTACHMENTS, CHECKOUT }
    private Tab currentTab = Tab.BODY;
    private int currentPage = 0; // ADAUGAT: Variabila pentru a tine minte pagina curenta

    private PlaneShopManager.PlanePart selBody = null;
    private PlaneShopManager.PlanePart selEngine = null;
    private PlaneShopManager.PlanePart selAttach = null;

    private float currentSlide = 0f;
    private float targetSlide = 0f;
    private PlaneShopManager.PlanePart visualPart = null;

    private Map<String, List<PlaneShopManager.PlanePart>> shopData;

    public PlaneShopScreen(String jsonData) {
        super(Component.literal("Evo Plane Dealership"));
        this.imageWidth = 520;
        this.imageHeight = 330;
        try {
            shopData = new Gson().fromJson(jsonData, new TypeToken<Map<String, List<PlaneShopManager.PlanePart>>>(){}.getType());
        } catch (Exception e) { shopData = new HashMap<>(); }
    }

    private float getScale() {
        double scale = this.minecraft != null ? this.minecraft.getWindow().getGuiScale() : 1.0;
        if (scale >= 4) return 0.65f;
        if (scale >= 3) return 0.85f;
        return 1.0f;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createItemStack(PlaneShopManager.PlanePart part) {
        if (part == null || "none".equals(part.id)) return new ItemStack(Items.BARRIER);

        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(part.id));
        if (item == null || item == Items.AIR) return new ItemStack(Items.BARRIER);

        ItemStack stack = new ItemStack(item);
        if (part.nbt != null && !part.nbt.isEmpty()) {
            try { stack.setTag(TagParser.parseTag(part.nbt)); } catch (Exception ignored) {}
        }
        return stack;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear(); navButtons.clear();

        float scale = getScale();
        int sw = (int) (this.width / scale);
        int sh = (int) (this.height / scale);

        int shift = (int) (currentSlide * 80);
        this.leftPos = (sw - this.imageWidth) / 2 - shift;
        this.topPos = (sh - this.imageHeight) / 2;

        int mainX = this.leftPos + 165;
        int mainY = this.topPos + 35;

        navButtons.add(new CustomButton("§c✖ Închide", this.leftPos + this.imageWidth - 80, this.topPos + 10, 70, 20, this::onClose));

        if (currentTab != Tab.CHECKOUT) {
            List<PlaneShopManager.PlanePart> currentParts = shopData.getOrDefault(currentTab.name(), new ArrayList<>());

            // ==========================================
            // FIX PAGINARE: Afișează max 6 pe pagină
            // ==========================================
            int maxPages = (int) Math.ceil(currentParts.size() / 6.0);
            if (maxPages == 0) maxPages = 1;
            int startIndex = currentPage * 6;
            int endIndex = Math.min(startIndex + 6, currentParts.size());

            int col = 0, row = 0;
            for (int i = startIndex; i < endIndex; i++) {
                PlaneShopManager.PlanePart part = currentParts.get(i);
                buttons.add(new ItemButton(part, mainX + col * 115, mainY + row * 120, 105, 110, () -> {
                    if (visualPart != part) {
                        visualPart = part; targetSlide = 1.0f;
                    } else {
                        targetSlide = 0f; visualPart = null;
                    }
                }));
                col++; if (col >= 3) { col = 0; row++; }
            }

            // Butoanele pentru paginare
            if (maxPages > 1) {
                if (currentPage > 0) {
                    navButtons.add(new CustomButton("< Prev", mainX, mainY + 235, 80, 20, () -> {
                        currentPage--; this.init();
                    }));
                }
                if (currentPage < maxPages - 1) {
                    navButtons.add(new CustomButton("Next >", mainX + 255, mainY + 235, 80, 20, () -> {
                        currentPage++; this.init();
                    }));
                }
            }
            // ==========================================

            Tab nextTab = getNextTab(currentTab);
            navButtons.add(new CustomButton(currentTab == Tab.ATTACHMENTS ? "Checkout >" : "Skip >", mainX + 110, this.topPos + this.imageHeight - 40, 140, 25, () -> switchTab(nextTab)));
            if (currentTab != Tab.BODY) navButtons.add(new CustomButton("< Back", mainX, this.topPos + this.imageHeight - 40, 100, 25, () -> switchTab(getPrevTab(currentTab))));

        } else {
            navButtons.add(new CustomButton("< Edit", mainX, this.topPos + this.imageHeight - 40, 100, 25, () -> switchTab(Tab.ATTACHMENTS)));
            double total = (selBody != null ? selBody.price : 0) + (selEngine != null ? selEngine.price : 0) + (selAttach != null ? selAttach.price : 0);

            navButtons.add(new CustomButton("§aPAY " + formatPrice(total) + " Evo", mainX + 110, this.topPos + this.imageHeight - 40, 200, 25, () -> {
                if (total == 0) { this.minecraft.player.sendSystemMessage(Component.literal("§cYour cart is empty!")); return; }
                if (ClientBalanceData.getBalance() >= total) {
                    EvoMarketsPacketHandler.INSTANCE.sendToServer(new EvoMarketsPacketHandler.C2S_BuyPlanePacket(
                            (selBody != null && selBody.uuid != null) ? selBody.uuid : "none",
                            (selEngine != null && selEngine.uuid != null) ? selEngine.uuid : "none",
                            (selAttach != null && selAttach.uuid != null) ? selAttach.uuid : "none"
                    ));
                    this.onClose();
                } else {
                    this.minecraft.player.sendSystemMessage(Component.literal("§cNot enough Evo!"));
                }
            }));
        }

        if (visualPart != null && currentSlide > 0.1f) {
            navButtons.add(new CustomButton("§aADD TO CART", this.leftPos + this.imageWidth + 15, this.topPos + this.imageHeight - 60, 130, 25, () -> {
                if (currentTab == Tab.BODY) selBody = visualPart;
                if (currentTab == Tab.ENGINE) selEngine = visualPart;
                if (currentTab == Tab.ATTACHMENTS) selAttach = visualPart;
                switchTab(getNextTab(currentTab));
            }));
        }
    }

    private Tab getNextTab(Tab t) { return Tab.values()[Math.min(t.ordinal() + 1, Tab.CHECKOUT.ordinal())]; }
    private Tab getPrevTab(Tab t) { return Tab.values()[Math.max(t.ordinal() - 1, 0)]; }

    private void switchTab(Tab newTab) {
        this.currentTab = newTab; this.currentPage = 0; this.targetSlide = 0f; this.visualPart = null; this.init();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        currentSlide += (targetSlide - currentSlide) * 0.2f;
        if (Math.abs(currentSlide - targetSlide) < 0.005f) currentSlide = targetSlide;
        this.init();

        float scale = getScale();
        int smx = (int) (mouseX / scale); int smy = (int) (mouseY / scale);

        this.renderBackground(g);
        g.pose().pushPose(); g.pose().scale(scale, scale, 1.0f);

        fillRounded(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BG_COLOR);
        outlineRounded(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BORDER_COLOR);

        g.pose().pushPose();
        g.pose().translate(this.leftPos + this.imageWidth / 2.0, this.topPos - 25, 0);
        g.pose().scale(1.5f, 1.5f, 1.5f);
        g.drawCenteredString(this.font, "EVO AVIATION DEALERSHIP", 0, 0, BORDER_COLOR);
        g.pose().popPose();

        int sidebarW = 145;
        fillRounded(g, this.leftPos + 10, this.topPos + 10, sidebarW, this.imageHeight - 20, CARD_BG);
        outlineRounded(g, this.leftPos + 10, this.topPos + 10, sidebarW, this.imageHeight - 20, CARD_BORDER);

        g.drawString(this.font, "§eYour Build", this.leftPos + 20, this.topPos + 20, 0xFFFFFF);
        g.fill(this.leftPos + 15, this.topPos + 32, this.leftPos + sidebarW + 5, this.topPos + 33, CARD_BORDER);

        int textY = this.topPos + 40;
        g.drawString(this.font, "Body:", this.leftPos + 15, textY, TEXT_COLOR);
        if (selBody != null) g.drawString(this.font, "§a" + selBody.name, this.leftPos + 15, textY + 12, 0xFFFFFF);
        textY += 35;

        g.drawString(this.font, "Engine:", this.leftPos + 15, textY, TEXT_COLOR);
        if (selEngine != null) g.drawString(this.font, "§a" + selEngine.name, this.leftPos + 15, textY + 12, 0xFFFFFF);
        textY += 35;

        g.drawString(this.font, "Accessory:", this.leftPos + 15, textY, TEXT_COLOR);
        if (selAttach != null) g.drawString(this.font, "§a" + selAttach.name, this.leftPos + 15, textY + 12, 0xFFFFFF);

        double total = (selBody != null ? selBody.price : 0) + (selEngine != null ? selEngine.price : 0) + (selAttach != null ? selAttach.price : 0);
        g.fill(this.leftPos + 15, this.topPos + this.imageHeight - 65, this.leftPos + sidebarW + 5, this.topPos + this.imageHeight - 64, CARD_BORDER);
        g.drawString(this.font, "§fTotal:", this.leftPos + 15, this.topPos + this.imageHeight - 55, 0xFFFFFF);
        g.drawString(this.font, "§a" + formatPrice(total) + " Evo", this.leftPos + 15, this.topPos + this.imageHeight - 40, 0xFFFFFF);

        if (currentTab == Tab.CHECKOUT) {
            int mainX = this.leftPos + 175;
            int mainY = this.topPos + 35;

            PlaneShopManager.PlanePart[] selectedParts = {selBody, selEngine, selAttach};
            String[] labels = {"Body", "Engine", "Accessory"};

            // Centram cele 3 sloturi (2 pe un rand, 1 dedesubt pe centru)
            int[][] positions = {
                    {mainX, mainY},                   // Top Left
                    {mainX + 160, mainY},             // Top Right
                    {mainX + 80, mainY + 120}         // Bottom Center
            };

            for (int i = 0; i < 3; i++) {
                PlaneShopManager.PlanePart part = selectedParts[i];
                int px = positions[i][0];
                int py = positions[i][1];
                int boxW = 140;
                int boxH = 100;

                fillRounded(g, px, py, boxW, boxH, CARD_BG);
                outlineRounded(g, px, py, boxW, boxH, CARD_BORDER);

                g.drawCenteredString(this.font, "§e" + labels[i], px + boxW / 2, py + 8, 0xFFFFFF);

                if (part != null) {
                    g.pose().pushPose();
                    g.pose().translate(px + boxW / 2f, py + boxH / 2f - 5, 100);
                    g.pose().scale(3.0f, 3.0f, 1.0f);
                    g.renderItem(createItemStack(part), -8, -8);
                    g.pose().popPose();

                    g.pose().pushPose();
                    g.pose().translate(px + boxW / 2f, py + boxH - 25, 0);
                    g.pose().scale(0.85f, 0.85f, 1.0f);
                    g.drawCenteredString(this.font, part.name, 0, 0, TEXT_COLOR);
                    g.drawCenteredString(this.font, "§a" + formatPrice(part.price) + " Evo", 0, 12, 0xFFFFFF);
                    g.pose().popPose();
                } else {
                    g.drawCenteredString(this.font, "§7[ Not selected ]", px + boxW / 2, py + boxH / 2 - 5, 0xFFFFFF);
                }
            }
        } else {
            if (currentSlide > 0.05f && visualPart != null) {
                int infoX = this.leftPos + this.imageWidth + 5;
                fillRounded(g, infoX, this.topPos, 160, this.imageHeight, BG_COLOR);
                outlineRounded(g, infoX, this.topPos, 160, this.imageHeight, BORDER_COLOR);

                g.drawCenteredString(this.font, "PART INFO", infoX + 80, this.topPos + 15, BORDER_COLOR);

                g.pose().pushPose();
                g.pose().translate(infoX + 80, this.topPos + 90, 150);
                g.pose().scale(5.0f, 5.0f, 5.0f);
                g.renderItem(createItemStack(visualPart), -8, -8);
                g.pose().popPose();

                g.drawCenteredString(this.font, visualPart.name, infoX + 80, this.topPos + 160, 0xFFFFFF);
                g.drawCenteredString(this.font, "Price: §a" + formatPrice(visualPart.price) + " Evo", infoX + 80, this.topPos + 180, TEXT_COLOR);
            }

            for (ItemButton b : buttons) {
                boolean glow = (visualPart != null && b.part != null && Objects.equals(visualPart.uuid, b.part.uuid));
                b.render(g, smx, smy, this.font, glow);
            }
        }

        for (CustomButton b : navButtons) b.render(g, smx, smy, this.font, false);
        g.pose().popPose();
    }

    private void fillRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color); g.fill(x, y + 1, x + w, y + h - 1, color);
    }
    private void outlineRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + 1, color); g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color); g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private String formatPrice(double price) {
        return EvoCurrencyFormatter.format(price);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        float scale = getScale();
        int smx = (int) (mx / scale); int smy = (int) (my / scale);
        for (ItemButton b : buttons) if (b.checkClick(smx, smy)) return true;
        for (CustomButton b : navButtons) if (b.checkClick(smx, smy)) return true;
        return super.mouseClicked(mx, my, btn);
    }

    class ItemButton {
        PlaneShopManager.PlanePart part; int x, y, w, h; Runnable action;
        public ItemButton(PlaneShopManager.PlanePart part, int x, int y, int w, int h, Runnable action) {
            this.part = part; this.x = x; this.y = y; this.w = w; this.h = h; this.action = action;
        }
        public void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font, boolean isSelected) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            int borderColor = isSelected ? 0xFFFFFFFF : (hover ? HOVER_COLOR : CARD_BORDER);

            fillRounded(g, x, y, w, h, isSelected ? 0xAA224422 : CARD_BG);
            outlineRounded(g, x, y, w, h, borderColor);

            g.pose().pushPose(); g.pose().translate(x + w / 2f, y + 8, 0); g.pose().scale(0.85f, 0.85f, 1.0f);
            g.drawCenteredString(font, part.name, 0, 0, isSelected ? 0xFFFFFF : TEXT_COLOR);
            g.pose().popPose();

            g.pose().pushPose(); g.pose().translate(x + w / 2f - 24, y + h / 2f - 18, 100); g.pose().scale(3.0f, 3.0f, 1.0f);
            g.renderItem(createItemStack(part), 0, 0);
            g.pose().popPose();

            g.drawCenteredString(font, "§a" + formatPrice(part.price) + " Evo", x + w / 2, y + h - 15, 0xFFFFFF);
        }
        public boolean checkClick(int mx, int my) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + h) { action.run(); return true; } return false;
        }
    }

    class CustomButton {
        String text; int x, y, w, h; Runnable action;
        public CustomButton(String text, int x, int y, int w, int h, Runnable action) {
            this.text = text; this.x = x; this.y = y; this.w = w; this.h = h; this.action = action;
        }
        public void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font, boolean isSelected) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            fillRounded(g, x, y, w, h, hover ? HOVER_COLOR : CARD_BG);
            outlineRounded(g, x, y, w, h, hover ? HOVER_COLOR : CARD_BORDER);
            g.drawCenteredString(font, text, x + w / 2, y + (h - 8) / 2, hover ? 0xFFFFFF : TEXT_COLOR);
        }
        public boolean checkClick(int mx, int my) {
            if (mx >= x && mx <= x + w && my >= y && my <= y + h) { action.run(); return true; } return false;
        }
    }
}
