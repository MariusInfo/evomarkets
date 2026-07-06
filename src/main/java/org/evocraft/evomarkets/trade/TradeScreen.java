package org.evocraft.evomarkets.trade;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import org.evocraft.evocore.util.EvoCurrencyFormatter;
import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TradeScreen extends AbstractContainerScreen<TradeMenu> {

    // --- CULORILE EXACT CA IN POZELE TALE ---
    private static final int BG_COLOR = 0xEE0D140D;
    private static final int BORDER_COLOR = 0xFF83B755;
    private static final int CARD_BG = 0xAA141C14;
    private static final int CARD_BORDER = 0xFF3A592D;
    private static final int HOVER_COLOR = 0xFF6C9945;
    private static final int EDIT_COLOR = 0xFFFFAA00;
    private static final int TEXT_COLOR = 0xFFDDDDDD;

    public static double myMoney = 0;
    public static double theirMoney = 0;
    public static boolean myAccept = false;
    public static boolean theirAccept = false;
    public static int countdown = -1;
    public static List<String> chatLog = new ArrayList<>();
    public static String myName = "Eu";
    public static String theirName = "El";

    private EditBox chatBox;
    private EditBox moneyInput;

    private final List<CustomButton> buttons = new ArrayList<>();
    CustomButton btnAccept, btnCancel, btnAddMoney, btnResetMoney;

    public TradeScreen(TradeMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 340;
        this.imageHeight = 240;
        this.inventoryLabelY = 10000;
        this.titleLabelX = 10000;
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

        float scale = getScale();
        int sw = (int) (this.width / scale);
        int sh = (int) (this.height / scale);

        this.leftPos = (sw - this.imageWidth) / 2;
        this.topPos = (sh - this.imageHeight) / 2;

        int x = this.leftPos;
        int y = this.topPos;

        // Casute de Cautare Customizate (Fara Background Default)
        chatBox = new EditBox(this.font, 0, 0, 105, 16, Component.empty());
        chatBox.setMaxLength(250);
        chatBox.setBordered(false);
        chatBox.setTextColor(TEXT_COLOR);
        chatBox.setHint(Component.literal("Scrie în chat...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        moneyInput = new EditBox(this.font, 0, 0, 45, 16, Component.empty());
        moneyInput.setMaxLength(10);
        moneyInput.setBordered(false);
        moneyInput.setTextColor(TEXT_COLOR);
        moneyInput.setHint(Component.literal("Suma...").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));

        int controlsY = y + 105;

        btnAddMoney = new CustomButton("+", x + 72, controlsY - 3, 20, 20, this::processMoneyInput);
        btnResetMoney = new CustomButton("Reset Bani", x + 15, controlsY + 18, 77, 18, () -> sendAct("REMOVE_ALL_MONEY", 0, ""));

        btnAccept = new CustomButton("§aACCEPTĂ", x + 130, y + 35, 80, 25, () -> sendAct("TOGGLE_ACCEPT", 0, ""));
        btnCancel = new CustomButton("§cANULEAZĂ", x + 130, y + 65, 80, 25, () -> sendAct("CANCEL", 0, "Anulat manual."));

        buttons.addAll(List.of(btnAddMoney, btnResetMoney, btnAccept, btnCancel));
    }

    private void processMoneyInput() {
        try {
            double val = Double.parseDouble(moneyInput.getValue());
            if (val > 0) {
                sendAct("ADD_MONEY", val, "");
                moneyInput.setValue("");
            }
        } catch (NumberFormatException ignored) {
            moneyInput.setValue("");
        }
    }

    private void sendAct(String action, double amount, String text) {
        EvoMarketsPacketHandler.INSTANCE.sendToServer(new EvoMarketsPacketHandler.C2S_TradeAction(action, amount, text));
    }

    // --- FIX PENTRU SCRIS IN CASUTE ---
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (chatBox.isFocused()) {
            return chatBox.charTyped(codePoint, modifiers);
        }
        if (moneyInput.isFocused()) {
            return moneyInput.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    // --- FIX TASTA "E" SI ESCAPE ---
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESCAPE inchide mereu
        if (keyCode == 256) {
            this.onClose();
            return true;
        }

        if (chatBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                String text = chatBox.getValue();
                if (!text.isEmpty()) {
                    sendAct("CHAT", 0, text);
                    chatBox.setValue("");
                }
                return true;
            }
            chatBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        if (moneyInput.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                processMoneyInput();
                return true;
            }
            moneyInput.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }

        // BLOCAM TASTA INVENTARULUI (De obicei "E") CA SA NU MAI INCHIDA MENIUL!
        if (this.minecraft != null && this.minecraft.options.keyInventory.isActiveAndMatches(com.mojang.blaze3d.platform.InputConstants.getKey(keyCode, scanCode))) {
            return true; // Returnam true ca sa blocam actiunea (nu se va inchide)
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public static void syncData(double m1, double m2, boolean a1, boolean a2, int cd, String jsonChat, String n1, String n2) {
        myMoney = m1; theirMoney = m2; myAccept = a1; theirAccept = a2; countdown = cd; myName = n1; theirName = n2;
        if (jsonChat != null && !jsonChat.isEmpty() && !jsonChat.equals("null")) {
            chatLog = new Gson().fromJson(jsonChat, new TypeToken<List<String>>(){}.getType());
        } else { chatLog = new ArrayList<>(); }
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
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (countdown > 0) {
            float scale = getScale();
            int sw = (int) (this.width / scale);
            int sh = (int) (this.height / scale);

            g.pose().pushPose();
            g.pose().scale(scale, scale, 1.0f);

            g.pose().translate(0, 0, 1000);
            g.fill(0, 0, sw, sh, 0xEE000000);

            g.pose().pushPose();
            g.pose().translate(sw / 2.0f, sh / 2.0f - 20, 0);
            g.pose().scale(5.0f, 5.0f, 5.0f);
            int sec = (countdown / 20) + 1;
            g.drawCenteredString(this.font, "§a" + sec, 0, 0, 0xFFFFFF);
            g.pose().popPose();

            g.drawCenteredString(this.font, "§eTrade Acceptat! Se procesează...", sw / 2, sh / 2 + 40, 0xFFFFFF);
            g.drawCenteredString(this.font, "§cPentru a anula, apasă ESC!", sw / 2, sh / 2 + 60, 0xFFFFFF);

            g.pose().popPose();
            return;
        }

        this.renderBackground(g);

        float scale = getScale();
        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);

        int smx = (int) (mouseX / scale);
        int smy = (int) (mouseY / scale);

        if (btnAccept != null) {
            if (myAccept) { btnAccept.text = "§eȘTERGE ACCEPT"; btnAccept.hoverColor = 0xFF999900; }
            else { btnAccept.text = "§aACCEPTĂ"; btnAccept.hoverColor = HOVER_COLOR; }
        }

        renderCustomBg(g, smx, smy);

        for (CustomButton b : buttons) {
            if (b.visible) b.render(g, smx, smy, this.font);
        }

        int x = this.leftPos;
        int y = this.topPos;

        g.drawCenteredString(this.font, myName, x + 46, y + 15, BORDER_COLOR);
        g.drawCenteredString(this.font, theirName, x + 293, y + 15, 0xFFFF5555);

        g.drawCenteredString(this.font, "Offer: " + EvoCurrencyFormatter.formatWithCurrency(myMoney), x + 46, y + 90, 0xFFD700);
        g.drawCenteredString(this.font, "Offer: " + EvoCurrencyFormatter.formatWithCurrency(theirMoney), x + 293, y + 90, 0xFFD700);

        if (myAccept) g.drawCenteredString(this.font, "§aEu: ACCEPTAT", x + 170, y + 95, 0xFFFFFF);
        if (theirAccept) g.drawCenteredString(this.font, "§a" + theirName + ": ACCEPTAT", x + 170, y + 108, 0xFFFFFF);

        List<FormattedCharSequence> wrappedLines = new ArrayList<>();
        for (String msg : chatLog) {
            wrappedLines.addAll(this.font.split(Component.literal(msg), 115));
        }

        int maxLines = 16;
        int startIndex = Math.max(0, wrappedLines.size() - maxLines);
        int chatY = y + 30;

        for (int i = startIndex; i < wrappedLines.size(); i++) {
            g.drawString(this.font, wrappedLines.get(i), x - 125, chatY + ((i - startIndex) * 11), 0xFFFFFF, false);
        }

        g.pose().popPose();

        super.render(g, smx, smy, partialTick);

        g.pose().pushPose();
        g.pose().scale(scale, scale, 1.0f);
        this.renderTooltip(g, smx, smy);
        g.pose().popPose();
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float pTick, int mX, int mY) {}

    private void renderCustomBg(GuiGraphics g, int smx, int smy) {
        int x = this.leftPos;
        int y = this.topPos;

        // CHAT PANEL
        fillRounded(g, x - 135, y, 125, 240, BG_COLOR);
        outlineRounded(g, x - 135, y, 125, 240, BORDER_COLOR);
        g.drawCenteredString(this.font, "§lCHAT", x - 72, y + 10, BORDER_COLOR);

        // Căsuța pentru CHAT randată manual în scalare
        fillRounded(g, x - 130, y + 212, 115, 20, CARD_BG);
        outlineRounded(g, x - 130, y + 212, 115, 20, chatBox.isFocused() ? EDIT_COLOR : CARD_BORDER);
        chatBox.setX(x - 125); chatBox.setY(y + 218);
        chatBox.render(g, smx, smy, 0f);

        // MAIN PANEL
        fillRounded(g, x, y, imageWidth, imageHeight, BG_COLOR);
        outlineRounded(g, x, y, imageWidth, imageHeight, BORDER_COLOR);

        g.pose().pushPose();
        g.pose().translate(x + this.imageWidth / 2.0, y - 20, 0);
        g.pose().scale(1.4f, 1.4f, 1.4f);
        g.drawCenteredString(this.font, "TRADE", 0, 0, BORDER_COLOR);
        g.pose().popPose();

        // SLOTURI TRADE 3x3
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                fillRounded(g, x + 19 + c * 18, y + 29 + r * 18, 18, 18, CARD_BG);
                outlineRounded(g, x + 19 + c * 18, y + 29 + r * 18, 18, 18, CARD_BORDER);
            }
        }
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                fillRounded(g, x + 265 + c * 18, y + 29 + r * 18, 18, 18, CARD_BG);
                outlineRounded(g, x + 265 + c * 18, y + 29 + r * 18, 18, 18, CARD_BORDER);
            }
        }

        // MONEY INPUT BOX randat manual
        int controlsY = y + 105;
        fillRounded(g, x + 15, controlsY - 3, 55, 20, CARD_BG);
        outlineRounded(g, x + 15, controlsY - 3, 55, 20, moneyInput.isFocused() ? EDIT_COLOR : CARD_BORDER);
        moneyInput.setX(x + 20); moneyInput.setY(controlsY + 3);
        moneyInput.render(g, smx, smy, 0f);

        // INVENTAR JUCATOR
        fillRounded(g, x + 85, y + 144, 170, 84, CARD_BG);
        outlineRounded(g, x + 85, y + 144, 170, 84, CARD_BORDER);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float scale = getScale();
        int smx = (int) (mouseX / scale);
        int smy = (int) (mouseY / scale);

        if (countdown > 0) return true; // Blocăm click-ul în timpul timer-ului

        int x = this.leftPos;
        int y = this.topPos;
        int controlsY = y + 105;

        // Verificăm focusul la căsuțe și dăm "mouseClicked" pe ele ca să apară cursorul unde apasă
        if (smx >= x - 130 && smx <= x - 15 && smy >= y + 212 && smy <= y + 232) {
            chatBox.setFocused(true); moneyInput.setFocused(false);
            chatBox.mouseClicked(smx, smy, button);
            return true;
        } else if (smx >= x + 15 && smx <= x + 70 && smy >= controlsY - 3 && smy <= controlsY + 17) {
            moneyInput.setFocused(true); chatBox.setFocused(false);
            moneyInput.mouseClicked(smx, smy, button);
            return true;
        } else {
            chatBox.setFocused(false); moneyInput.setFocused(false);
        }

        for (CustomButton b : buttons) {
            if (b.checkClick(smx, smy)) return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    // --- CLASA PENTRU BUTOANE COMPLET CUSTOM ---
    class CustomButton {
        String text; int x, y, w, h; Runnable action; boolean visible = true; int hoverColor = HOVER_COLOR;
        public CustomButton(String text, int x, int y, int w, int h, Runnable action) {
            this.text = text; this.x = x; this.y = y; this.w = w; this.h = h; this.action = action;
        }
        public void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font) {
            boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
            fillRounded(g, x, y, w, h, CARD_BG);
            outlineRounded(g, x, y, w, h, hover ? hoverColor : CARD_BORDER);
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

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) { float s = getScale(); return super.mouseReleased(mouseX / s, mouseY / s, button); }
    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { float s = getScale(); return super.mouseDragged(mouseX / s, mouseY / s, button, dragX / s, dragY / s); }
    @Override public boolean mouseScrolled(double mouseX, double mouseY, double delta) { float s = getScale(); return super.mouseScrolled(mouseX / s, mouseY / s, delta); }
}
