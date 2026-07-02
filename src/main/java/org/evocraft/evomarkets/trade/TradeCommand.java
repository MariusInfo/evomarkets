package org.evocraft.evomarkets.trade;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TradeCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("trade")
                // Daca scrie doar /trade ii dam mesajul de ajutor
                .executes(ctx -> {
                    ctx.getSource().sendSystemMessage(Component.literal("§cFolosire: /trade <jucator> sau /trade accept"));
                    return 1;
                })
                // /trade accept
                .then(Commands.literal("accept")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            TradeManager.get().acceptRequest(player);
                            return 1;
                        })
                )
                // /trade <nume_jucator>
                .then(Commands.argument("jucator", EntityArgument.player())
                        .executes(ctx -> {
                            ServerPlayer sender = ctx.getSource().getPlayerOrException();
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "jucator");
                            TradeManager.get().sendRequest(sender, target);
                            return 1;
                        })
                )
        );
    }
}