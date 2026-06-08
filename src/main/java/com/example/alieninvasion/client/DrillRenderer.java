package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.DrillModel;
import com.example.alieninvasion.entity.DrillEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public class DrillRenderer extends EntityRenderer<DrillEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/drill.png");
    private final DrillModel model;

    public DrillRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.model = new DrillModel(context.bakeLayer(ModModelLayers.DRILL));
    }

    @Override
    public void render(DrillEntity entity, float yaw, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        // Flip so the drill bit points downward, then spin fast around the vertical axis.
        poseStack.scale(-2.2F, -2.2F, 2.2F); // much bigger, hulking drill
        poseStack.translate(0.0F, -1.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 40.0F));
        VertexConsumer vc = buffer.getBuffer(this.model.renderType(TEXTURE));
        this.model.renderToBuffer(poseStack, vc, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DrillEntity entity) {
        return TEXTURE;
    }
}
