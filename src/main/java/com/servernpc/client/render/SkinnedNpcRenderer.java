package com.servernpc.client.render;

import com.servernpc.entity.ReimuGoodNpcEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public class SkinnedNpcRenderer extends HumanoidMobRenderer<ReimuGoodNpcEntity, PlayerModel<ReimuGoodNpcEntity>> {
    private final ResourceLocation texture;

    public SkinnedNpcRenderer(EntityRendererProvider.Context context, ResourceLocation texture) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.texture = texture;
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(ReimuGoodNpcEntity entity) {
        return this.texture;
    }
}
