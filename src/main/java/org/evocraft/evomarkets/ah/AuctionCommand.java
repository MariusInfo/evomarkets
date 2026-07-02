package org.evocraft.evomarkets.ah;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class AuctionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ah")

                // --- VÂNZARE ITEM (/ah sell <pret>) ---
                .then(Commands.literal("sell")
                        .then(Commands.argument("pret", DoubleArgumentType.doubleArg(1.0))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    double price = DoubleArgumentType.getDouble(ctx, "pret");
                                    ItemStack handItem = player.getMainHandItem();

                                    if (handItem.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("§c[!] Trebuie să ții un item în mână pentru a-l vinde!"));
                                        return 0;
                                    }

                                    // LIMITA DE 10 ITEME A FOST STEARSA COMPLET DE AICI!

                                    AuctionListing listing = new AuctionListing(player.getUUID(), player.getScoreboardName(), handItem.copy(), price);
                                    AuctionMarketManager.get().addListing(listing);
                                    handItem.shrink(handItem.getCount());

                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[AH] Ai pus itemul la vânzare pentru " + price + " Lei!"), false);
                                    return 1;
                                })
                        )
                )

                // --- ISTORIC VANZARI (/ah istoric) ---
                .then(Commands.literal("istoric")
                        .executes(ctx -> {
                            try {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                List<AuctionHistoryManager.Transaction> list = AuctionHistoryManager.get().getHistory(player.getUUID());

                                if (list.isEmpty()) {
                                    ctx.getSource().sendFailure(Component.literal("§cNu ai vandut niciun item recent."));
                                    return 0;
                                }

                                ctx.getSource().sendSystemMessage(Component.literal("§2§l=== ISTORIC VANZARI AH (Ultimele 20) ==="));
                                for (AuctionHistoryManager.Transaction t : list) {
                                    String line = String.format("§7[%s] §b%s §7cumparat de §e%s §7cu §a%.2f Lei §7(Taxa: §c%.2f§7)",
                                            t.date, t.itemName, t.buyerName, t.price, t.tax);
                                    ctx.getSource().sendSystemMessage(Component.literal(line));
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                            return 1;
                        })
                )

                // --- COMENZI DE ADMIN (/ah admin setstation / removestation) ---
                .then(Commands.literal("admin")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("setstation")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    HitResult h = p.pick(5, 0, false);
                                    if (h.getType() == HitResult.Type.BLOCK) {
                                        BlockPos pos = ((BlockHitResult) h).getBlockPos();

                                        AuctionStationManager.get().addStation(p.serverLevel(), pos);

                                        ctx.getSource().sendSuccess(() -> Component.literal("§a[AH] Stație setată pe blocul selectat!"), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§cTrebuie să te uiți la un bloc!"));
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("removestation")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    HitResult h = p.pick(5, 0, false);
                                    if (h.getType() == HitResult.Type.BLOCK) {
                                        BlockPos pos = ((BlockHitResult) h).getBlockPos();

                                        AuctionStationManager.get().removeStation(p.serverLevel(), pos);

                                        ctx.getSource().sendSuccess(() -> Component.literal("§c[AH] Stație ștearsă!"), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§cTrebuie să te uiți la un bloc!"));
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }
}