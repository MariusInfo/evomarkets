package org.evocraft.evomarkets.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.evocraft.evomarkets.EvoMarkets;

@Mod.EventBusSubscriber(modid = EvoMarkets.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EvoMarketsEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, EvoMarkets.MODID);

    public static final RegistryObject<EntityType<CarDealerEntity>> CAR_DEALER = ENTITIES.register("car_dealer",
            () -> EntityType.Builder.of(CarDealerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F) // Copiat exact de la JobNPC
                    .clientTrackingRange(8)
                    .build("car_dealer"));

    public static final RegistryObject<EntityType<ItemShopEntity>> ITEM_SHOP_NPC = ENTITIES.register("item_shop_npc",
            () -> EntityType.Builder.of(ItemShopEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("item_shop_npc"));

    public static final RegistryObject<EntityType<AuctionNpcEntity>> AUCTION_NPC = ENTITIES.register("auction_npc",
            () -> EntityType.Builder.of(AuctionNpcEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("auction_npc"));

    public static final RegistryObject<EntityType<PlaneDealerEntity>> PLANE_DEALER = ENTITIES.register("plane_dealer",
            () -> EntityType.Builder.of(PlaneDealerEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("plane_dealer"));

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(CAR_DEALER.get(), CarDealerEntity.createAttributes().build());
        event.put(ITEM_SHOP_NPC.get(), ItemShopEntity.createAttributes().build());
        event.put(AUCTION_NPC.get(), AuctionNpcEntity.createAttributes().build());
        event.put(PLANE_DEALER.get(), PlaneDealerEntity.createAttributes().build());
    }
}