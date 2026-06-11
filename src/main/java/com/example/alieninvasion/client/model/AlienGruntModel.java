package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.AlienGruntEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * The grunt: a lean grey-green creature with a swollen cranium, two huge glossy
 * eyes (lit by {@link com.example.alieninvasion.client.AlienGruntEyesLayer}),
 * twitching antennae, an exposed spine ridge, a whipping tail and long clawed
 * arms. Digitigrade legs + hunched idle make it read as "wrong" at a glance.
 *
 * UV layout is mirrored in generate_alien_textures.py - keep them in sync.
 */
public class AlienGruntModel extends EntityModel<AlienGruntEntity> {
        private final ModelPart head;
        private final ModelPart body;
        private final ModelPart tail;
        private final ModelPart rightArm;
        private final ModelPart leftArm;
        private final ModelPart rightLeg;
        private final ModelPart leftLeg;

        public AlienGruntModel(ModelPart root) {
                this.head = root.getChild("head");
                this.body = root.getChild("body");
                this.tail = this.body.getChild("tail");
                this.rightArm = root.getChild("right_arm");
                this.leftArm = root.getChild("left_arm");
                this.rightLeg = root.getChild("right_leg");
                this.leftLeg = root.getChild("left_leg");
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition mesh = new MeshDefinition();
                PartDefinition root = mesh.getRoot();

                // HEAD: swollen dome, narrow snapping jaw, two big eyes, antennae.
                PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-4.0F, -9.0F, -4.0F, 8.0F, 7.0F, 8.0F), // cranium dome
                                PartPose.offset(0.0F, 0.0F, -2.0F)); // hunched forward

                head.addOrReplaceChild("lower_jaw", CubeListBuilder.create()
                                .texOffs(0, 16).addBox(-2.5F, 0.0F, -4.5F, 5.0F, 2.0F, 5.0F),
                                PartPose.offsetAndRotation(0.0F, -2.0F, 0.0F, 0.25F, 0.0F, 0.0F));

                head.addOrReplaceChild("eye_right", CubeListBuilder.create()
                                .texOffs(33, 0).addBox(-3.6F, -7.5F, -4.6F, 3.0F, 2.0F, 1.0F),
                                PartPose.rotation(0.0F, 0.0F, 0.12F)); // slight tilt - classic almond eyes
                head.addOrReplaceChild("eye_left", CubeListBuilder.create()
                                .texOffs(33, 0).addBox(0.6F, -7.5F, -4.6F, 3.0F, 2.0F, 1.0F),
                                PartPose.rotation(0.0F, 0.0F, -0.12F));

                head.addOrReplaceChild("antenna_right", CubeListBuilder.create()
                                .texOffs(42, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                                .texOffs(48, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 1.0F, 1.0F), // glowing tip
                                PartPose.offsetAndRotation(-2.0F, -9.0F, 0.0F, 0.0F, 0.0F, -0.25F));
                head.addOrReplaceChild("antenna_left", CubeListBuilder.create()
                                .texOffs(42, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F)
                                .texOffs(48, 0).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                                PartPose.offsetAndRotation(2.0F, -9.0F, 0.0F, 0.0F, 0.0F, 0.25F));

                // BODY: narrow chest, pinched waist, spine ridge, whip tail.
                PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                                .texOffs(0, 24).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 6.0F, 4.0F)   // chest
                                .texOffs(24, 16).addBox(-3.0F, 6.0F, -1.5F, 6.0F, 4.0F, 3.0F)  // waist
                                .texOffs(0, 35).addBox(-3.5F, 10.0F, -2.0F, 7.0F, 3.0F, 4.0F), // pelvis
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                body.addOrReplaceChild("spike1", CubeListBuilder.create()
                                .texOffs(56, 0).addBox(-0.5F, 0.5F, 2.0F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
                body.addOrReplaceChild("spike2", CubeListBuilder.create()
                                .texOffs(56, 0).addBox(-0.5F, 3.5F, 1.6F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
                body.addOrReplaceChild("spike3", CubeListBuilder.create()
                                .texOffs(56, 0).addBox(-0.5F, 6.5F, 1.2F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);

                PartDefinition tail = body.addOrReplaceChild("tail", CubeListBuilder.create()
                                .texOffs(44, 8).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 6.0F),
                                PartPose.offset(0.0F, 11.5F, 1.5F));
                tail.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                                .texOffs(44, 17).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F),
                                PartPose.offset(0.0F, 0.0F, 6.0F));

                // ARMS: long, thin, ending in two hooked claws each.
                root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                                .texOffs(24, 35).addBox(-2.0F, -1.0F, -1.5F, 3.0F, 3.0F, 3.0F) // shoulder
                                .texOffs(38, 35).addBox(-1.5F, 1.0F, -1.0F, 2.0F, 9.0F, 2.0F)  // arm
                                .texOffs(48, 35).addBox(-1.5F, 10.0F, -1.0F, 1.0F, 4.0F, 1.0F) // claw
                                .texOffs(48, 35).addBox(-0.5F, 10.0F, 0.0F, 1.0F, 4.0F, 1.0F), // claw
                                PartPose.offset(-5.0F, 1.5F, 0.0F));
                root.addOrReplaceChild("left_arm", CubeListBuilder.create().mirror()
                                .texOffs(24, 35).addBox(-1.0F, -1.0F, -1.5F, 3.0F, 3.0F, 3.0F)
                                .texOffs(38, 35).addBox(-0.5F, 1.0F, -1.0F, 2.0F, 9.0F, 2.0F)
                                .texOffs(48, 35).addBox(0.5F, 10.0F, -1.0F, 1.0F, 4.0F, 1.0F)
                                .texOffs(48, 35).addBox(-0.5F, 10.0F, 0.0F, 1.0F, 4.0F, 1.0F),
                                PartPose.offset(5.0F, 1.5F, 0.0F));

                // LEGS: digitigrade - thigh, reverse shin, wide splayed foot.
                root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                                .texOffs(0, 43).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F)   // thigh
                                .texOffs(17, 43).addBox(-1.5F, 5.5F, -0.5F, 3.0F, 5.0F, 3.0F)  // shin
                                .texOffs(30, 50).addBox(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 4.0F),// foot
                                PartPose.offset(-2.0F, 12.0F, 0.0F));
                root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
                                .texOffs(0, 43).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F)
                                .texOffs(17, 43).addBox(-1.5F, 5.5F, -0.5F, 3.0F, 5.0F, 3.0F)
                                .texOffs(30, 50).addBox(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 4.0F),
                                PartPose.offset(2.0F, 12.0F, 0.0F));

                return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public void setupAnim(AlienGruntEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                        float netHeadYaw, float headPitch) {
                this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
                this.head.xRot = headPitch * ((float) Math.PI / 180F) + 0.25F; // hunched, peering up

                float twitch = Mth.sin(ageInTicks * 0.8F) * 0.1F;
                this.head.zRot = twitch * 0.5F;

                // Antennae wobble independently - reads as constant alertness.
                ModelPart antR = this.head.getChild("antenna_right");
                ModelPart antL = this.head.getChild("antenna_left");
                antR.zRot = -0.25F + Mth.sin(ageInTicks * 0.35F) * 0.18F;
                antL.zRot = 0.25F + Mth.sin(ageInTicks * 0.35F + 1.7F) * 0.18F;
                antR.xRot = Mth.cos(ageInTicks * 0.22F) * 0.12F;
                antL.xRot = Mth.cos(ageInTicks * 0.22F + 0.9F) * 0.12F;

                // Tail whips with movement, sways at rest.
                float tailSwing = Mth.sin(limbSwing * 0.6662F) * 0.6F * limbSwingAmount;
                this.tail.yRot = tailSwing + Mth.sin(ageInTicks * 0.12F) * 0.25F;
                this.tail.xRot = 0.35F + Mth.cos(ageInTicks * 0.17F) * 0.1F;
                this.tail.getChild("tail_tip").yRot = tailSwing * 1.4F + Mth.sin(ageInTicks * 0.12F + 0.8F) * 0.35F;

                // Digitigrade gait: quick, slightly skittering steps.
                this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
                this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

                // Loose asymmetric arm sway.
                this.rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.2F * limbSwingAmount * 0.5F;
                this.leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * limbSwingAmount * 0.5F;
                this.rightArm.zRot = 0.08F + twitch;
                this.leftArm.zRot = -0.08F - twitch;

                ModelPart jaw = this.head.getChild("lower_jaw");
                if (entity.isAggressive()) {
                        // Claws up, rapid swipes, jaw snapping.
                        float attackSwing = Mth.sin(ageInTicks * 0.8F);
                        this.rightArm.xRot = -1.5F + attackSwing * 0.5F;
                        this.rightArm.yRot = attackSwing * 0.3F;
                        this.leftArm.xRot = -1.3F - attackSwing * 0.5F;
                        this.leftArm.yRot = -attackSwing * 0.3F;
                        jaw.xRot = 0.25F + Math.abs(Mth.sin(ageInTicks * 0.5F)) * 0.55F;
                } else {
                        jaw.xRot = 0.25F + twitch;
                }
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                        int packedOverlay, int color) {
                head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                rightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                leftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
}
