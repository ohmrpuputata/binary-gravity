package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.PlasmaCasterEntity;
import net.minecraft.client.model.SkeletonModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Rare Plasma Caster rendered on the vanilla skeleton layout with a custom texture.
public class PlasmaCasterRenderer extends MobRenderer<PlasmaCasterEntity, SkeletonModel<PlasmaCasterEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/plasma_caster.png");

    public PlasmaCasterRenderer(EntityRendererProvider.Context context) {
        super(context, new SkeletonModel<>(context.bakeLayer(ModelLayers.SKELETON)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(PlasmaCasterEntity entity) {
        return TEXTURE;
    }
}
