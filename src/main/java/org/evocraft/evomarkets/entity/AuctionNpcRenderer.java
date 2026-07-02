package org.evocraft.evomarkets.entity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.evocraft.evomarkets.EvoMarkets;

public class AuctionNpcRenderer extends MobRenderer<AuctionNpcEntity, PlayerModel<AuctionNpcEntity>> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(EvoMarkets.MODID, "textures/entity/auction_npc.png");

    public AuctionNpcRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(AuctionNpcEntity entity) {
        // AM REZOLVAT EROAREA: Acum folosește textura statică, nu getSkinPath()!
        return TEXTURE;
    }
}