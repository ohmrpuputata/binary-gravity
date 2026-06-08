package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.BorerModel;
import com.example.alieninvasion.entity.BorerVehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class BorerRenderer extends EntityRenderer<BorerVehicleEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/borer.png");
    private final BorerModel model;

    public BorerRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.9F;
        this.model = new BorerModel(context.bakeLayer(ModModelLayers.BORER));
    }

    @Override
    public void render(BorerVehicleEntity entity, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(entity.getXRot() * 0.35F));
        // Big mining machine. Scaled up so the hull visibly encloses the seated driver,
        // and lifted so the bigger body sits ON the ground instead of sunk into it.
        poseStack.scale(-1.8F, -1.8F, 1.8F);
        poseStack.translate(0.0F, -0.82F, 0.0F);
        this.model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTick, 0.0F, 0.0F);
        VertexConsumer vc = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(BorerVehicleEntity entity) {
        return TEXTURE;
    }
}
