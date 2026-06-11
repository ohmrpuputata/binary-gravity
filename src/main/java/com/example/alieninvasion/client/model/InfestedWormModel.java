package com.example.alieninvasion.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Segmented infection worm: a fanged head and four tapering body rings that
 * slither with a travelling sine wave. Used by the worm itself (all growth
 * stages — size comes from the SCALE attribute) and by the brain parasite.
 * UV (32x32) is mirrored in generate_worm_raptor_textures.py.
 */
public class InfestedWormModel<T extends Mob> extends EntityModel<T> {
    private final ModelPart head;
    private final ModelPart seg1;
    private final ModelPart seg2;
    private final ModelPart seg3;
    private final ModelPart tail;

    public InfestedWormModel(ModelPart root) {
        this.head = root.getChild("head");
        this.seg1 = root.getChild("seg1");
        this.seg2 = root.getChild("seg2");
        this.seg3 = root.getChild("seg3");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Chain along +Z: head in front, rings behind, all pivoting at their front
        // edge so the sine wave reads as slithering, not jitter.
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 21.5F, -4.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(0, 24).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.0F, 3.0F),
                PartPose.offsetAndRotation(0.0F, 1.0F, -1.0F, 0.3F, 0.0F, 0.0F));
        root.addOrReplaceChild("seg1", CubeListBuilder.create()
                        .texOffs(0, 9).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 4.0F),
                PartPose.offset(0.0F, 21.0F, -4.0F));
        root.addOrReplaceChild("seg2", CubeListBuilder.create()
                        .texOffs(0, 9).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 21.5F, 0.0F));
        root.addOrReplaceChild("seg3", CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F),
                PartPose.offset(0.0F, 22.0F, 4.0F));
        root.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(18, 0).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F),
                PartPose.offset(0.0F, 22.5F, 8.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // Travelling wave down the body: each ring lags the one in front.
        float speed = 0.9F;
        float amp = 0.45F * Math.max(0.35F, limbSwingAmount) + 0.06F;
        float t = limbSwing * speed + ageInTicks * 0.08F;
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F) * 0.4F + Mth.sin(t) * amp * 0.5F;
        this.head.xRot = headPitch * ((float) Math.PI / 180F) * 0.4F;
        this.seg1.yRot = Mth.sin(t - 0.9F) * amp;
        this.seg2.yRot = Mth.sin(t - 1.8F) * amp;
        this.seg3.yRot = Mth.sin(t - 2.7F) * amp;
        this.tail.yRot = Mth.sin(t - 3.6F) * amp * 1.4F;

        ModelPart jaw = this.head.getChild("jaw");
        jaw.xRot = entity.isAggressive()
                ? 0.3F + Math.abs(Mth.sin(ageInTicks * 0.6F)) * 0.7F
                : 0.3F + Mth.sin(ageInTicks * 0.15F) * 0.08F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        seg1.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        seg2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        seg3.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        tail.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
