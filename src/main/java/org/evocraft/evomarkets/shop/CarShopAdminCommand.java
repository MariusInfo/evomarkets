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
import org.evocraft.evocore.util.EvoCurrencyFormatter;

import java.util.UUID;

public class CarShopAdminCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("carshopadmin")
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
                                                        player.sendSystemMessage(Component.literal("§cHold the part in your hand!")); return 0;
                                                    }
                                                    if (!CarShopManager.shopData.containsKey(category)) {
                                                        player.sendSystemMessage(Component.literal("§cCategories: BODY, WHEELS, ENGINE, ATTACHMENTS")); return 0;
                                                    }

                                                    ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(handItem.getItem());
                                                    // SALVĂM NBT-UL ITEMULUI PENTRU A PĂSTRA MODELUL 3D!
                                                    String nbtStr = handItem.hasTag() ? handItem.getTag().toString() : "";
                                                    String uniqueId = UUID.randomUUID().toString();

                                                    CarShopManager.addPart(category, uniqueId, itemId.toString(), nbtStr, name, price);
                                                    player.sendSystemMessage(Component.literal("§aAdded " + name + " (with 3D model) to " + category + " - Price: " + EvoCurrencyFormatter.formatWithCurrency(price)));
                                                    return 1;
                                                })))))
                // ADAUGAT: Partea pentru comanda de remove
                .then(Commands.literal("remove")
                        .then(Commands.argument("category", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            String category = StringArgumentType.getString(context, "category").toUpperCase();
                                            String name = StringArgumentType.getString(context, "name");

                                            if (!CarShopManager.shopData.containsKey(category)) {
                                                player.sendSystemMessage(Component.literal("§cCategories: BODY, WHEELS, ENGINE, ATTACHMENTS")); return 0;
                                            }

                                            boolean success = CarShopManager.removePart(category, name);
                                            if (success) {
                                                player.sendSystemMessage(Component.literal("§aRemoved part '" + name + "' from category " + category));
                                                return 1;
                                            } else {
                                                player.sendSystemMessage(Component.literal("§cNo part named '" + name + "' was found in this category!"));
                                                return 0;
                                            }
                                        })))));
    }
}
