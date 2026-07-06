package org.evocraft.evomarkets.ah;

import org.evocraft.evocore.client.ClientBalanceData;
import org.evocraft.evocore.util.EvoCurrencyFormatter;
import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AuctionScreen extends AbstractContainerScreen<AuctionMenu> {

    // --- CULORILE EXACT CA IN POZELE TALE ---
    private static final int BG_COLOR = 0xEE0D140D;
    private static final int BORDER_COLOR = 0xFF83B755;
    private static final int CARD_BG = 0xAA141C14;
    private static final int CARD_BORDER = 0xFF3A592D;
    private static final int HOVER_COLOR = 0xFF6C9945;
    private static final int EDIT_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFDDDDDD;

    private EditBox searchItemBox;
    private EditBox searchSellerBox;

    private int selectedVisualSlot = -1;
    private float currentSlide = 0f;
    private float targetSlide = 0f;
    private final int infoPanelWidth = 175;

    private final List<CustomButton> buttons = new ArrayList<>();
    CustomButton btnPrev, btnNext, btnClose, btnSort, btnMyListings, btnAction;

    public AuctionScreen(AuctionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.empty());
        this.imageWidth = 500;
        this.imageHeight = 345;
        this.titleLabelX = 10000;
        this.inventoryLabelX = 10000;
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
        this.clearWidgets();
        buttons.clear();

        searchItemBox = new EditBox(this.font, 0, 0, 90, 16, Component.empty());
        searchItemBox.setBordered(false);
        searchItemBox.setTextColor(TEXT_COLOR);
        searchItemBox.setHint(Component.literal("Caută item...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        searchSellerBox = new EditBox(this.font, 0, 0, 90, 16, Component.empty());
        searchSellerBox.setBordered(false);
        searchSellerBox.setTextColor(TEXT_COLOR);
        searchSellerBox.setHint(Component.literal("Seller...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        btnSort = new CustomButton("Sortare: Recent", 0, 0, 100, 20, () -> {
            menu.cycleSort(); updateSortText(); syncFilters();
        });

        btnMyListings = new CustomButton("Toate Itemele", 0, 0, 110, 20, () -> {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
            this.menu.showMyListings = !this.menu.showMyListings;
            updateMyListingsText();
        });

        btnPrev = new CustomButton("< Previous Page", 0, 0, 115, 20, () -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2));
        btnNext = new CustomButton("Next Page >", 0, 0, 115, 20, () -> this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1));
        btnClose = new CustomButton("Închide", 0, 0, 70, 20, this::onClose);

        btnAction = new CustomButton("§aCUMPĂRĂ", 0, 0, 140, 25, () -> {
            if (selectedVisualSlot != -1) {
                this.minecraft.gameMode.handleInventoryMouseClick(this.menu.containerId, selectedVisualSlot, 0, net.minecraft.world.inventory.ClickType.PICKUP, this.minecraft.player);
                targetSlide = 0f; selectedVisualSlot = -1;
            }
        });

        buttons.addAll(List.of(btnSort, btnMyListings, btnPrev, btnNext, btnClose, btnAction));
        updateSortText();
        updateMyListingsText();
    }

    private void updateSortText() {
        String txt = switch (menu.sortOrder) { case 1 -> "Sortare: Ieftin"; case 2 -> "Sortare: Scump"; default -> "Sortare: Recent"; };
        btnSort.text = txt;
    }

    private void updateMyListingsText() {
        btnMyListings.text = menu.showMyListings ? "§aDoar Itemele Mele" : "Toate Itemele";
    }

    private void syncFilters() {
        EvoMarketsPacketHandler.INSTANCE.sendToServer(new EvoMarketsPacketHandler.C2S_AuctionFilter(searchItemBox.getValue(), searchSellerBox.getValue(), menu.sortOrder));
    }

    public void refreshFromServer() {
        syncFilters();
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchItemBox.isFocused()) {
            searchItemBox.charTyped(codePoint, modifiers);
            syncFilters();
            return true;
        }
        if (searchSellerBox.isFocused()) {
            searchSellerBox.charTyped(codePoint, modifiers);
            syncFilters();
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            this.onClose();
            return true;
        }
        if (searchItemBox.isFocused()) {
            searchItemBox.keyPressed(keyCode, scanCode, modifiers);
            syncFilters();
            return true;
        }
        if (searchSellerBox.isFocused()) {
            searchSellerBox.keyPressed(keyCode, scanCode, modifiers);
            syncFilters();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // --- FUNCTIE SUPREMA PENTRU TAIEREA TEXTULUI (Calculeaza exact pixelii liberi) ---
    private String trimText(String text, int maxWidth) {
        if (this.font.width(text) <= maxWidth) return text;
        String result = text;
        while (result.length() > 0 && this.font.width(result + "...") > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
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

        renderCustomBg(g, smx, smy, delta);

        for (CustomButton b : buttons) {
            if (b.visible) b.render(g, smx, smy, this.font);
        }

        g.pose().popPose();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float pt, int mouseX, int mouseY) {}

    private void renderCustomBg(GuiGraphics g, int smx, int smy, float delta) {
        fillRounded(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BG_COLOR);
        outlineRounded(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, BORDER_COLOR);

        g.pose().pushPose();
        g.pose().translate(this.leftPos + this.imageWidth / 2.0, this.topPos - 25, 0);
        g.pose().scale(1.5f, 1.5f, 1.5f);
        g.drawCenteredString(this.font, "PIAȚA EVO CRAFT", 0, 0, BORDER_COLOR);
        g.pose().popPose();

        int topY = this.topPos + 15;

        fillRounded(g, this.leftPos + 20, topY, 100, 20, CARD_BG);
        outlineRounded(g, this.leftPos + 20, topY, 100, 20, searchItemBox.isFocused() ? EDIT_COLOR : CARD_BORDER);
        searchItemBox.setX(this.leftPos + 25); searchItemBox.setY(topY + 6);
        searchItemBox.render(g, smx, smy, delta);

        fillRounded(g, this.leftPos + 130, topY, 100, 20, CARD_BG);
        outlineRounded(g, this.leftPos + 130, topY, 100, 20, searchSellerBox.isFocused() ? EDIT_COLOR : CARD_BORDER);
        searchSellerBox.setX(this.leftPos + 135); searchSellerBox.setY(topY + 6);
        searchSellerBox.render(g, smx, smy, delta);

        btnSort.x = this.leftPos + 240; btnSort.y = topY;
        btnMyListings.x = this.leftPos + 360; btnMyListings.y = topY;

        int maxPages = this.menu.getMaxPages();
        int currentPage = this.menu.getPage();

        btnPrev.visible = maxPages > 0 && currentPage > 0;
        btnNext.visible = maxPages > 0 && currentPage < maxPages;

        if (maxPages > 0) {
            g.drawString(this.font, (currentPage + 1) + "/" + (maxPages + 1), this.leftPos + 35, this.topPos + this.imageHeight - 22, BORDER_COLOR, false);
        }

        btnPrev.x = this.leftPos + 85; btnPrev.y = this.topPos + this.imageHeight - 28;
        btnNext.x = this.leftPos + 210; btnNext.y = this.topPos + this.imageHeight - 28;
        btnClose.x = this.leftPos + 410; btnClose.y = this.topPos + this.imageHeight - 28;

        int cardW = 114, cardH = 48, gapX = 6, gapY = 6;
        int startX = 13, startY = 45;

        for (int i = 0; i < 20; i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.hasItem()) continue;

            int col = i % 4;
            int row = i / 4;
            int cx = this.leftPos + startX + col * (cardW + gapX);
            int cy = this.topPos + startY + row * (cardH + gapY);

            boolean isHovered = smx >= cx && smx <= cx + cardW && smy >= cy && smy <= cy + cardH;
            int outline = (i == selectedVisualSlot) ? EDIT_COLOR : (isHovered ? HOVER_COLOR : CARD_BORDER);

            fillRounded(g, cx, cy, cardW, cardH, CARD_BG);
            outlineRounded(g, cx, cy, cardW, cardH, outline);

            ItemStack stack = slot.getItem();

            g.pose().pushPose();
            g.pose().translate(cx + 6, cy + 12, 100);
            g.pose().scale(1.6f, 1.6f, 1.0f);
            g.renderItem(stack, 0, 0);
            g.renderItemDecorations(this.font, stack, 0, 0); // Adăugat: Afișează numărul de blocuri din pachet
            g.pose().popPose();

            CompoundTag tag = stack.getTag();
            if (tag != null) {
                double price = tag.getDouble("AH_Price");
                String seller = tag.getString("AH_Seller");

                // --- TAIEREA PERFECTA LA PIXEL PENTRU CHENARELE MICI ---
                // Pixel trim limits for compact item, seller and price rows.
                String displayItemName = trimText(stack.getHoverName().getString(), 78);
                String displaySeller = trimText(seller, 40);
                String displayPriceNum = trimText(formatPrice(price), 45);

                g.pose().pushPose();
                g.pose().translate(cx + 38, cy + 6, 100);
                g.pose().scale(0.9f, 0.9f, 1.0f);
                g.drawString(this.font, displayItemName, 0, 0, TEXT_COLOR, false);
                g.pose().popPose();

                g.pose().pushPose();
                g.pose().translate(cx + 38, cy + 20, 100);
                g.pose().scale(0.8f, 0.8f, 1.0f);
                g.drawString(this.font, "Seller: §b" + displaySeller, 0, 0, TEXT_COLOR, false);
                g.drawString(this.font, "Price: §a" + displayPriceNum + " Evo", 0, 12, TEXT_COLOR, false);
                g.pose().popPose();
            }
        }

        // ================= PANOU INFO DREAPTA =================
        boolean showInfo = currentSlide > 0.01f && selectedVisualSlot != -1;
        btnAction.visible = showInfo;

        if (showInfo) {
            int infoX = this.leftPos + this.imageWidth + 8;
            int infoY = this.topPos;

            fillRounded(g, infoX, infoY, infoPanelWidth, this.imageHeight, BG_COLOR);
            outlineRounded(g, infoX, infoY, infoPanelWidth, this.imageHeight, BORDER_COLOR);
            g.drawCenteredString(this.font, "INFO ITEM", infoX + infoPanelWidth / 2, infoY + 10, BORDER_COLOR);

            ItemStack stack = this.menu.getSlot(selectedVisualSlot).getItem();
            if (!stack.isEmpty() && stack.hasTag()) {

                String titleName = trimText(stack.getHoverName().getString(), 160);

                g.pose().pushPose();
                g.pose().translate(infoX + infoPanelWidth / 2, infoY + 28, 0);
                g.pose().scale(1.0f, 1.0f, 1.0f);
                g.drawCenteredString(this.font, titleName, 0, 0, 0xFF44AAFF);
                g.pose().popPose();

                g.pose().pushPose();
                g.pose().translate(infoX + infoPanelWidth / 2, infoY + 80, 150);
                g.pose().scale(3.5f, 3.5f, 3.5f);
                g.renderItem(stack, -8, -8);
                g.renderItemDecorations(this.font, stack, -8, -8); // Adăugat: Afișează numărul de blocuri și pe previzualizarea mare
                g.pose().popPose();

                CompoundTag tag = stack.getTag();
                double price = tag.getDouble("AH_Price");
                String seller = tag.getString("AH_Seller");
                boolean isMine = false;

                if (this.minecraft != null && this.minecraft.player != null) {
                    isMine = tag.getString("AH_SellerUUID").equals(this.minecraft.player.getUUID().toString());
                }

                // Tăiere text pt panoul din dreapta
                String displaySellerInfo = trimText(seller, 100);
                String displayPriceInfo = trimText(formatPrice(price), 100);

                g.pose().pushPose();
                g.pose().translate(infoX + 10, infoY + 115, 0);
                g.pose().scale(0.85f, 0.85f, 1.0f);
                g.drawString(this.font, "Seller: §b" + displaySellerInfo, 0, 0, TEXT_COLOR, false);
                g.drawString(this.font, "Price: §a" + displayPriceInfo + " Evo", 0, 13, TEXT_COLOR, false);
                g.drawString(this.font, "Quantity: §e" + stack.getCount() + "x", 0, 26, TEXT_COLOR, false);
                g.pose().popPose();

                int loreBoxY = infoY + 145;
                int loreBoxH = 105;

                fillRounded(g, infoX + 8, loreBoxY, infoPanelWidth - 16, loreBoxH, CARD_BG);
                outlineRounded(g, infoX + 8, loreBoxY, infoPanelWidth - 16, loreBoxH, BORDER_COLOR);

                int ty = loreBoxY + 5;
                if (this.minecraft != null && this.minecraft.player != null) {
                    List<Component> tooltip = stack.getTooltipLines(this.minecraft.player, net.minecraft.world.item.TooltipFlag.Default.NORMAL);
                    g.pose().pushPose();
                    g.pose().translate(infoX + 12, 0, 0);
                    g.pose().scale(0.75f, 0.75f, 1.0f);

                    for (int i = 1; i < tooltip.size(); i++) {
                        if (ty > loreBoxY + loreBoxH - 12) {
                            g.drawString(this.font, "...", 0, (int)(ty / 0.75f), 0xFFAAAAAA, false);
                            break;
                        }
                        g.drawString(this.font, tooltip.get(i), 0, (int)(ty / 0.75f), 0xFFFFFF, false);
                        ty += 9;
                    }
                    g.pose().popPose();
                }

                btnAction.text = isMine ? "§cCANCEL AUCTION" : "§aBUY ITEM";
                btnAction.x = infoX + 12;
                btnAction.y = infoY + 258;
                btnAction.w = infoPanelWidth - 24;

                int balY = infoY + 288;
                fillRounded(g, infoX + 12, balY, infoPanelWidth - 24, 36, CARD_BG);
                outlineRounded(g, infoX + 12, balY, infoPanelWidth - 24, 36, CARD_BORDER);

                g.pose().pushPose();
                g.pose().translate(infoX + infoPanelWidth / 2f, balY + 6, 0);
                g.pose().scale(0.9f, 0.9f, 1.0f);
                g.drawCenteredString(this.font, "Your Balance", 0, 0, TEXT_COLOR);
                g.drawCenteredString(this.font, formatPrice(ClientBalanceData.getBalance()) + " Evo", 0, 14, BORDER_COLOR);
                g.pose().popPose();
            }
        }
    }

    // --- FIX FORMATTER BANI ---
    private String formatPrice(double price) {
        return EvoCurrencyFormatter.format(price);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = getScale();
        int smx = (int) (mouseX / scale);
        int smy = (int) (mouseY / scale);

        int topY = this.topPos + 15;

        if (smx >= this.leftPos + 20 && smx <= this.leftPos + 120 && smy >= topY && smy <= topY + 20) {
            searchItemBox.setFocused(true); searchSellerBox.setFocused(false); return true;
        } else if (smx >= this.leftPos + 130 && smx <= this.leftPos + 230 && smy >= topY && smy <= topY + 20) {
            searchSellerBox.setFocused(true); searchItemBox.setFocused(false); return true;
        } else {
            searchItemBox.setFocused(false); searchSellerBox.setFocused(false);
        }

        for (CustomButton b : buttons) {
            if (b.checkClick(smx, smy)) return true;
        }

        int cardW = 114, cardH = 48, gapX = 6, gapY = 6;
        int startX = 13, startY = 45;

        for (int i = 0; i < 20; i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.hasItem()) continue;

            int col = i % 4;
            int row = i / 4;
            int cx = this.leftPos + startX + col * (cardW + gapX);
            int cy = this.topPos + startY + row * (cardH + gapY);

            if (smx >= cx && smx <= cx + cardW && smy >= cy && smy <= cy + cardH) {
                if (selectedVisualSlot == i) {
                    targetSlide = 0f;
                    selectedVisualSlot = -1;
                } else {
                    selectedVisualSlot = i;
                    targetSlide = 1.0f;
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
