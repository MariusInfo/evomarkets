package org.evocraft.evomarkets.shop;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;

public class PlaneShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("planeshop")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    String jsonData = new Gson().toJson(PlaneShopManager.shopData);

                    EvoMarketsPacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new EvoMarketsPacketHandler.S2C_OpenPlaneShop(jsonData)
                    );

                    return 1;
                })
        );
    }
}