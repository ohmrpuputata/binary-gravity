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
 * Raptor: horizontal body, long snapping snout, two strong digitigrade legs,
 * stubby fore-claws and a counterbalance tail that whips while it sprints.
 * UV (64x64) is mirrored in generate_worm_raptor_textures.py.
 */
public class AlienRaptorModel<T extends Mob> extends EntityModel<T> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public AlienRaptorModel(ModelPart root) {
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Horizontal torso, hip pivot at y=13.
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -3.0F, -7.0F, 6.0F, 6.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, 13.0F, 0.0F, -0.1F, 0.0F, 0.0F));

        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(33, 0).addBox(-2.0F, -2.5F, -6.0F, 4.0F, 3.0F, 6.0F)   // skull + snout
                        .texOffs(33, 10).addBox(-1.5F, -4.0F, -3.0F, 3.0F, 2.0F, 3.0F), // crest
                PartPose.offset(0.0F, -1.0F, -7.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(33, 16).addBox(-1.5F, 0.0F, -5.0F, 3.0F, 1.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.5F, -1.0F, 0.25F, 0.0F, 0.0F));

        PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 8.0F),
                PartPose.offsetAndRotation(0.0F, -0.5F, 3.0F, 0.12F, 0.0F, 0.0F));
        tail.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                        .texOffs(0, 29).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 7.0F),
                PartPose.offset(0.0F, 0.0F, 8.0F));

        body.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(23, 17).addBox(-1.0F, 0.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(-3.0F, 0.5F, -5.0F, -0.4F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
                        .texOffs(23, 17).addBox(0.0F, 0.0F, -1.0F, 1.0F, 4.0F, 2.0F),
                PartPose.offsetAndRotation(3.0F, 0.5F, -5.0F, -0.4F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(48, 24).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F)   // thigh
                        .texOffs(48, 35).addBox(-1.0F, 5.5F, -0.5F, 2.0F, 4.0F, 2.0F)   // shin
                        .texOffs(48, 42).addBox(-1.5F, 9.0F, -3.0F, 3.0F, 2.0F, 4.0F),  // foot claw
                PartPose.offset(-2.5F, 13.0F, 1.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
                        .texOffs(48, 24).addBox(-1.5F, 0.0F, -2.0F, 3.0F, 6.0F, 4.0F)
                        .texOffs(48, 35).addBox(-1.0F, 5.5F, -0.5F, 2.0F, 4.0F, 2.0F)
                        .texOffs(48, 42).addBox(-1.5F, 9.0F, -3.0F, 3.0F, 2.0F, 4.0F),
                PartPose.offset(2.5F, 13.0F, 1.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // Sprinting gait: long fast strides, body bobs, tail counter-whips.
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.8F) * 1.5F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.8F + (float) Math.PI) * 1.5F * limbSwingAmount;
        this.body.xRot = -0.1F + Mth.cos(limbSwing * 1.6F) * 0.05F * limbSwingAmount;
        float tailSwing = Mth.cos(limbSwing * 0.8F) * 0.4F * limbSwingAmount;
        this.tail.yRot = tailSwing + Mth.sin(ageInTicks * 0.1F) * 0.12F;
        this.tail.getChild("tail_tip").yRot = tailSwing * 1.5F + Mth.sin(ageInTicks * 0.1F + 0.7F) * 0.2F;

        this.rightArm.xRot = -0.4F + Mth.sin(ageInTicks * 0.3F) * 0.1F;
        this.leftArm.xRot = -0.4F + Mth.sin(ageInTicks * 0.3F + 1.2F) * 0.1F;

        ModelPart jaw = this.head.getChild("jaw");
        if (entity.isAggressive()) {
            jaw.xRot = 0.25F + Math.abs(Mth.sin(ageInTicks * 0.55F)) * 0.65F;
            this.rightArm.xRot = -1.0F;
            this.leftArm.xRot = -1.0F;
        } else {
            jaw.xRot = 0.25F + Mth.sin(ageInTicks * 0.12F) * 0.06F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
