package org.evocraft.evomarkets.entity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.evocraft.evomarkets.EvoMarkets;

public class PlaneDealerRenderer extends MobRenderer<PlaneDealerEntity, PlayerModel<PlaneDealerEntity>> {

    // Textura separată pentru NPC-ul de avioane!
    private static final ResourceLocation TEXTURE = new ResourceLocation(EvoMarkets.MODID, "textures/entity/plane_dealer.png");

    public PlaneDealerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(PlaneDealerEntity entity) {
        return TEXTURE;
    }
}