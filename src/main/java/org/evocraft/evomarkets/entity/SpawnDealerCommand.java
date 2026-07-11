package org.evocraft.evomarkets.entity;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class SpawnDealerCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // Comanda pentru spawnare
        dispatcher.register(Commands.literal("spawndealer")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // ADAUGAT: Verificăm dacă există deja un Dealer Auto oriunde în lume
                    boolean exists = false;
                    for (Entity e : player.serverLevel().getAllEntities()) {
                        if (e instanceof CarDealerEntity) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        player.sendSystemMessage(Component.literal("§c✖ A Car Dealer already exists on the map! You cannot add another one."));
                        return 0;
                    }

                    CarDealerEntity npc = EvoMarketsEntities.CAR_DEALER.get().create(player.level());
                    if (npc != null) {
                        npc.setPos(player.getX(), player.getY(), player.getZ());
                        // Îl facem să se uite în aceeași direcție cu tine
                        npc.setYRot(player.getYRot());
                        npc.setYHeadRot(player.getYRot());

                        player.level().addFreshEntity(npc);
                        player.sendSystemMessage(Component.literal("§a✔ Car Dealer NPC was created!"));
                    }
                    return 1;
                }));

        // ADAUGAT: Comanda pentru a șterge NPC-ul
        dispatcher.register(Commands.literal("removedealer")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Căutăm dealerul într-o rază de 50 de blocuri
                    List<CarDealerEntity> npcs = player.level().getEntitiesOfClass(CarDealerEntity.class, player.getBoundingBox().inflate(50.0D));

                    if (npcs.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§c✖ No Car Dealer found nearby (50 blocks)!"));
                        return 0;
                    }

                    int removed = 0;
                    for (CarDealerEntity npc : npcs) {
                        npc.discard(); // Funcția de la Minecraft care șterge permanent entitatea
                        removed++;
                    }

                    player.sendSystemMessage(Component.literal("§a✔ Removed " + removed + " Car Dealer(s)!"));
                    return removed;
                }));
    }
}
