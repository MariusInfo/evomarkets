package org.evocraft.evomarkets.entity;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

public class SpawnItemShopCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // Comanda pentru spawnare NPC Shop
        dispatcher.register(Commands.literal("spawnitemshop")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    // Verificăm dacă există deja un NPC Shop pe hartă
                    boolean exists = false;
                    for (Entity e : player.serverLevel().getAllEntities()) {
                        if (e instanceof ItemShopEntity) {
                            exists = true;
                            break;
                        }
                    }

                    if (exists) {
                        player.sendSystemMessage(Component.literal("§c✖ Există deja un NPC Shop Iteme pe hartă! Nu poți adăuga altul."));
                        return 0;
                    }

                    // AICI TRB SA TE ASIGURI CA AI ITEM_SHOP_NPC in EvoMarketsEntities
                    ItemShopEntity npc = EvoMarketsEntities.ITEM_SHOP_NPC.get().create(player.level());
                    if (npc != null) {
                        npc.setPos(player.getX(), player.getY(), player.getZ());
                        npc.setYRot(player.getYRot());
                        npc.setYHeadRot(player.getYRot());

                        player.level().addFreshEntity(npc);
                        player.sendSystemMessage(Component.literal("§a✔ NPC-ul Shop Iteme a fost creat!"));
                    }
                    return 1;
                }));

        // Comanda pentru stergere NPC Shop
        dispatcher.register(Commands.literal("removeitemshop")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();

                    List<ItemShopEntity> npcs = player.level().getEntitiesOfClass(ItemShopEntity.class, player.getBoundingBox().inflate(50.0D));

                    if (npcs.isEmpty()) {
                        player.sendSystemMessage(Component.literal("§c✖ Nu s-a găsit niciun NPC Shop Iteme în apropiere (50 blocuri)!"));
                        return 0;
                    }

                    int removed = 0;
                    for (ItemShopEntity npc : npcs) {
                        npc.discard();
                        removed++;
                    }

                    player.sendSystemMessage(Component.literal("§a✔ Au fost șterși " + removed + " NPC-uri Shop Iteme!"));
                    return removed;
                }));
    }
}