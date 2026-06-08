package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.AlienBreacherEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AlienBreacherRenderer extends MobRenderer<AlienBreacherEntity, SpiderModel<AlienBreacherEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/alien_breacher.png");

    public AlienBreacherRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienBreacherEntity entity) {
        return TEXTURE;
    }
}
