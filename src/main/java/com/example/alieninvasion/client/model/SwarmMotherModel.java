package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.SwarmMotherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * Custom model for the final boss, the Swarm Mother — a colossal insectoid
 * brood-queen: a broad spiked skull with four eyes, snapping mandibles and a
 * gaping maw; a heavy carapaced thorax bristling with back spines; two huge
 * clawed forelimbs; a two-segment bloated egg-sac abdomen slung behind; and six
 * legs (two thick anchor legs plus a splayed insectoid pair). Painted onto a
 * 256x256 organic-chitin sheet — the whole sheet reads as carapace, so UVs only
 * need to stay roughly in-bounds.
 */
public class SwarmMotherModel extends EntityModel<SwarmMotherEntity> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart abdomen;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart midLegR;
    private final ModelPart midLegL;

    public SwarmMotherModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.abdomen = this.body.getChild("abdomen");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.midLegR = root.getChild("mid_leg_r");
        this.midLegL = root.getChild("mid_leg_l");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // HEAD — broad chitin skull that juts forward
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6.0F, -11.0F, -7.0F, 12.0F, 11.0F, 12.0F),
                PartPose.offset(0.0F, -2.0F, -3.0F));
        // crown of five spikes
        head.addOrReplaceChild("crown_mid", CubeListBuilder.create()
                .texOffs(52, 0).addBox(-1.5F, -9.0F, -1.5F, 3.0F, 10.0F, 3.0F),
                PartPose.offset(0.0F, -11.0F, -1.0F));
        head.addOrReplaceChild("crown_l1", CubeListBuilder.create()
                .texOffs(64, 0).addBox(0.0F, -8.0F, -1.5F, 2.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(2.0F, -11.0F, -1.0F, 0F, 0F, -0.35F));
        head.addOrReplaceChild("crown_r1", CubeListBuilder.create()
                .texOffs(74, 0).addBox(-2.0F, -8.0F, -1.5F, 2.0F, 7.0F, 3.0F),
                PartPose.offsetAndRotation(-2.0F, -11.0F, -1.0F, 0F, 0F, 0.35F));
        head.addOrReplaceChild("crown_l2", CubeListBuilder.create()
                .texOffs(84, 0).addBox(0.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(4.0F, -10.0F, -1.0F, 0F, 0F, -0.7F));
        head.addOrReplaceChild("crown_r2", CubeListBuilder.create()
                .texOffs(92, 0).addBox(-2.0F, -6.0F, -1.0F, 2.0F, 6.0F, 2.0F),
                PartPose.offsetAndRotation(-4.0F, -10.0F, -1.0F, 0F, 0F, 0.7F));
        // mandibles + lower maw
        head.addOrReplaceChild("mandible_left", CubeListBuilder.create()
                .texOffs(0, 40).addBox(0.0F, -1.5F, -6.0F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(2.5F, -2.0F, -7.0F, 0F, 0F, 0.25F));
        head.addOrReplaceChild("mandible_right", CubeListBuilder.create()
                .texOffs(20, 40).addBox(-3.0F, -1.5F, -6.0F, 3.0F, 3.0F, 6.0F),
                PartPose.offsetAndRotation(-2.5F, -2.0F, -7.0F, 0F, 0F, -0.25F));
        head.addOrReplaceChild("maw", CubeListBuilder.create()
                .texOffs(40, 40).addBox(-3.5F, -1.0F, -5.0F, 7.0F, 3.0F, 5.0F),
                PartPose.offset(0.0F, -1.0F, -6.0F));
        // four glowing eyes (texture paints these regions bright)
        head.addOrReplaceChild("eye_l1", CubeListBuilder.create()
                .texOffs(100, 0).addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(2.0F, -8.0F, -7.4F));
        head.addOrReplaceChild("eye_r1", CubeListBuilder.create()
                .texOffs(110, 0).addBox(-2.0F, 0.0F, 0.0F, 2.0F, 2.0F, 1.0F),
                PartPose.offset(-2.0F, -8.0F, -7.4F));
        head.addOrReplaceChild("eye_l2", CubeListBuilder.create()
                .texOffs(100, 6).addBox(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(3.5F, -5.0F, -7.4F));
        head.addOrReplaceChild("eye_r2", CubeListBuilder.create()
                .texOffs(106, 6).addBox(-1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F),
                PartPose.offset(-3.5F, -5.0F, -7.4F));

        // BODY — heavy carapaced thorax
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 64).addBox(-9.0F, -2.0F, -7.0F, 18.0F, 16.0F, 14.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // back carapace spines
        body.addOrReplaceChild("back_spike_mid", CubeListBuilder.create()
                .texOffs(70, 40).addBox(-1.0F, -6.0F, -2.0F, 2.0F, 6.0F, 4.0F),
                PartPose.offsetAndRotation(0.0F, -2.0F, -2.0F, -0.25F, 0F, 0F));
        body.addOrReplaceChild("back_spike_l", CubeListBuilder.create()
                .texOffs(80, 40).addBox(-1.0F, -5.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(5.0F, -1.0F, 0.0F, -0.25F, 0F, -0.35F));
        body.addOrReplaceChild("back_spike_r", CubeListBuilder.create()
                .texOffs(90, 40).addBox(-1.0F, -5.0F, -1.5F, 2.0F, 5.0F, 3.0F),
                PartPose.offsetAndRotation(-5.0F, -1.0F, 0.0F, -0.25F, 0F, 0.35F));

        // ABDOMEN — giant egg sac slung behind, tilted up, with a tapering tip
        PartDefinition abdomen = body.addOrReplaceChild("abdomen", CubeListBuilder.create()
                .texOffs(0, 110).addBox(-9.0F, -2.0F, 0.0F, 18.0F, 18.0F, 18.0F),
                PartPose.offsetAndRotation(0.0F, 7.0F, 5.0F, 0.5F, 0.0F, 0.0F));
        abdomen.addOrReplaceChild("abdomen_tip", CubeListBuilder.create()
                .texOffs(80, 110).addBox(-6.0F, -5.0F, 0.0F, 12.0F, 12.0F, 13.0F),
                PartPose.offset(0.0F, 3.0F, 17.0F));
        abdomen.addOrReplaceChild("abd_spike_l", CubeListBuilder.create()
                .texOffs(150, 110).addBox(-1.0F, -3.0F, -1.5F, 2.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(6.0F, 0.0F, 6.0F, 0F, 0F, -0.4F));
        abdomen.addOrReplaceChild("abd_spike_r", CubeListBuilder.create()
                .texOffs(162, 110).addBox(-1.0F, -3.0F, -1.5F, 2.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-6.0F, 0.0F, 6.0F, 0F, 0F, 0.4F));

        // ARMS — huge clawed forelimbs
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(130, 60).addBox(-6.0F, -2.0F, -4.0F, 6.0F, 20.0F, 8.0F),
                PartPose.offset(-9.0F, -1.0F, 0.0F));
        rightArm.addOrReplaceChild("claw_r1", CubeListBuilder.create()
                .texOffs(180, 60).addBox(-5.5F, 18.0F, -3.0F, 1.5F, 6.0F, 1.5F), PartPose.ZERO);
        rightArm.addOrReplaceChild("claw_r2", CubeListBuilder.create()
                .texOffs(188, 60).addBox(-3.0F, 18.0F, -3.0F, 1.5F, 5.0F, 1.5F), PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(130, 90).addBox(0.0F, -2.0F, -4.0F, 6.0F, 20.0F, 8.0F),
                PartPose.offset(9.0F, -1.0F, 0.0F));
        leftArm.addOrReplaceChild("claw_l1", CubeListBuilder.create()
                .texOffs(180, 90).addBox(4.0F, 18.0F, -3.0F, 1.5F, 6.0F, 1.5F), PartPose.ZERO);
        leftArm.addOrReplaceChild("claw_l2", CubeListBuilder.create()
                .texOffs(188, 90).addBox(1.5F, 18.0F, -3.0F, 1.5F, 5.0F, 1.5F), PartPose.ZERO);

        // LEGS — two thick anchor legs (feet ~y=28, proven ground line)
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(200, 0).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 16.0F, 8.0F),
                PartPose.offset(-5.0F, 12.0F, 1.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(200, 36).addBox(-4.0F, 0.0F, -4.0F, 8.0F, 16.0F, 8.0F),
                PartPose.offset(5.0F, 12.0F, 1.0F));
        // splayed insectoid mid-legs
        root.addOrReplaceChild("mid_leg_r", CubeListBuilder.create()
                .texOffs(220, 80).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 28.0F, 6.0F),
                PartPose.offsetAndRotation(-9.0F, 4.0F, -2.0F, 0.1F, 0F, 0.42F));
        root.addOrReplaceChild("mid_leg_l", CubeListBuilder.create()
                .texOffs(220, 80).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 28.0F, 6.0F),
                PartPose.offsetAndRotation(9.0F, 4.0F, -2.0F, 0.1F, 0F, -0.42F));

        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(SwarmMotherEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // heavy lumbering gait — anchor legs and mid-legs stride in counterphase
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.5F) * 0.9F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.9F * limbSwingAmount;
        this.midLegR.xRot = 0.1F + Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.6F * limbSwingAmount;
        this.midLegL.xRot = 0.1F + Mth.cos(limbSwing * 0.5F) * 0.6F * limbSwingAmount;
        // mid-legs also twitch at idle so she never looks frozen
        float skitter = Mth.sin(ageInTicks * 0.18F) * 0.05F;
        this.midLegR.zRot = 0.42F + skitter;
        this.midLegL.zRot = -0.42F - skitter;

        // swaying clawed arms, reared up when aggressive
        float idle = Mth.cos(ageInTicks * 0.08F) * 0.1F;
        this.rightArm.xRot = Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.6F * limbSwingAmount - 0.2F + idle;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.5F) * 0.6F * limbSwingAmount - 0.2F - idle;
        this.rightArm.zRot = 0.18F;
        this.leftArm.zRot = -0.18F;
        if (entity.isAggressive()) {
            this.rightArm.xRot = -1.7F + Mth.sin(ageInTicks * 0.6F) * 0.4F;
            this.leftArm.xRot = -1.7F - Mth.sin(ageInTicks * 0.6F) * 0.4F;
        }

        // the egg sac breathes / pulses
        this.abdomen.xRot = 0.5F + Mth.sin(ageInTicks * 0.12F) * 0.07F;

        // mandibles snap
        ModelPart ml = this.head.getChild("mandible_left");
        ModelPart mr = this.head.getChild("mandible_right");
        float snap = Math.abs(Mth.sin(ageInTicks * 0.3F)) * 0.3F;
        ml.zRot = 0.25F + snap;
        mr.zRot = -0.25F - snap;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.body.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.rightArm.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.leftArm.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.midLegR.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.midLegL.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
