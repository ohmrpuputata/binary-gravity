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
 * One parametric "swarm humanoid" skeleton shared by every biped alien: dome
 * head with glossy eyes, snapping jaw, optional antennae/horns/crest, ribbed
 * torso with spine spikes and an optional whip tail, claw arms (one can be a
 * plasma cannon), digitigrade legs. Variants differ in proportions and
 * attachments; absolute size comes from the renderer's scale. All variants
 * share ONE 96x96 UV layout, mirrored in generate_swarm_textures.py.
 */
public class AlienHumanoidModel<T extends Mob> extends EntityModel<T> {

    public enum Variant {
        BRUTE, STALKER, CASTER, SHAMAN, TELEKINETIC, TROLL, TYRANT, SPITTER, BREACHER
    }

    private final Variant variant;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public AlienHumanoidModel(ModelPart root, Variant variant) {
        this.variant = variant;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.tail = this.body.hasChild("tail") ? this.body.getChild("tail") : null;
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer(Variant v) {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // --- proportions per variant ---
        float headW = switch (v) { case BRUTE, BREACHER -> 7; case TELEKINETIC -> 9; default -> 8; };
        float headH = headW - 1;
        float chestW = switch (v) { case BRUTE -> 12; case TYRANT, BREACHER -> 11; case TROLL -> 6;
                case SPITTER -> 9; case CASTER -> 8; case TELEKINETIC -> 6; default -> 7; };
        float chestH = switch (v) { case BRUTE, TYRANT, BREACHER -> 7; default -> 6; };
        float armW = switch (v) { case BRUTE, BREACHER -> 4; case TYRANT -> 3; default -> 2; };
        float armLen = switch (v) { case BRUTE -> 11; case TELEKINETIC, STALKER -> 12; default -> 9; };
        boolean antennae = v == Variant.STALKER || v == Variant.TELEKINETIC || v == Variant.SHAMAN
                || v == Variant.TROLL;
        boolean horns = v == Variant.TYRANT;
        boolean crest = v == Variant.SHAMAN;
        boolean tail = v == Variant.STALKER || v == Variant.TYRANT || v == Variant.SPITTER
                || v == Variant.TROLL;
        boolean cannon = v == Variant.CASTER;
        boolean drills = v == Variant.BREACHER; // wedge-drills on BOTH arms

        float hw = headW / 2.0F;
        // HEAD: dome + eyes + jaw (+ antennae / horns / crest)
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-hw, -1.0F - headH, -hw, headW, headH, headW),
                PartPose.offset(0.0F, 0.0F, -1.5F));
        head.addOrReplaceChild("lower_jaw", CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-2.5F, 0.0F, -4.5F, 5.0F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, 0.5F, 0.25F, 0.0F, 0.0F));
        head.addOrReplaceChild("eye_right", CubeListBuilder.create()
                        .texOffs(41, 0).addBox(-hw + 0.4F, -headH + 0.8F, -hw - 0.6F, 3.0F, 2.0F, 1.0F),
                PartPose.rotation(0.0F, 0.0F, 0.12F));
        head.addOrReplaceChild("eye_left", CubeListBuilder.create()
                        .texOffs(41, 0).addBox(hw - 3.4F, -headH + 0.8F, -hw - 0.6F, 3.0F, 2.0F, 1.0F),
                PartPose.rotation(0.0F, 0.0F, -0.12F));
        if (antennae) {
            head.addOrReplaceChild("antenna_right", CubeListBuilder.create()
                            .texOffs(41, 4).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F)
                            .texOffs(47, 4).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(-hw + 1.5F, -headH - 1.0F, 0.0F, 0.0F, 0.0F, -0.25F));
            head.addOrReplaceChild("antenna_left", CubeListBuilder.create()
                            .texOffs(41, 4).addBox(-0.5F, -5.0F, -0.5F, 1.0F, 5.0F, 1.0F)
                            .texOffs(47, 4).addBox(-0.5F, -6.0F, -0.5F, 1.0F, 1.0F, 1.0F),
                    PartPose.offsetAndRotation(hw - 1.5F, -headH - 1.0F, 0.0F, 0.0F, 0.0F, 0.25F));
        }
        if (horns) {
            head.addOrReplaceChild("horn_right", CubeListBuilder.create()
                            .texOffs(52, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(-hw + 1.0F, -headH, 0.0F, 0.0F, 0.0F, -0.45F));
            head.addOrReplaceChild("horn_left", CubeListBuilder.create()
                            .texOffs(52, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
                    PartPose.offsetAndRotation(hw - 1.0F, -headH, 0.0F, 0.0F, 0.0F, 0.45F));
        }
        if (crest) {
            head.addOrReplaceChild("crest", CubeListBuilder.create()
                            .texOffs(52, 8).addBox(-0.5F, -5.0F, -2.5F, 1.0F, 5.0F, 5.0F),
                    PartPose.offsetAndRotation(0.0F, -headH - 0.5F, 1.0F, -0.2F, 0.0F, 0.0F));
        }

        float cw = chestW / 2.0F;
        // BODY: chest + waist + pelvis (+ spine spikes, + tail)
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 30).addBox(-cw, 0.0F, -2.5F, chestW, chestH, 5.0F)
                        .texOffs(37, 30).addBox(-cw + 1.0F, chestH, -2.0F, chestW - 2.0F, 4.0F, 4.0F)
                        .texOffs(0, 45).addBox(-cw + 0.5F, chestH + 4.0F, -2.5F, chestW - 1.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        body.addOrReplaceChild("spike1", CubeListBuilder.create()
                .texOffs(62, 0).addBox(-0.5F, 0.5F, 2.5F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
        body.addOrReplaceChild("spike2", CubeListBuilder.create()
                .texOffs(62, 0).addBox(-0.5F, 3.5F, 2.1F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
        body.addOrReplaceChild("spike3", CubeListBuilder.create()
                .texOffs(62, 0).addBox(-0.5F, 6.5F, 1.7F, 1.0F, 2.0F, 1.0F), PartPose.ZERO);
        if (tail) {
            PartDefinition tailPart = body.addOrReplaceChild("tail", CubeListBuilder.create()
                            .texOffs(37, 40).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 7.0F),
                    PartPose.offset(0.0F, chestH + 5.5F, 2.0F));
            tailPart.addOrReplaceChild("tail_tip", CubeListBuilder.create()
                            .texOffs(37, 50).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 6.0F),
                    PartPose.offset(0.0F, 0.0F, 7.0F));
        }

        // ARMS: claws or cannon
        float armX = cw + armW / 2.0F + 0.5F;
        CubeListBuilder rightArmCubes = CubeListBuilder.create()
                .texOffs(0, 55).addBox(-armW / 2.0F - 1.0F, -1.0F, -2.0F, armW, 4.0F, 4.0F)
                .texOffs(17, 55).addBox(-armW / 2.0F - 0.5F, 2.0F, -armW / 2.0F, armW, armLen, armW);
        if (cannon || drills) {
            rightArmCubes.texOffs(30, 55).addBox(-armW / 2.0F - 2.0F, armLen - 3.0F, -2.5F, 5.0F, 6.0F, 5.0F);
        } else {
            rightArmCubes.texOffs(51, 55).addBox(-armW / 2.0F - 0.5F, armLen + 1.0F, -1.0F, 1.0F, 4.0F, 1.0F)
                    .texOffs(51, 55).addBox(armW / 2.0F - 1.5F, armLen + 1.0F, 0.0F, 1.0F, 4.0F, 1.0F);
        }
        root.addOrReplaceChild("right_arm", rightArmCubes, PartPose.offset(-armX, 1.5F, 0.0F));
        CubeListBuilder leftArmCubes = CubeListBuilder.create().mirror()
                .texOffs(0, 55).addBox(-armW / 2.0F + 1.0F, -1.0F, -2.0F, armW, 4.0F, 4.0F)
                .texOffs(17, 55).addBox(-armW / 2.0F + 0.5F, 2.0F, -armW / 2.0F, armW, armLen, armW);
        if (drills) {
            leftArmCubes.texOffs(30, 55).addBox(-armW / 2.0F - 1.0F, armLen - 3.0F, -2.5F, 5.0F, 6.0F, 5.0F);
        } else {
            leftArmCubes.texOffs(51, 55).addBox(-armW / 2.0F + 0.5F, armLen + 1.0F, -1.0F, 1.0F, 4.0F, 1.0F)
                    .texOffs(51, 55).addBox(armW / 2.0F - 0.5F, armLen + 1.0F, 0.0F, 1.0F, 4.0F, 1.0F);
        }
        root.addOrReplaceChild("left_arm", leftArmCubes, PartPose.offset(armX, 1.5F, 0.0F));

        // LEGS: digitigrade
        float legX = Math.max(2.0F, cw - 1.5F);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 70).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F)
                        .texOffs(21, 70).addBox(-1.5F, 5.5F, -0.5F, 3.0F, 5.0F, 3.0F)
                        .texOffs(38, 70).addBox(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(-legX, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
                        .texOffs(0, 70).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F)
                        .texOffs(21, 70).addBox(-1.5F, 5.5F, -0.5F, 3.0F, 5.0F, 3.0F)
                        .texOffs(38, 70).addBox(-2.0F, 10.0F, -3.0F, 4.0F, 2.0F, 4.0F),
                PartPose.offset(legX, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 96, 96);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F)
                + (variant == Variant.TROLL || variant == Variant.SPITTER ? 0.3F : 0.15F);

        float twitch = Mth.sin(ageInTicks * 0.7F) * 0.08F;
        this.head.zRot = twitch * 0.5F;

        if (this.head.hasChild("antenna_right")) {
            this.head.getChild("antenna_right").zRot = -0.25F + Mth.sin(ageInTicks * 0.35F) * 0.18F;
            this.head.getChild("antenna_left").zRot = 0.25F + Mth.sin(ageInTicks * 0.35F + 1.7F) * 0.18F;
        }

        if (this.tail != null) {
            float tailSwing = Mth.sin(limbSwing * 0.6662F) * 0.6F * limbSwingAmount;
            this.tail.yRot = tailSwing + Mth.sin(ageInTicks * 0.12F) * 0.25F;
            this.tail.xRot = 0.3F + Mth.cos(ageInTicks * 0.17F) * 0.1F;
            this.tail.getChild("tail_tip").yRot = tailSwing * 1.4F + Mth.sin(ageInTicks * 0.12F + 0.8F) * 0.35F;
        }

        float gait = variant == Variant.BRUTE || variant == Variant.TYRANT ? 0.45F : 0.6662F;
        this.rightLeg.xRot = Mth.cos(limbSwing * gait) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * gait + (float) Math.PI) * 1.4F * limbSwingAmount;

        this.rightArm.xRot = Mth.cos(limbSwing * gait + (float) Math.PI) * 1.2F * limbSwingAmount * 0.5F;
        this.leftArm.xRot = Mth.cos(limbSwing * gait) * 1.2F * limbSwingAmount * 0.5F;
        this.rightArm.zRot = 0.06F + twitch;
        this.leftArm.zRot = -0.06F - twitch;

        ModelPart jaw = this.head.getChild("lower_jaw");
        if (entity.isAggressive()) {
            float attackSwing = Mth.sin(ageInTicks * 0.8F);
            if (variant == Variant.CASTER || variant == Variant.SPITTER) {
                // Ranged stance: cannon arm raised and levelled at the target.
                this.rightArm.xRot = -1.45F + twitch;
                this.rightArm.yRot = 0.0F;
            } else {
                this.rightArm.xRot = -1.5F + attackSwing * 0.5F;
                this.rightArm.yRot = attackSwing * 0.3F;
                this.leftArm.xRot = -1.3F - attackSwing * 0.5F;
                this.leftArm.yRot = -attackSwing * 0.3F;
            }
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
