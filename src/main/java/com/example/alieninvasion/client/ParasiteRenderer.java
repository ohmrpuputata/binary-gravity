package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.ParasiteEntity;
import net.minecraft.client.model.SilverfishModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class ParasiteRenderer extends MobRenderer<ParasiteEntity, SilverfishModel<ParasiteEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/parasite.png");

    public ParasiteRenderer(EntityRendererProvider.Context context) {
        super(context, new SilverfishModel<>(context.bakeLayer(ModelLayers.SILVERFISH)), 0.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteEntity entity) {
        return TEXTURE;
    }
}
