package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.AlienGruntEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class AlienGruntModel extends EntityModel<AlienGruntEntity> {
        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public AlienGruntModel(ModelPart root) {
                this.head = root.getChild("head");
                this.body = root.getChild("body");
                this.rightArm = root.getChild("right_arm");
                this.leftArm = root.getChild("left_arm");
                this.rightLeg = root.getChild("right_leg");
                this.leftLeg = root.getChild("left_leg");
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                // HEAD: Elongated, skeletal, unhinged jaw
                PartDefinition head = partdefinition.addOrReplaceChild("head", CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-3.5F, -8.0F, -4.0F, 7.0F, 6.0F, 8.0F) // Cranium
                                .texOffs(32, 0).addBox(-3.0F, -2.0F, -4.0F, 6.0F, 1.0F, 6.0F), // Upper jaw
                                PartPose.offset(0.0F, 0.0F, -2.0F)); // Hunched forward

                head.addOrReplaceChild("lower_jaw", CubeListBuilder.create()
                                .texOffs(32, 8).addBox(-2.5F, 0.0F, -5.0F, 5.0F, 2.0F, 6.0F)
                                .texOffs(32, 17).addBox(-2.5F, 2.0F, -4.0F, 5.0F, 1.0F, 4.0F), // Chin spike
                                PartPose.rotation(0.3F, 0.0F, 0.0F));

                head.addOrReplaceChild("left_eye_stalk",
                                CubeListBuilder.create().texOffs(0, 0).addBox(3.0F, -7.0F, -2.0F, 2.0F, 2.0F, 2.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                // BODY: Ribcage, exposed spine
                PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create()
                                .texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 5.0F, 4.0F).mirror() // Upper chest
                                .texOffs(16, 26).addBox(-3.0F, 5.0F, -1.5F, 6.0F, 4.0F, 3.0F).mirror(), // Mid torso
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                // Exposed Ribs
                body.addOrReplaceChild("ribs_right",
                                CubeListBuilder.create().texOffs(50, 0).addBox(-4.5F, 3.0F, -2.5F, 1.0F, 4.0F, 5.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));
                body.addOrReplaceChild("ribs_left",
                                CubeListBuilder.create().texOffs(50, 0).addBox(3.5F, 3.0F, -2.5F, 1.0F, 4.0F, 5.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                // Pelvis
                body.addOrReplaceChild("pelvis",
                                CubeListBuilder.create().texOffs(16, 34).addBox(-3.5F, 9.0F, -2.0F, 7.0F, 3.0F, 4.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                // RIGHT ARM: Long, clawed
                PartDefinition right_arm = partdefinition.addOrReplaceChild("right_arm", CubeListBuilder.create()
                                .texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 3.0F, 4.0F, 4.0F) // Shoulder
                                .texOffs(40, 25).addBox(-2.5F, 2.0F, -1.5F, 2.0F, 10.0F, 3.0F) // Upper arm
                                .texOffs(40, 39).addBox(-3.0F, 12.0F, -2.0F, 3.0F, 4.0F, 4.0F), // Forearm
                                PartPose.offset(-5.0F, 2.0F, 0.0F));

                right_arm.addOrReplaceChild("claw1",
                                CubeListBuilder.create().texOffs(40, 48).addBox(-2.0F, 16.0F, -2.0F, 1.0F, 3.0F, 1.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));
                right_arm.addOrReplaceChild("claw2",
                                CubeListBuilder.create().texOffs(40, 48).addBox(-1.0F, 16.0F, 0.0F, 1.0F, 3.0F, 1.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                // LEFT ARM: Withered, club-like
                PartDefinition left_arm = partdefinition.addOrReplaceChild("left_arm", CubeListBuilder.create()
                                .texOffs(32, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 6.0F, 4.0F) // Hump shoulder
                                .texOffs(32, 59).addBox(0.0F, 4.0F, -1.5F, 2.0F, 8.0F, 3.0F), // Arm
                                PartPose.offset(5.0F, 2.0F, 0.0F));

                // LEGS: Digitigrade-ish, twitchy
                partdefinition.addOrReplaceChild("right_leg", CubeListBuilder.create()
                                .texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F) // Thigh
                                .texOffs(0, 27).addBox(-1.5F, 6.0F, -1.0F, 3.0F, 6.0F, 3.0F) // Shin
                                .texOffs(0, 37).addBox(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 2.0F), // Foot spike
                                PartPose.offset(-2.0F, 12.0F, 0.0F));

                partdefinition.addOrReplaceChild("left_leg", CubeListBuilder.create()
                                .texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F).mirror(false)
                                .texOffs(0, 27).mirror().addBox(-1.5F, 6.0F, -1.0F, 3.0F, 6.0F, 3.0F).mirror(false)
                                .texOffs(0, 37).mirror().addBox(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 2.0F).mirror(false),
                                PartPose.offset(2.0F, 12.0F, 0.0F));

                return LayerDefinition.create(meshdefinition, 64, 64);
        }

        @Override
        public void setupAnim(AlienGruntEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                        float netHeadYaw, float headPitch) {
                this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
                this.head.xRot = headPitch * ((float) Math.PI / 180F) + 0.3F; // Always looking slightly up/hunched

                // Twitchy, erratic movement
                float twitch = Mth.sin(ageInTicks * 0.8F) * 0.1F;

                this.head.zRot = twitch * 0.5F;

                // Walk animation - Limping
                this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
                this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

                // Arm sway - Asymmetrical
                this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 2.0F * limbSwingAmount * 0.5F;
                this.rightArm.zRot = 0.1F + twitch;

                this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.0F * limbSwingAmount * 0.5F - 0.5F; // Held closer
                                                                                                         // to body

                // ATTACK ANIMATION
                if (entity.isAggressive()) {
                        float attackSwing = Mth.sin(ageInTicks * 0.8F);
                        // Rapid claw swipes
                        this.rightArm.xRot = -1.5F + attackSwing * 0.5F;
                        this.rightArm.yRot = attackSwing * 0.3F;

                        // Jaw snapping
                        ModelPart lowerJaw = this.head.getChild("lower_jaw");
                        lowerJaw.xRot = 0.3F + Math.abs(Mth.sin(ageInTicks * 0.5F)) * 0.5F;
                } else {
                        ModelPart lowerJaw = this.head.getChild("lower_jaw");
                        lowerJaw.xRot = 0.3F + twitch;
                }
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                        int packedOverlay,
                        int color) {
                head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                rightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                leftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
}
