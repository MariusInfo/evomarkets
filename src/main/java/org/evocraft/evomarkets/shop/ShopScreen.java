package org.evocraft.evomarkets.shop;

import org.evocraft.evocore.client.ClientBalanceData;
import org.evocraft.evocore.util.EvoCurrencyFormatter;
import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {

    private static final int BG_COLOR = 0xEE0D140D;
    private static final int BORDER_COLOR = 0xFF83B755;
    private static final int CARD_BG = 0xAA141C14;
    private static final int CARD_BORDER = 0xFF3A592D;
    private static final int HOVER_COLOR = 0xFF6C9945;
    private static final int EDIT_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFDDDDDD;

    private int selectedVisualSlot = -1;
    private int currentQuantity = 1;

    private float currentSlide = 0f;
    private float targetSlide = 0f;
    private final int infoPanelWidth = 160;
    private boolean backpackPromptVisible = false;
    private String backpackPromptCategoryId = "";
    private int backpackPromptItemIndex = -1;
    private int backpackPromptCount = 0;

    private final List<CustomButton> buttons = new ArrayList<>();
    CustomButton btnX1, btnX32, btnX64, btnBuy, btnSell, btnSellAll, btnPrev, btnNext, btnBack, btnClose;

    public ShopScreen(ShopMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 500;
        this.imageHeight = 315;
        this.titleLabelX = 10000;
        this.inventoryLabelX = 10000;
        this.titleLabelY = 10000;
        this.inventoryLabelY = 10000;
    }

    private float getScale() {
        double guiScale = this.minecraft != null ? this.minecraft.getWindow().getGuiScale() : 1.0;
        if (guiScale >= 4) return 0.65f;
        if (guiScale >= 3) return 0.85f;
        return 1.0f;
    }

    @Override
    protected void init() {
        super.init();
        buttons.clear();

        btnPrev = new CustomButton("< Previous Page", 0, 0, 110, 20, () -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 101));
        btnNext = new CustomButton("Next Page >", 0, 0, 110, 20, () -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 102));
        btnClose = new CustomButton("Close", 0, 0, 60, 20, this::onClose);

        btnBack = new CustomButton("< Back to Categories", 0, 0, 130, 20, () -> {
            selectedVisualSlot = -1; targetSlide = 0f;
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 99);
        });

        btnX1 = new CustomButton("x1", 0, 0, 40, 20, () -> setQty(1));
        btnX32 = new CustomButton("x32", 0, 0, 40, 20, () -> setQty(32));
        btnX64 = new CustomButton("x64", 0, 0, 40, 20, () -> setQty(64));

        btnBuy = new CustomButton("§aBUY", 0, 0, 130, 25, () -> executeTrade("BUY", currentQuantity));
        btnSell = new CustomButton("§cSELL", 0, 0, 62, 25, () -> executeTrade("SELL", currentQuantity));
        btnSellAll = new CustomButton("§cSELL ALL", 0, 0, 63, 25, () -> executeTrade("SELL", -1));

        buttons.addAll(List.of(btnPrev, btnNext, btnClose, btnBack, btnX1, btnX32, btnX64, btnBuy, btnSell, btnSellAll));
        setQty(1);
    }

    private void setQty(int q) {
        this.currentQuantity = q;
        btnX1.text = (q == 1 ? "§a[x1]" : "x1");
        btnX32.text = (q == 32 ? "§a[x32]" : "x32");
        btnX64.text = (q == 64 ? "§a[x64]" : "x64");
    }

    private void executeTrade(String type, int amount) {
        executeTrade(type, amount, false, false);
    }

    private void executeTrade(String type, int amount, boolean includeBackpackItems, boolean skipBackpackPrompt) {
        if (selectedVisualSlot == -1 || this.menu.isMainMenu()) return;
        String catId = this.menu.getCurrentCategoryId();
        int realIndex = (this.menu.getPage() * 20) + selectedVisualSlot;

        Slot s = this.menu.getSlot(selectedVisualSlot);
        if (s.hasItem() && s.getItem().hasTag() && s.getItem().getTag().contains("Real_List_Index")) {
            realIndex = s.getItem().getTag().getInt("Real_List_Index");
        }
        EvoMarketsPacketHandler.INSTANCE.sendToServer(new EvoMarketsPacketHandler.C2S_ShopTrade(catId, realIndex, type, amount, includeBackpackItems, skipBackpackPrompt));
    }

    public void showBackpackSellPrompt(String categoryId, int itemIndex, int backpackCount) {
        this.backpackPromptVisible = true;
        this.backpackPromptCategoryId = categoryId;
        this.backpackPromptItemIndex = itemIndex;
        this.backpackPromptCount = backpackCount;
    }

    private void fillRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + w, y + h - 1, color);
    }

    private void outlineRounded(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 1, y, x + w - 1, y + 1, color);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h, color);
        g.fill(x, y + 1, x + 1, y + h - 1, color);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float delta) {
        if (this.menu.isMainMenu()) { targetSlide = 0f; selectedVisualSlot = -1; }

        currentSlide += (targetSlide - currentSlide) * 0.2f;
        if (Math.abs(currentSlide - targetSlide) < 0.005f) currentSlide = targetSlide;

        float scale = getScale();
        int sw = (int) (this.width / scale);
        int sh = (int) (this.height / scale);

        int shift = (int) (currentSlide * (infoPanelWidth / 2f + 5));
        this.leftPos = (sw - this.imageWidth) / 2 - shift;
        this.topPos = (sh - this.imageHeight) / 2;

        int smx = (int) (mouseX / scale);
        int smy = (int) (mouseY / scale);

        this.renderBackground(g);

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);

        renderCustomBg(g, smx, smy);

        for (CustomButton b : buttons) {
            if (b.visible) b.render(g, smx, smy, this.font);
        }

        if (backpackPromptVisible) {
            renderBackpackPrompt(g);
        }

        g.pose().popPose();

        // Nu mai randam tooltip-ul deloc (Vanilla Hover OFF)
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float pt, int mouseX, int mouseY) {}

    private void renderCustomBg(GuiGraphics g, int mouseX, int mouseY) {
        fillRounded(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BG_COLOR);
        outlineRounded(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BORDER_COLOR);

        // TITLUL DEASUPRA, AFARA DIN CHENAR
        g.pose().pushPose();
        g.pose().translate(this.leftPos + this.imageWidth / 2.0, this.topPos - 25, 0);
        g.pose().scale(1.5f, 1.5f, 1.5f);
        g.drawCenteredString(this.font, this.menu.getCategoryName().toUpperCase(), 0, 0, BORDER_COLOR);
        g.pose().popPose();

        boolean isMain = this.menu.isMainMenu();
        int maxPages = this.menu.getMaxPages();
        int currentPage = this.menu.getPage();

        btnBack.visible = !isMain;
        btnPrev.visible = maxPages > 0 && currentPage > 0;
        btnNext.visible = maxPages > 0 && currentPage < maxPages;

        if (maxPages > 0) {
            g.drawString(this.font, (currentPage + 1) + "/" + (maxPages + 1), this.leftPos + 35, this.topPos + this.imageHeight - 20, BORDER_COLOR, false);
        }

        btnPrev.x = this.leftPos + 75; btnPrev.y = this.topPos + this.imageHeight - 26;
        btnNext.x = this.leftPos + 190; btnNext.y = this.topPos + this.imageHeight - 26;

        if (isMain) {
            btnClose.x = this.leftPos + 420; btnClose.y = this.topPos + this.imageHeight - 26;
            btnClose.visible = true;
        } else {
            btnClose.visible = false;
            btnBack.x = this.leftPos + 350; btnBack.y = this.topPos + this.imageHeight - 26;
        }

        int cardW = 114, cardH = 48, gapX = 6, gapY = 6;
        int startX = 12, startY = 15;

        for (int i = 0; i < 20; i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.hasItem()) continue;

            int col = i % 4;
            int row = i / 4;
            int cx = this.leftPos + startX + col * (cardW + gapX);
            int cy = this.topPos + startY + row * (cardH + gapY);

            boolean isHovered = mouseX >= cx && mouseX <= cx + cardW && mouseY >= cy && mouseY <= cy + cardH;
            int outline = (i == selectedVisualSlot && !isMain) ? EDIT_COLOR : (isHovered ? HOVER_COLOR : CARD_BORDER);

            fillRounded(g, cx, cy, cardW, cardH, CARD_BG);
            outlineRounded(g, cx, cy, cardW, cardH, outline);

            ItemStack stack = slot.getItem();

            g.pose().pushPose();
            g.pose().translate(cx + 6, cy + 12, 100);
            g.pose().scale(1.6f, 1.6f, 1.0f);
            g.renderItem(stack, 0, 0);
            g.pose().popPose();

            if (!isMain) {
                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    double buy = tag.getDouble("Buy_Price");
                    double sell = tag.getDouble("Sell_Price");

                    g.pose().pushPose();
                    g.pose().translate(cx + 38, cy + 6, 100);
                    g.pose().scale(0.9f, 0.9f, 1.0f);
                    g.drawString(this.font, stack.getHoverName().getString(), 0, 0, TEXT_COLOR, false);
                    g.pose().popPose();

                    g.pose().pushPose();
                    g.pose().translate(cx + 38, cy + 20, 100);
                    g.pose().scale(0.8f, 0.8f, 1.0f);
                    g.drawString(this.font, "Buy: §a" + formatPrice(buy) + " Evo", 0, 0, TEXT_COLOR, false);
                    g.drawString(this.font, "Sell: §c" + formatPrice(sell) + " Evo", 0, 12, TEXT_COLOR, false);
                    g.pose().popPose();
                }
            } else {
                g.pose().pushPose();
                g.pose().translate(cx + 38, cy + 18, 100);
                g.pose().scale(1.1f, 1.1f, 1.0f);
                g.drawString(this.font, stack.getHoverName().getString(), 0, 0, TEXT_COLOR, false);
                g.pose().popPose();
            }
        }

        boolean showInfo = currentSlide > 0.01f && selectedVisualSlot != -1 && !isMain;

        btnX1.visible = showInfo; btnX32.visible = showInfo; btnX64.visible = showInfo;
        btnBuy.visible = showInfo; btnSell.visible = showInfo; btnSellAll.visible = showInfo;

        if (showInfo) {
            int infoX = this.leftPos + this.imageWidth + 8;
            int infoY = this.topPos;

            fillRounded(g, infoX, infoY, infoPanelWidth, this.imageHeight, BG_COLOR);
            outlineRounded(g, infoX, infoY, infoPanelWidth, this.imageHeight, BORDER_COLOR);
            g.drawCenteredString(this.font, "INFO ITEM", infoX + 80, infoY + 10, BORDER_COLOR);

            ItemStack stack = this.menu.getSlot(selectedVisualSlot).getItem();
            if (!stack.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(infoX + 80, infoY + 28, 0);
                g.pose().scale(1.0f, 1.0f, 1.0f);
                g.drawCenteredString(this.font, stack.getHoverName().getString(), 0, 0, 0xFF44AAFF);
                g.pose().popPose();

                g.pose().pushPose();
                g.pose().translate(infoX + 80, infoY + 85, 150);
                g.pose().scale(4.0f, 4.0f, 4.0f);
                g.renderItem(stack, -8, -8);
                g.pose().popPose();

                int invCount = getInventoryCount(stack);
                g.pose().pushPose();
                g.pose().translate(infoX + 80, infoY + 135, 0);
                g.pose().scale(0.85f, 0.85f, 1.0f);
                g.drawCenteredString(this.font, "In inventory: " + invCount + " units", 0, 0, 0xFFAAAAAA);
                g.pose().popPose();

                CompoundTag tag = stack.getTag();
                if (tag != null) {
                    double buy = tag.getDouble("Buy_Price");
                    double sell = tag.getDouble("Sell_Price");
                    g.pose().pushPose();
                    g.pose().translate(infoX + 15, infoY + 160, 0);
                    g.pose().scale(0.95f, 0.95f, 1.0f);
                    g.drawString(this.font, "Buy Price: §a" + formatPrice(buy) + " Evo", 0, 0, TEXT_COLOR, false);
                    g.drawString(this.font, "Sell Price: §c" + formatPrice(sell) + " Evo", 0, 15, TEXT_COLOR, false);
                    g.pose().popPose();
                }

                btnX1.x = infoX + 15; btnX1.y = infoY + 190;
                btnX32.x = infoX + 60; btnX32.y = infoY + 190;
                btnX64.x = infoX + 105; btnX64.y = infoY + 190;

                btnBuy.x = infoX + 15; btnBuy.y = infoY + 220;
                btnSell.x = infoX + 15; btnSell.y = infoY + 250;
                btnSellAll.x = infoX + 83; btnSellAll.y = infoY + 250;

                fillRounded(g, infoX + 10, infoY + 280, 140, 30, CARD_BG);
                outlineRounded(g, infoX + 10, infoY + 280, 140, 30, CARD_BORDER);

                g.pose().pushPose();
                g.pose().translate(infoX + 80, infoY + 285, 0);
                g.pose().scale(0.8f, 0.8f, 1.0f);
                g.drawCenteredString(this.font, "Your Balance", 0, 0, TEXT_COLOR);
                g.drawCenteredString(this.font, formatPrice(ClientBalanceData.getBalance()) + " Evo", 0, 12, BORDER_COLOR);
                g.pose().popPose();
            }
        }
    }

    private void renderBackpackPrompt(GuiGraphics g) {
        int boxW = 330;
        int boxH = 122;
        int x = this.leftPos + (this.imageWidth - boxW) / 2;
        int y = this.topPos + (this.imageHeight - boxH) / 2;

        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x99000000);
        fillRounded(g, x, y, boxW, boxH, BG_COLOR);
        outlineRounded(g, x, y, boxW, boxH, BORDER_COLOR);

        g.drawCenteredString(this.font, "Backpack Sell All", x + boxW / 2, y + 12, BORDER_COLOR);
        g.drawCenteredString(this.font, "Do you want to sell items", x + boxW / 2, y + 34, TEXT_COLOR);
        g.drawCenteredString(this.font, "from your backpack as well?", x + boxW / 2, y + 47, TEXT_COLOR);
        g.drawCenteredString(this.font, backpackPromptCount + " matching items found", x + boxW / 2, y + 65, 0xFFAAAAAA);

        renderPromptButton(g, x + 56, y + 88, 92, 22, "Yes", 0xFF58C76B);
        renderPromptButton(g, x + boxW - 148, y + 88, 92, 22, "No", 0xFFE25F5F);
    }

    private void renderPromptButton(GuiGraphics g, int x, int y, int w, int h, String label, int color) {
        fillRounded(g, x, y, w, h, CARD_BG);
        outlineRounded(g, x, y, w, h, color);
        g.drawCenteredString(this.font, label, x + w / 2, y + 7, color);
    }

    private boolean handleBackpackPromptClick(int mouseX, int mouseY) {
        if (!backpackPromptVisible) return false;

        int boxW = 330;
        int boxH = 122;
        int x = this.leftPos + (this.imageWidth - boxW) / 2;
        int y = this.topPos + (this.imageHeight - boxH) / 2;

        int yesX = x + 56;
        int yesY = y + 88;
        int noX = x + boxW - 148;
        int noY = y + 88;

        if (mouseX >= yesX && mouseX <= yesX + 92 && mouseY >= yesY && mouseY <= yesY + 22) {
            sendBackpackPromptChoice(true);
            return true;
        }

        if (mouseX >= noX && mouseX <= noX + 92 && mouseY >= noY && mouseY <= noY + 22) {
            sendBackpackPromptChoice(false);
            return true;
        }

        return mouseX >= x && mouseX <= x + boxW && mouseY >= y && mouseY <= y + boxH;
    }

    private void sendBackpackPromptChoice(boolean includeBackpackItems) {
        backpackPromptVisible = false;
        EvoMarketsPacketHandler.INSTANCE.sendToServer(new EvoMarketsPacketHandler.C2S_ShopTrade(
                backpackPromptCategoryId,
                backpackPromptItemIndex,
                "SELL",
                -1,
                includeBackpackItems,
                true
        ));
    }

    private int getInventoryCount(ItemStack target) {
        if (this.minecraft == null || this.minecraft.player == null) return 0;
        int count = 0;
        for (ItemStack stack : this.minecraft.player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) count += stack.getCount();
        }
        return count;
    }

    private String formatPrice(double price) {
        return EvoCurrencyFormatter.format(price);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = getScale();
        int smx = (int) (mouseX / scale);
        int smy = (int) (mouseY / scale);

        if (handleBackpackPromptClick(smx, smy)) {
            return true;
        }

        for (CustomButton b : buttons) {
            if (b.checkClick(smx, smy)) return true;
        }

        int cardW = 114, cardH = 48, gapX = 6, gapY = 6;
        int startX = 12, startY = 15;

        for (int i = 0; i < 20; i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.hasItem()) continue;

            int col = i % 4;
            int row = i / 4;
            int cx = this.leftPos + startX + col * (cardW + gapX);
            int cy = this.topPos + startY + row * (cardH + gapY);

            if (smx >= cx && smx <= cx + cardW && smy >= cy && smy <= cy + cardH) {
                if (this.menu.isMainMenu()) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, i);
                } else {
                    if (selectedVisualSlot == i) {
                        targetSlide = 0f;
                        selectedVisualSlot = -1;
                    } else {
                        selectedVisualSlot = i;
                        targetSlide = 1.0f;
                        setQty(1);
                    }
                }
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    class CustomButton {
        String text; int x, y, w, h; Runnable action; boolean visible = true;
        public CustomButton(String text, int x, int y, int w, int h, Runnable action) {
            this.text = text; this.x = x; this.y = y; this.w = w; this.h = h; this.action = action;
        }
        public void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            fillRounded(g, x, y, w, h, CARD_BG);
            outlineRounded(g, x, y, w, h, hover ? HOVER_COLOR : CARD_BORDER);
            g.pose().pushPose();
            g.pose().translate(x + w / 2f, y + (h - 8) / 2f, 0);
            g.drawCenteredString(font, text, 0, 0, hover ? 0xFFFFFF : TEXT_COLOR);
            g.pose().popPose();
        }
        public boolean checkClick(int mx, int my) {
            if (visible && mx >= x && mx <= x + w && my >= y && my <= y + h) {
                action.run(); return true;
            }
            return false;
        }
    }
}
