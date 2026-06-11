package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.DrillEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * Orbital burrowing drill: an armoured capsule with side thrusters and a
 * three-stage drill cone whose segments counter-rotate, plus machine shudder.
 * UV (64x64) mirrored in generate_machine_textures.py.
 */
public class DrillModel extends EntityModel<DrillEntity> {
    private final ModelPart body;
    private final ModelPart cone1;
    private final ModelPart cone2;
    private final ModelPart tip;

    public DrillModel(ModelPart root) {
        this.body = root.getChild("body");
        this.cone1 = root.getChild("cone1");
        this.cone2 = root.getChild("cone2");
        this.tip = root.getChild("tip");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        body.addOrReplaceChild("thruster_r", CubeListBuilder.create()
                        .texOffs(33, 0).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(-5.0F, -6.0F, 0.0F));
        body.addOrReplaceChild("thruster_l", CubeListBuilder.create()
                        .texOffs(33, 0).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 6.0F, 3.0F),
                PartPose.offset(5.0F, -6.0F, 0.0F));

        root.addOrReplaceChild("cone1", CubeListBuilder.create()
                        .texOffs(0, 19).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 4.0F, 6.0F),
                PartPose.offset(0.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("cone2", CubeListBuilder.create()
                        .texOffs(0, 30).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.offset(0.0F, 18.0F, 0.0F));
        root.addOrReplaceChild("tip", CubeListBuilder.create()
                        .texOffs(0, 39).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                PartPose.offset(0.0F, 22.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(DrillEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.cone1.yRot = ageInTicks * 0.9F;
        this.cone2.yRot = -ageInTicks * 1.3F;
        this.tip.yRot = ageInTicks * 1.8F;
        this.body.zRot = Mth.sin(ageInTicks * 1.7F) * 0.02F;
        this.body.xRot = Mth.cos(ageInTicks * 1.9F) * 0.02F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        cone1.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        cone2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        tip.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
