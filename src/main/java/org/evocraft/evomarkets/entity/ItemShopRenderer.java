package org.evocraft.evomarkets.entity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.evocraft.evomarkets.EvoMarkets;

public class ItemShopRenderer extends MobRenderer<ItemShopEntity, PlayerModel<ItemShopEntity>> {

    // IMPORTANT: Verifică dacă textura ta se numește "item_shop_npc.png" sau altfel, și modifică aici dacă e nevoie!
    private static final ResourceLocation TEXTURE = new ResourceLocation(EvoMarkets.MODID, "textures/entity/item_shop_npc.png");

    public ItemShopRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(ItemShopEntity entity) {
        return TEXTURE;
    }
}