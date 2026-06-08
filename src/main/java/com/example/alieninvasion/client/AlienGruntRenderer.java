package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.AlienGruntEntity;
import com.example.alieninvasion.client.model.AlienGruntModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class AlienGruntRenderer extends MobRenderer<AlienGruntEntity, AlienGruntModel> {
    public AlienGruntRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienGruntModel(context.bakeLayer(ModModelLayers.ALIEN_GRUNT)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienGruntEntity entity) {
        return ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "textures/entity/alien_grunt.png");
    }
}
