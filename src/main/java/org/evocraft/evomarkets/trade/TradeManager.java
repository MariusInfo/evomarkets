package org.evocraft.evomarkets.trade;

import com.google.gson.Gson;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import org.evocraft.evomarkets.EvoMarkets;
import org.evocraft.evocore.data.EconomyManager;
import org.evocraft.evocore.data.PlayerStatsManager;
import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EvoMarkets.MODID)
public class TradeManager {
    private static TradeManager INSTANCE;

    public static TradeManager get() {
        if (INSTANCE == null) INSTANCE = new TradeManager();
        return INSTANCE;
    }

    public static class PendingRequest {
        public ServerPlayer sender;
        public ServerPlayer target;
        public int ticksLeft = 1200;

        public PendingRequest(ServerPlayer sender, ServerPlayer target) {
            this.sender = sender;
            this.target = target;
        }
    }

    public final Map<UUID, PendingRequest> requests = new HashMap<>();
    public final List<TradeSession> activeTrades = new ArrayList<>();

    public void sendRequest(ServerPlayer sender, ServerPlayer target) {
        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(Component.literal("§cYou cannot trade with yourself!"));
            return;
        }
        if (isInTrade(sender) || isInTrade(target)) {
            sender.sendSystemMessage(Component.literal("§cThat player is already in a trade!"));
            return;
        }

        if (!PlayerStatsManager.get().canReceiveTrade(target.getUUID(), sender.getUUID())) {
            sender.sendSystemMessage(Component.literal("§c[!] You cannot send this player trade requests. Trade is disabled or they blocked you."));
            return;
        }

        if (requests.containsKey(target.getUUID())) {
            PendingRequest existingReq = requests.get(target.getUUID());
            if (existingReq.sender != null && existingReq.sender.getUUID().equals(sender.getUUID())) {
                sender.sendSystemMessage(Component.literal("§c[!] You already sent this player a trade request. Wait for a response!"));
            } else {
                sender.sendSystemMessage(Component.literal("§c[!] This player already has a pending trade request from someone else."));
            }
            return;
        }

        requests.put(target.getUUID(), new PendingRequest(sender, target));

        sender.sendSystemMessage(Component.literal("§aTrade request sent to " + target.getScoreboardName() + ". They have 1 minute to accept."));
        target.sendSystemMessage(Component.literal("§e[Trade] §fYou received a trade request from §b" + sender.getScoreboardName() + "§f!"));
        target.sendSystemMessage(Component.literal("§7You have 1 minute. Type §a/trade accept§7 to confirm."));

