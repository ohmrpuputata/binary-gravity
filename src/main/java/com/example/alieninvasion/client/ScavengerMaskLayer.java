package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.BioFilterMaskModel;
import com.example.alieninvasion.entity.RogueScavengerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Выживший-NPC носит МАСКУ: рендерим модель противогаза на его лице (как у игрока в
 * слоте маски), чтобы он выглядел снаряжённым выживальщиком, а не просто зомби в броне.
 */
public class ScavengerMaskLayer
        extends RenderLayer<RogueScavengerEntity, HumanoidModel<RogueScavengerEntity>> {

    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/models/armor/gas_mask.png");
    private BioFilterMaskModel model;

    public ScavengerMaskLayer(RenderLayerParent<RogueScavengerEntity, HumanoidModel<RogueScavengerEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, RogueScavengerEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        if (this.model == null) {
            this.model = new BioFilterMaskModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.BIO_FILTER_MASK));
        }
        this.model.head.visible = true;
        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);
        this.model.head.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEX)),
                light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }
}
