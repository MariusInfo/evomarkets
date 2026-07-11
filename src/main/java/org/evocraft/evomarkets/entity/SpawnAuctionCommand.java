package org.evocraft.evomarkets.entity;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SpawnAuctionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(Commands.literal("spawnauction")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // AM ȘTERS VERIFICAREA! Acum forțează spawn-ul indiferent ce ar fi!
                    AuctionNpcEntity npc = EvoMarketsEntities.AUCTION_NPC.get().create(player.serverLevel());

                    if (npc != null) {
                        Vec3 pos = player.position();
                        npc.moveTo(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());

                        // ÎL FACEM SĂ STRĂLUCEASCĂ SĂ-L VEDEM PRIN PEREȚ

                        player.serverLevel().addFreshEntity(npc);
                        player.sendSystemMessage(Component.literal("§a✔ NPC was force-spawned on the map!"));
                    }
                    return 1;
                }));

        dispatcher.register(Commands.literal("removeauction")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Mărim raza la 200 de blocuri să prindem toate fantomele
                    List<AuctionNpcEntity> npcs = player.level().getEntitiesOfClass(AuctionNpcEntity.class, player.getBoundingBox().inflate(200.0D));

                    if (npcs.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§c✖ No NPC found within 200 blocks."));
                        return 0;
                    }

                    int removed = 0;
                    for (AuctionNpcEntity npc : npcs) {
                        npc.discard();
                        removed++;
                    }

                    player.sendSystemMessage(Component.literal("§a✔ Removed " + removed + " NPC(s)!"));
                    return removed;
                }));
    }
}
