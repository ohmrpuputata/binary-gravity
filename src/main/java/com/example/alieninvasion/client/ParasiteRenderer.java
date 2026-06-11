package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.InfestedWormModel;
import com.example.alieninvasion.entity.ParasiteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Brain parasite on the custom worm model - a small sickly-green broodling.
public class ParasiteRenderer extends MobRenderer<ParasiteEntity, InfestedWormModel<ParasiteEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/parasite.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/parasite_eyes.png");

    public ParasiteRenderer(EntityRendererProvider.Context context) {
        super(context, new InfestedWormModel<>(context.bakeLayer(ModModelLayers.PARASITE)), 0.25F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    protected void scale(ParasiteEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.55F, 0.55F, 0.55F);
    }

    @Override
    public ResourceLocation getTextureLocation(ParasiteEntity entity) {
        return TEXTURE;
    }
}
