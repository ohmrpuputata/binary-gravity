package com.example.alieninvasion.client;

import com.example.alieninvasion.entity.RadiationBoltEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;

public class RadiationBoltRenderer extends EntityRenderer<RadiationBoltEntity> {
    private static final ResourceLocation DUMMY_TEXTURE = ResourceLocation.withDefaultNamespace("textures/misc/unknown.png");

    public RadiationBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RadiationBoltEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // Render nothing to make the physical item model invisible, shooting only particles.
    }

    @Override
    public ResourceLocation getTextureLocation(RadiationBoltEntity entity) {
        return DUMMY_TEXTURE;
    }
}
