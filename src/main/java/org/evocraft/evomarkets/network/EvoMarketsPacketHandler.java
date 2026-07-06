package org.evocraft.evomarkets.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.evocraft.evocore.util.EvoCurrencyFormatter;
import org.evocraft.evomarkets.EvoMarkets;
import org.evocraft.evomarkets.shop.ShopMenu;
import org.evocraft.evomarkets.shop.ShopConfigManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class EvoMarketsPacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EvoMarkets.MODID, "market_channel"),
            () -> PROTOCOL_VERSION,
            s -> true,
            s -> true
    );

    private static int packetId = 0;
    private static int nextId() { return packetId++; }

    public static void register() {
        // Pachete Shop Normal
        INSTANCE.registerMessage(nextId(), S2C_SyncShop.class, S2C_SyncShop::toBytes, S2C_SyncShop::new, S2C_SyncShop::handle);
        INSTANCE.registerMessage(nextId(), C2S_ShopTrade.class, C2S_ShopTrade::toBytes, C2S_ShopTrade::new, C2S_ShopTrade::handle);
        INSTANCE.registerMessage(nextId(), S2C_BackpackSellPrompt.class, S2C_BackpackSellPrompt::toBytes, S2C_BackpackSellPrompt::new, S2C_BackpackSellPrompt::handle);
        INSTANCE.registerMessage(nextId(), C2S_EditShop.class, C2S_EditShop::toBytes, C2S_EditShop::new, C2S_EditShop::handle);

        // Pachete Auction House
        INSTANCE.registerMessage(nextId(), S2C_AuctionGlobalUpdate.class, S2C_AuctionGlobalUpdate::toBytes, S2C_AuctionGlobalUpdate::new, S2C_AuctionGlobalUpdate::handle);
        INSTANCE.registerMessage(nextId(), C2S_AuctionFilter.class, C2S_AuctionFilter::toBytes, C2S_AuctionFilter::new, C2S_AuctionFilter::handle);

        // Pachete Trade
        INSTANCE.registerMessage(nextId(), S2C_TradeNotification.class, S2C_TradeNotification::toBytes, S2C_TradeNotification::new, S2C_TradeNotification::handle);
        INSTANCE.registerMessage(nextId(), S2C_TradeSync.class, S2C_TradeSync::toBytes, S2C_TradeSync::new, S2C_TradeSync::handle);
        INSTANCE.registerMessage(nextId(), C2S_TradeAction.class, C2S_TradeAction::toBytes, C2S_TradeAction::new, C2S_TradeAction::handle);

        // Pachete Car Shop (Dealership)
        INSTANCE.registerMessage(nextId(), S2C_OpenCarShop.class, S2C_OpenCarShop::toBytes, S2C_OpenCarShop::new, S2C_OpenCarShop::handle);
        INSTANCE.registerMessage(nextId(), C2S_BuyCarPacket.class, C2S_BuyCarPacket::toBytes, C2S_BuyCarPacket::new, C2S_BuyCarPacket::handle);

        // Pachete Plane Shop (Aviation)
        INSTANCE.registerMessage(nextId(), S2C_OpenPlaneShop.class, S2C_OpenPlaneShop::toBytes, S2C_OpenPlaneShop::new, S2C_OpenPlaneShop::handle);
        INSTANCE.registerMessage(nextId(), C2S_BuyPlanePacket.class, C2S_BuyPlanePacket::toBytes, C2S_BuyPlanePacket::new, C2S_BuyPlanePacket::handle);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        if(player != null) INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static <MSG> void sendToAll(MSG message) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), message);
    }

    private static byte[] compress(String str) { try(ByteArrayOutputStream o=new ByteArrayOutputStream();GZIPOutputStream g=new GZIPOutputStream(o)){g.write(str.getBytes(StandardCharsets.UTF_8));g.close();return o.toByteArray();}catch(Exception e){return new byte[0];}}
    private static String decompress(byte[] c) { try(ByteArrayInputStream i=new ByteArrayInputStream(c);GZIPInputStream g=new GZIPInputStream(i);ByteArrayOutputStream o=new ByteArrayOutputStream()){byte[] b=new byte[1024];int l;while((l=g.read(b))!=-1)o.write(b,0,l);return o.toString(StandardCharsets.UTF_8);}catch(Exception e){return "";}}

    // ========================================================
    // PACHETE PLANE SHOP (AVIATION)
    // ========================================================
    public static class S2C_OpenPlaneShop {
        public final String jsonData;

        public S2C_OpenPlaneShop(String jsonData) { this.jsonData = jsonData; }

        public S2C_OpenPlaneShop(FriendlyByteBuf buf) {
            this.jsonData = buf.readUtf(262144);
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(jsonData, 262144);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientEvoMarketsHandler.openPlaneShop(jsonData);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2S_BuyPlanePacket {
        public final String bUuid, eUuid, aUuid;

        public C2S_BuyPlanePacket(String b, String e, String a) {
            this.bUuid = b; this.eUuid = e; this.aUuid = a;
        }

        public C2S_BuyPlanePacket(FriendlyByteBuf buf) {
            this.bUuid = buf.readUtf(256);
            this.eUuid = buf.readUtf(256);
            this.aUuid = buf.readUtf(256);
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(bUuid, 256);
            buf.writeUtf(eUuid, 256);
            buf.writeUtf(aUuid, 256);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                double total = 0;
                org.evocraft.evomarkets.shop.PlaneShopManager.PlanePart body = getPart("BODY", bUuid);
                org.evocraft.evomarkets.shop.PlaneShopManager.PlanePart engine = getPart("ENGINE", eUuid);
                org.evocraft.evomarkets.shop.PlaneShopManager.PlanePart attach = getPart("ATTACHMENTS", aUuid);

                if (body != null) total += body.price;
                if (engine != null) total += engine.price;
                if (attach != null) total += attach.price;

                if (total == 0) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cYour cart is empty!")); return;
                }

                org.evocraft.evocore.data.PlayerStatsManager.PlayerStats stats = org.evocraft.evocore.data.PlayerStatsManager.get().getStats(player.getUUID());
                if (stats.balance < total) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNot enough Evo!")); return;
                }

                stats.balance -= total;
                org.evocraft.evocore.data.PlayerStatsManager.get().saveToDatabase(player.getUUID());
                org.evocraft.evocore.data.PlayerStatsManager.get().syncClient(player.getUUID());

                if (body != null) giveItem(player, body, 1);
                if (engine != null) giveItem(player, engine, 1);
                if (attach != null) giveItem(player, attach, 1);

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aPurchased aviation parts for " + EvoCurrencyFormatter.formatWithCurrency(total) + "!"));
            });
            ctx.get().setPacketHandled(true);
        }

        private org.evocraft.evomarkets.shop.PlaneShopManager.PlanePart getPart(String cat, String uuid) {
            if (uuid.equals("none")) return null;
            java.util.List<org.evocraft.evomarkets.shop.PlaneShopManager.PlanePart> parts = org.evocraft.evomarkets.shop.PlaneShopManager.shopData.get(cat);
            if (parts != null) for (org.evocraft.evomarkets.shop.PlaneShopManager.PlanePart p : parts) if (p.uuid.equals(uuid)) return p;
            return null;
        }

        private void giveItem(ServerPlayer player, org.evocraft.evomarkets.shop.PlaneShopManager.PlanePart part, int amount) {
            @SuppressWarnings("deprecation")
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(part.id));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, amount);

                if (part.nbt != null && !part.nbt.isEmpty()) {
                    try { stack.setTag(net.minecraft.nbt.TagParser.parseTag(part.nbt)); } catch (Exception ignored) {}
                }

                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
        }
    }

    // ========================================================
    // PACHETE CAR SHOP (DEALERSHIP)
    // ========================================================
    public static class S2C_OpenCarShop {
        public final String jsonData;

        public S2C_OpenCarShop(String jsonData) { this.jsonData = jsonData; }

        public S2C_OpenCarShop(FriendlyByteBuf buf) {
            this.jsonData = buf.readUtf(262144);
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(jsonData, 262144);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientEvoMarketsHandler.openCarShop(jsonData);
                });
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2S_BuyCarPacket {
        public final String bUuid, wUuid, eUuid, aUuid;

        public C2S_BuyCarPacket(String b, String w, String e, String a) {
            this.bUuid = b; this.wUuid = w; this.eUuid = e; this.aUuid = a;
        }

        public C2S_BuyCarPacket(FriendlyByteBuf buf) {
            this.bUuid = buf.readUtf(256);
            this.wUuid = buf.readUtf(256);
            this.eUuid = buf.readUtf(256);
            this.aUuid = buf.readUtf(256);
        }

        public void toBytes(FriendlyByteBuf buf) {
            buf.writeUtf(bUuid, 256);
            buf.writeUtf(wUuid, 256);
            buf.writeUtf(eUuid, 256);
            buf.writeUtf(aUuid, 256);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player == null) return;

                double total = 0;
                org.evocraft.evomarkets.shop.CarShopManager.CarPart body = getPart("BODY", bUuid);
                org.evocraft.evomarkets.shop.CarShopManager.CarPart wheels = getPart("WHEELS", wUuid);
                org.evocraft.evomarkets.shop.CarShopManager.CarPart engine = getPart("ENGINE", eUuid);
                org.evocraft.evomarkets.shop.CarShopManager.CarPart attach = getPart("ATTACHMENTS", aUuid);

                if (body != null) total += body.price;
                if (wheels != null) total += wheels.price;
                if (engine != null) total += engine.price;
                if (attach != null) total += attach.price;

                if (total == 0) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cYour cart is empty!")); return;
                }

                org.evocraft.evocore.data.PlayerStatsManager.PlayerStats stats = org.evocraft.evocore.data.PlayerStatsManager.get().getStats(player.getUUID());
                if (stats.balance < total) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§cNot enough Evo!")); return;
                }

                stats.balance -= total;
                org.evocraft.evocore.data.PlayerStatsManager.get().saveToDatabase(player.getUUID());
                org.evocraft.evocore.data.PlayerStatsManager.get().syncClient(player.getUUID());

                if (body != null) giveItem(player, body, 1);
                if (wheels != null) giveItem(player, wheels, 4); // 4 Roți!
                if (engine != null) giveItem(player, engine, 1);
                if (attach != null) giveItem(player, attach, 1);

                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§aPurchased car parts for " + EvoCurrencyFormatter.formatWithCurrency(total) + "!"));
            });
            ctx.get().setPacketHandled(true);
        }

        private org.evocraft.evomarkets.shop.CarShopManager.CarPart getPart(String cat, String uuid) {
            if (uuid.equals("none")) return null;
            java.util.List<org.evocraft.evomarkets.shop.CarShopManager.CarPart> parts = org.evocraft.evomarkets.shop.CarShopManager.shopData.get(cat);
            if (parts != null) for (org.evocraft.evomarkets.shop.CarShopManager.CarPart p : parts) if (p.uuid.equals(uuid)) return p;
            return null;
        }

        private void giveItem(ServerPlayer player, org.evocraft.evomarkets.shop.CarShopManager.CarPart part, int amount) {
            @SuppressWarnings("deprecation")
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(part.id));
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item, amount);

                if (part.nbt != null && !part.nbt.isEmpty()) {
                    try { stack.setTag(net.minecraft.nbt.TagParser.parseTag(part.nbt)); } catch (Exception ignored) {}
                }

                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
        }
    }

    // ========================================================
    // PACHETE SHOP NORMAL
    // ========================================================
    public static class S2C_SyncShop {
        private final String json;
        public S2C_SyncShop(String json) { this.json = json; }
        public S2C_SyncShop(FriendlyByteBuf buffer) { byte[] d = buffer.readByteArray(); this.json = decompress(d); }
        public void toBytes(FriendlyByteBuf buffer) { buffer.writeByteArray(compress(json)); }
        public void handle(Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ShopConfigManager.get().loadFromJson(json);
            }));
            context.setPacketHandled(true);
        }
    }

    public static class C2S_ShopTrade {
        public final String c,t; public final int i,a;
        public final boolean includeBackpackItems, skipBackpackPrompt;
        public C2S_ShopTrade(String c, int i, String t, int a){this(c, i, t, a, false, false);}
        public C2S_ShopTrade(String c, int i, String t, int a, boolean includeBackpackItems, boolean skipBackpackPrompt){this.c=c;this.i=i;this.t=t;this.a=a;this.includeBackpackItems=includeBackpackItems;this.skipBackpackPrompt=skipBackpackPrompt;}
        public C2S_ShopTrade(FriendlyByteBuf b){this.c=b.readUtf();this.i=b.readInt();this.t=b.readUtf();this.a=b.readInt();this.includeBackpackItems=b.readBoolean();this.skipBackpackPrompt=b.readBoolean();}
        public void toBytes(FriendlyByteBuf b){b.writeUtf(c);b.writeInt(i);b.writeUtf(t);b.writeInt(a);b.writeBoolean(includeBackpackItems);b.writeBoolean(skipBackpackPrompt);}
        public void handle(Supplier<NetworkEvent.Context> ctx){
            ctx.get().enqueueWork(()->{
                ServerPlayer p=ctx.get().getSender();
                if(p!=null && p.containerMenu instanceof ShopMenu m) {
                    m.performTransaction(this.c,i,"BUY".equals(t),"SELL".equals(t),a,includeBackpackItems,skipBackpackPrompt);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class S2C_BackpackSellPrompt {
        public final String categoryId;
        public final int itemIndex;
        public final int backpackCount;

        public S2C_BackpackSellPrompt(String categoryId, int itemIndex, int backpackCount) {
            this.categoryId = categoryId;
            this.itemIndex = itemIndex;
            this.backpackCount = backpackCount;
        }

        public S2C_BackpackSellPrompt(FriendlyByteBuf b) {
            this.categoryId = b.readUtf();
            this.itemIndex = b.readInt();
            this.backpackCount = b.readInt();
        }

        public void toBytes(FriendlyByteBuf b) {
            b.writeUtf(categoryId);
            b.writeInt(itemIndex);
            b.writeInt(backpackCount);
        }

        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientEvoMarketsHandler.showBackpackSellPrompt(categoryId, itemIndex, backpackCount);
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2S_EditShop {
        public final String c,a; public final int f,t;
        public C2S_EditShop(String c, int f, int t, String a){this.c=c;this.f=f;this.t=t;this.a=a;}
        public C2S_EditShop(FriendlyByteBuf b){this.c=b.readUtf();this.f=b.readInt();this.t=b.readInt();this.a=b.readUtf();}
        public void toBytes(FriendlyByteBuf b){b.writeUtf(c);b.writeInt(f);b.writeInt(t);b.writeUtf(a);}
        public void handle(Supplier<NetworkEvent.Context> ctx){
            ctx.get().enqueueWork(()->{if("SWAP".equals(a)) ShopConfigManager.get().swapItems(this.c,f,t);});
            ctx.get().setPacketHandled(true);
        }
    }

    // ========================================================
    // PACHETE AUCTION HOUSE
    // ========================================================
    public static class S2C_AuctionGlobalUpdate {
        public S2C_AuctionGlobalUpdate() {}
        public S2C_AuctionGlobalUpdate(FriendlyByteBuf b) {}
        public void toBytes(FriendlyByteBuf b) {}
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientEvoMarketsHandler.updateAuction();
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2S_AuctionFilter {
        public final String i, s; public final int o;
        public C2S_AuctionFilter(String i, String s, int o) { this.i = i; this.s = s; this.o = o; }
        public C2S_AuctionFilter(FriendlyByteBuf b) { this.i = b.readUtf(); this.s = b.readUtf(); this.o = b.readInt(); }
        public void toBytes(FriendlyByteBuf b) { b.writeUtf(i); b.writeUtf(s); b.writeInt(o); }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null && p.containerMenu instanceof org.evocraft.evomarkets.ah.AuctionMenu m) {
                    m.updateFiltersFromServer(i, s, o);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    // ========================================================
    // PACHETE TRADE
    // ========================================================
    public static class S2C_TradeNotification {
        public final String t, d;
        public S2C_TradeNotification(String t, String d) { this.t=t; this.d=d; }
        public S2C_TradeNotification(FriendlyByteBuf b) { this.t=b.readUtf(); this.d=b.readUtf(); }
        public void toBytes(FriendlyByteBuf b) { b.writeUtf(t); b.writeUtf(d); }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientEvoMarketsHandler.showTradeNotification(t, d);
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class S2C_TradeSync {
        public final double m1, m2; public final boolean a1, a2; public final int cd; public final String c, n1, n2;
        public S2C_TradeSync(double m1, double m2, boolean a1, boolean a2, int cd, String c, String n1, String n2) {
            this.m1=m1; this.m2=m2; this.a1=a1; this.a2=a2; this.cd=cd; this.c=c; this.n1=n1; this.n2=n2;
        }
        public S2C_TradeSync(FriendlyByteBuf b) {
            this.m1=b.readDouble(); this.m2=b.readDouble(); this.a1=b.readBoolean(); this.a2=b.readBoolean();
            this.cd=b.readInt(); this.c=b.readUtf(); this.n1=b.readUtf(); this.n2=b.readUtf();
        }
        public void toBytes(FriendlyByteBuf b) {
            b.writeDouble(m1); b.writeDouble(m2); b.writeBoolean(a1); b.writeBoolean(a2);
            b.writeInt(cd); b.writeUtf(c); b.writeUtf(n1); b.writeUtf(n2);
        }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientEvoMarketsHandler.syncTradeData(m1, m2, a1, a2, cd, c, n1, n2);
            }));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class C2S_TradeAction {
        public final String a, t; public final double m;
        public C2S_TradeAction(String a, double m, String t) { this.a=a; this.m=m; this.t=t; }
        public C2S_TradeAction(FriendlyByteBuf b) { this.a=b.readUtf(); this.m=b.readDouble(); this.t=b.readUtf(); }
        public void toBytes(FriendlyByteBuf b) { b.writeUtf(a); b.writeDouble(m); b.writeUtf(t); }
        public void handle(Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer p = ctx.get().getSender();
                if (p != null) org.evocraft.evomarkets.trade.TradeManager.get().handleAction(p, a, m, t);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
