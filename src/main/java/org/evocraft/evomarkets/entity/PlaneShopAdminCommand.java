package org.evocraft.evomarkets.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class PlaneShopAdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("planeshopadmin")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("add")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .then(Commands.argument("price", DoubleArgumentType.doubleArg(0))
                                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                                    String category = StringArgumentType.getString(context, "category").toUpperCase();
                                                    double price = DoubleArgumentType.getDouble(context, "price");
                                                    String name = StringArgumentType.getString(context, "name");

                                                    ItemStack handItem = player.getMainHandItem();
                                                    if (handItem.isEmpty()) {
                                                        player.sendSystemMessage(Component.literal("§c✖ Ține piesa în mână!")); return 0;
                                                    }
                                                    if (!PlaneShopManager.shopData.containsKey(category)) {
                                                        player.sendSystemMessage(Component.literal("§c✖ Categorii permise: BODY, ENGINE, ATTACHMENTS")); return 0;
                                                    }

                                                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(handItem.getItem());
                                                    String nbtStr = handItem.hasTag() ? handItem.getTag().toString() : "";
                                                    String uniqueId = UUID.randomUUID().toString();

                                                    PlaneShopManager.addPart(category, uniqueId, itemId.toString(), nbtStr, name, price);
                                                    player.sendSystemMessage(Component.literal("§a✔ Ai adăugat " + name + " (Avioane) în " + category + " - Preț: " + price));
                                                    return 1;
                                                })))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String category = StringArgumentType.getString(context, "category").toUpperCase();
                                            String name = StringArgumentType.getString(context, "name");

                                            if (!PlaneShopManager.shopData.containsKey(category)) {
                                                player.sendSystemMessage(Component.literal("§c✖ Categorii permise: BODY, ENGINE, ATTACHMENTS")); return 0;
                                            }

                                            boolean success = PlaneShopManager.removePart(category, name);
                                            if (success) {
                                                player.sendSystemMessage(Component.literal("§a✔ Ai șters piesa '" + name + "' din categoria " + category));
                                                return 1;
                                            } else {
                                                player.sendSystemMessage(Component.literal("§c✖ Nu s-a găsit nicio piesă cu numele '" + name + "'!"));
                                                return 0;
                                            }
                                        })))));
    }
}