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
import org.evocraft.evocore.util.EvoCurrencyFormatter;

import java.util.List;

public class AuctionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ah")

                // --- SELL ITEM (/ah sell <pret>) ---
                .then(Commands.literal("sell")
                        .then(Commands.argument("pret", DoubleArgumentType.doubleArg(1.0))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    double price = DoubleArgumentType.getDouble(ctx, "pret");
                                    ItemStack handItem = player.getMainHandItem();

                                    if (handItem.isEmpty()) {
                                        ctx.getSource().sendFailure(Component.literal("§c[!] Hold an item in your hand before selling it!"));
                                        return 0;
                                    }

                                    // The old 10 item limit was removed.

                                    AuctionListing listing = new AuctionListing(player.getUUID(), player.getScoreboardName(), handItem.copy(), price);
                                    AuctionMarketManager.get().addListing(listing);
                                    handItem.shrink(handItem.getCount());

                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[AH] Listed the item for " + EvoCurrencyFormatter.formatWithCurrency(price) + "!"), false);
                                    return 1;
                                })
                        )
                )

                // --- SALES HISTORY (/ah istoric) ---
                .then(Commands.literal("istoric")
                        .executes(ctx -> {
                            try {
                                ServerPlayer player = ctx.getSource().getPlayerOrException();
                                List<AuctionHistoryManager.Transaction> list = AuctionHistoryManager.get().getHistory(player.getUUID());

                                if (list.isEmpty()) {
                                    ctx.getSource().sendFailure(Component.literal("§cYou have not sold any items recently."));
                                    return 0;
                                }

                                ctx.getSource().sendSystemMessage(Component.literal("§2§l=== AH SALES HISTORY (Last 20) ==="));
                                for (AuctionHistoryManager.Transaction t : list) {
                                    String line = "§7[" + t.date + "] §b" + t.itemName
                                            + " §7bought by §e" + t.buyerName
                                            + " §7for §a" + EvoCurrencyFormatter.formatWithCurrency(t.price);
                                    ctx.getSource().sendSystemMessage(Component.literal(line));
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                            return 1;
                        })
                )

                // --- ADMIN COMMANDS (/ah admin setstation / removestation) ---
                .then(Commands.literal("admin")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("setstation")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    HitResult h = p.pick(5, 0, false);
                                    if (h.getType() == HitResult.Type.BLOCK) {
                                        BlockPos pos = ((BlockHitResult) h).getBlockPos();

                                        AuctionStationManager.get().addStation(p.serverLevel(), pos);

                                        ctx.getSource().sendSuccess(() -> Component.literal("§a[AH] Station set on the selected block!"), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§cYou must look at a block!"));
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

                                        ctx.getSource().sendSuccess(() -> Component.literal("§c[AH] Station removed!"), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§cYou must look at a block!"));
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }
}
