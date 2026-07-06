package org.evocraft.evomarkets.entity;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class SpawnPlaneDealerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spawnplanedealer")
                .requires(source -> source.hasPermission(2)) // Comanda necesită drepturi de OP/Admin
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Creează entitatea pe baza tipului înregistrat în EvoMarketsEntities
                    PlaneDealerEntity dealer = EvoMarketsEntities.PLANE_DEALER.get().create(player.serverLevel());

                    if (dealer != null) {
                        Vec3 position = player.position();
                        // Poziționează NPC-ul exact unde este jucătorul și la aceeași rotație
                        dealer.moveTo(position.x, position.y, position.z, player.getYRot(), player.getXRot());

                        // Adaugă entitatea în lume
                        player.serverLevel().addFreshEntity(dealer);

                        player.sendSystemMessage(Component.literal("§aPlane dealer spawned successfully!"));
                        return 1;
                    } else {
                        player.sendSystemMessage(Component.literal("§cError: Could not create the entity!"));
                        return 0;
                    }
                })
        );
    }
}