        EvoMarketsPacketHandler.sendToPlayer(new EvoMarketsPacketHandler.S2C_TradeNotification("REQUEST", sender.getScoreboardName()), target);
    }

    public void acceptRequest(ServerPlayer target) {
        PendingRequest req = requests.get(target.getUUID());
        if (req != null && req.sender != null && !req.sender.hasDisconnected() && !isInTrade(req.sender) && !isInTrade(target)) {
            requests.remove(target.getUUID());
            startTrade(req.sender, target);
        } else {
            target.sendSystemMessage(Component.literal("§cThe request expired or the player is no longer available."));
        }
    }

    private void startTrade(ServerPlayer p1, ServerPlayer p2) {
        TradeSession session = new TradeSession(p1, p2);
        activeTrades.add(session);

        // FIX: Acum trimitem rolul (isP1) clientului prin buffer!
        NetworkHooks.openScreen(p1, new MenuProvider() {
            @Override
            @NotNull
            public Component getDisplayName() { return Component.literal("Trade"); }
            @Override
            @NotNull
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
                return new TradeMenu(id, inv, session.items1, session.items2, true);
            }
        }, buf -> buf.writeBoolean(true));

        NetworkHooks.openScreen(p2, new MenuProvider() {
            @Override
            @NotNull
            public Component getDisplayName() { return Component.literal("Trade"); }
            @Override
            @NotNull
            public AbstractContainerMenu createMenu(int id, @NotNull Inventory inv, @NotNull Player player) {
                return new TradeMenu(id, inv, session.items2, session.items1, false);
            }
        }, buf -> buf.writeBoolean(false));

        session.sync();
    }

    public TradeSession getSession(ServerPlayer player) {
        for (TradeSession ts : activeTrades) {
            if (ts.p1.equals(player) || ts.p2.equals(player)) return ts;
        }
        return null;
    }

    public boolean isInTrade(ServerPlayer player) {
        return getSession(player) != null;
    }

    public void handleAction(ServerPlayer player, String action, double amount, String text) {
        TradeSession session = getSession(player);
        if (session == null) return;
        boolean isP1 = session.p1.equals(player);

        switch (action) {
            case "TOGGLE_ACCEPT" -> {
                if (isP1) session.accept1 = !session.accept1;
                else session.accept2 = !session.accept2;
                if (session.accept1 && session.accept2) session.countdown = 5 * 20;
                else session.countdown = -1;
            }
            case "CANCEL" -> session.cancelTrade("Trade cancelled by " + player.getScoreboardName());
            case "ADD_MONEY" -> {
                if (EconomyManager.get().getBalance(player.getUUID()) >= amount) {
                    session.resetAccept();
                    if (isP1) session.money1 += amount;
                    else session.money2 += amount;
                    EconomyManager.get().removeBalance(player.getUUID(), amount);
                } else {
                    player.sendSystemMessage(Component.literal("§c[!] You do not have enough Evo to add that amount!"));
                    player.displayClientMessage(Component.literal("§c[!] Insufficient funds!"), true);
                }
            }
            case "REMOVE_ALL_MONEY" -> {
                session.resetAccept();
                if (isP1 && session.money1 > 0) {
                    EconomyManager.get().addBalance(player.getUUID(), session.money1);
                    session.money1 = 0;
                } else if (!isP1 && session.money2 > 0) {
                    EconomyManager.get().addBalance(player.getUUID(), session.money2);
                    session.money2 = 0;
                }
            }
            case "CHAT" -> {
                session.chatLog.add("§e" + player.getScoreboardName() + "§f: " + text);
                if (session.chatLog.size() > 25) session.chatLog.remove(0);
            }
        }
        session.sync();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {

            Iterator<TradeSession> it = get().activeTrades.iterator();
            while (it.hasNext()) {
                TradeSession ts = it.next();
                if (ts.isFinished) { it.remove(); continue; }

                if (ts.countdown > 0) {
                    ts.countdown--;
                    if (ts.countdown % 20 == 0) ts.sync();
                    if (ts.countdown == 0) ts.completeTrade();
                }
            }

            Iterator<Map.Entry<UUID, PendingRequest>> reqIt = get().requests.entrySet().iterator();
            while (reqIt.hasNext()) {
                Map.Entry<UUID, PendingRequest> entry = reqIt.next();
                PendingRequest req = entry.getValue();
                req.ticksLeft--;
                if (req.ticksLeft <= 0) {
                    if (req.sender != null) req.sender.sendSystemMessage(Component.literal("§cThe trade request to " + req.target.getScoreboardName() + " expired."));
                    if (req.target != null) req.target.sendSystemMessage(Component.literal("§cThe trade request from " + req.sender.getScoreboardName() + " expired."));
                    reqIt.remove();
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLogOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TradeSession s = get().getSession(sp);
            if (s != null) s.cancelTrade("The player disconnected!");
        }
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event) {
        for (TradeSession s : get().activeTrades) s.cancelTrade("Server Oprit");
    }

    public static class TradeSession {
        public ServerPlayer p1, p2;
        // FIX: Modificat de la 12 la 9 ca sa bata cu grila clientului
        public SimpleContainer items1 = new SimpleContainer(9);
        public SimpleContainer items2 = new SimpleContainer(9);
        public double money1 = 0, money2 = 0;
        public boolean accept1 = false, accept2 = false;
        public int countdown = -1;
        public List<String> chatLog = new ArrayList<>();
        public boolean isFinished = false;

        public TradeSession(ServerPlayer p1, ServerPlayer p2) {
            this.p1 = p1;
            this.p2 = p2;
            items1.addListener(c -> resetAccept());
            items2.addListener(c -> resetAccept());
        }

        public void resetAccept() {
            if (countdown > 0 || accept1 || accept2) {
                accept1 = false;
                accept2 = false;
                countdown = -1;
                sync();
            }
        }

        public void sync() {
            if (isFinished) return;
            String chatJson = new Gson().toJson(chatLog);
            String n1 = p1.getScoreboardName();
            String n2 = p2.getScoreboardName();
            EvoMarketsPacketHandler.sendToPlayer(new EvoMarketsPacketHandler.S2C_TradeSync(money1, money2, accept1, accept2, countdown, chatJson, n1, n2), p1);
            EvoMarketsPacketHandler.sendToPlayer(new EvoMarketsPacketHandler.S2C_TradeSync(money2, money1, accept2, accept1, countdown, chatJson, n2, n1), p2);
        }

        public void completeTrade() {
            isFinished = true;
            EconomyManager.get().addBalance(p1.getUUID(), money2);
            EconomyManager.get().addBalance(p2.getUUID(), money1);
            giveItems(p1, items2);
            giveItems(p2, items1);
            p1.closeContainer();
            p2.closeContainer();
            EvoMarketsPacketHandler.sendToPlayer(new EvoMarketsPacketHandler.S2C_TradeNotification("SUCCESS", ""), p1);
            EvoMarketsPacketHandler.sendToPlayer(new EvoMarketsPacketHandler.S2C_TradeNotification("SUCCESS", ""), p2);
        }

        public void cancelTrade(String reason) {
            if (isFinished) return;
            isFinished = true;
            if (money1 > 0) EconomyManager.get().addBalance(p1.getUUID(), money1);
            if (money2 > 0) EconomyManager.get().addBalance(p2.getUUID(), money2);
            giveItems(p1, items1);
            giveItems(p2, items2);
            p1.closeContainer();
            p2.closeContainer();
            EvoMarketsPacketHandler.sendToPlayer(new EvoMarketsPacketHandler.S2C_TradeNotification("CANCELLED", reason), p1);
            EvoMarketsPacketHandler.sendToPlayer(new EvoMarketsPacketHandler.S2C_TradeNotification("CANCELLED", reason), p2);
        }

        private void giveItems(ServerPlayer player, SimpleContainer container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                    container.setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }
}
