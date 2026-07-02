package org.evocraft.evomarkets.shop;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import org.evocraft.evomarkets.network.EvoMarketsPacketHandler;

public class CarShopCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("carshop")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Creăm un Gson nou aici ca să fentăm eroarea de "private access"
                    String jsonData = new Gson().toJson(CarShopManager.shopData);

                    // Trimitem pachetul către client să deschidă interfața grafică!
                    EvoMarketsPacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new EvoMarketsPacketHandler.S2C_OpenCarShop(jsonData)
                    );

                    return 1;
                })
        );
    }
}