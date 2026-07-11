package org.evocraft.evomarkets.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;

public class ShopCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")

                // --- COMENZI ADMIN (Necesita permisiunea 2) ---
                .then(Commands.literal("admin")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("setstation")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                                    HitResult h = p.pick(5, 0, false);
                                    if (h.getType() == HitResult.Type.BLOCK) {
                                        BlockPos pos = ((BlockHitResult) h).getBlockPos();
                                        ShopStationManager.get().addStation(p.serverLevel(), pos);
                                        ctx.getSource().sendSuccess(() -> Component.literal("§a[Shop] Station set successfully on the selected block!"), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§c[!] You must look at a block!"));
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
                                        ShopStationManager.get().removeStation(p.serverLevel(), pos);
                                        ctx.getSource().sendSuccess(() -> Component.literal("§c[Shop] Station removed from this block!"), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§c[!] You must look at the block set as a station!"));
                                    }
                                    return 1;
                                })
                        )
                )

                .then(Commands.literal("reload")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            ShopConfigManager.get().load();
                            ShopConfigManager.get().syncToAll();
                            ctx.getSource().sendSuccess(() -> Component.literal("§a[Evo Shop] §fShop reloaded and synchronized for §eALL §fplayers!"), true);
                            return 1;
                        }))

                .then(Commands.literal("add_category")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.string())
                                        .executes(ctx -> {
                                            String id = StringArgumentType.getString(ctx, "id");
                                            String name = StringArgumentType.getString(ctx, "name");
                                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                                            ItemStack hand = player.getMainHandItem();
                                            if(hand.isEmpty()) {
                                                ctx.getSource().sendFailure(Component.literal("Hold an item in your hand for the icon!"));
                                                return 0;
                                            }
                                            String icon = ForgeRegistries.ITEMS.getKey(hand.getItem()).toString();
                                            ShopConfigManager.get().addCategory(id, name, icon);
                                            ctx.getSource().sendSuccess(() -> Component.literal("§aCategory created: " + name), true);
                                            return 1;
                                        }))))

                .then(Commands.literal("add_item")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .then(Commands.argument("buy", DoubleArgumentType.doubleArg(0))
                                        .then(Commands.argument("sell", DoubleArgumentType.doubleArg(0))
                                                .executes(ctx -> {
                                                    String cat = StringArgumentType.getString(ctx, "category");
                                                    double buy = DoubleArgumentType.getDouble(ctx, "buy");
                                                    double sell = DoubleArgumentType.getDouble(ctx, "sell");
                                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                                    ItemStack hand = player.getMainHandItem();
                                                    if (hand.isEmpty()) {
                                                        ctx.getSource().sendFailure(Component.literal("Hold an item in your hand!"));
                                                        return 0;
                                                    }
                                                    String itemId = ForgeRegistries.ITEMS.getKey(hand.getItem()).toString();
                                                    ShopConfigManager.get().addItem(cat, itemId, buy, sell);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("§aItem added to " + cat), true);
                                                    return 1;
                                                })))))

                .then(Commands.literal("remove_item")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .then(Commands.argument("item_id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String cat = StringArgumentType.getString(ctx, "category");
                                            String itemId = StringArgumentType.getString(ctx, "item_id");
                                            boolean success = ShopConfigManager.get().removeItem(cat, itemId);

                                            if (success) {
                                                ctx.getSource().sendSuccess(() -> Component.literal("§aItem removed from " + cat), true);
                                            } else {
                                                ctx.getSource().sendFailure(Component.literal("§cCould not find that item or category!"));
                                            }
                                            return 1;
                                        }))))

                .then(Commands.literal("remove_category")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.argument("category", StringArgumentType.word())
                                .executes(ctx -> {
                                    String cat = StringArgumentType.getString(ctx, "category");
                                    boolean success = ShopConfigManager.get().removeCategory(cat);

                                    if (success) {
                                        ctx.getSource().sendSuccess(() -> Component.literal("§aCategory removed: " + cat), true);
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("§cCould not find that category!"));
                                    }
                                    return 1;
                                })))
        );
    }
}
