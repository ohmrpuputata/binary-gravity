package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienHumanoidModel;
import com.example.alieninvasion.entity.AlienBruteEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// BRUTE build of the shared swarm-humanoid skeleton, with glowing eyes.
public class AlienBruteRenderer extends MobRenderer<AlienBruteEntity, AlienHumanoidModel<AlienBruteEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_brute.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_brute_eyes.png");

    public AlienBruteRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienHumanoidModel<>(context.bakeLayer(ModModelLayers.ALIEN_BRUTE),
                AlienHumanoidModel.Variant.BRUTE), 0.9F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    protected void scale(AlienBruteEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.7F, 1.7F, 1.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienBruteEntity entity) {
        return TEXTURE;
    }
}
