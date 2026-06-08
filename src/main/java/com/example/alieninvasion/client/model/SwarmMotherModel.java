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
 * Custom model for the final boss, the Swarm Mother: an insectoid brood-queen
 * with a spiked crown, snapping mandibles, glowing eyes, heavy clawed arms and
 * a massive bloated egg-sac abdomen. Painted by generate_boss_armor_art.py onto
 * a 128x128 organic-chitin sheet (the whole sheet is filled, so UVs only need to
 * stay in-bounds).
 */
public class SwarmMotherModel extends EntityModel<SwarmMotherEntity> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart abdomen;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public SwarmMotherModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.abdomen = this.body.getChild("abdomen");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // HEAD - broad chitin skull
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -9.0F, -5.0F, 10.0F, 9.0F, 10.0F),
                PartPose.offset(0.0F, -2.0F, 0.0F));
        // crown of spikes
        head.addOrReplaceChild("crown_mid", CubeListBuilder.create()
                .texOffs(0, 22).addBox(-1.0F, -15.0F, -1.0F, 2.0F, 6.0F, 2.0F), PartPose.ZERO);
        head.addOrReplaceChild("crown_left", CubeListBuilder.create()
                .texOffs(12, 22).addBox(2.0F, -14.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.rotation(0F, 0F, -0.4F));
        head.addOrReplaceChild("crown_right", CubeListBuilder.create()
                .texOffs(24, 22).addBox(-4.0F, -14.0F, -1.0F, 2.0F, 5.0F, 2.0F), PartPose.rotation(0F, 0F, 0.4F));
        // mandibles
        head.addOrReplaceChild("mandible_left", CubeListBuilder.create()
                .texOffs(0, 32).addBox(2.0F, -3.0F, -7.0F, 2.0F, 2.0F, 5.0F), PartPose.rotation(0F, 0F, 0.2F));
        head.addOrReplaceChild("mandible_right", CubeListBuilder.create()
                .texOffs(16, 32).addBox(-4.0F, -3.0F, -7.0F, 2.0F, 2.0F, 5.0F), PartPose.rotation(0F, 0F, -0.2F));
        // glowing eyes (texture paints this region bright)
        head.addOrReplaceChild("eye_left", CubeListBuilder.create()
                .texOffs(100, 0).addBox(1.5F, -6.0F, -5.6F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);
        head.addOrReplaceChild("eye_right", CubeListBuilder.create()
                .texOffs(110, 0).addBox(-3.5F, -6.0F, -5.6F, 2.0F, 2.0F, 1.0F), PartPose.ZERO);

        // BODY - heavy thorax
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(44, 0).addBox(-7.0F, -2.0F, -5.0F, 14.0F, 14.0F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 0.0F));

        // ABDOMEN - giant egg sac slung behind, tilted up
        body.addOrReplaceChild("abdomen", CubeListBuilder.create()
                .texOffs(0, 46).addBox(-8.0F, -2.0F, 0.0F, 16.0F, 16.0F, 20.0F),
                PartPose.offsetAndRotation(0.0F, 6.0F, 4.0F, 0.55F, 0.0F, 0.0F));

        // ARMS - long clawed limbs
        PartDefinition rightArm = root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                .texOffs(76, 46).addBox(-4.0F, -2.0F, -3.0F, 4.0F, 16.0F, 6.0F),
                PartPose.offset(-7.0F, 0.0F, 0.0F));
        rightArm.addOrReplaceChild("claw_r", CubeListBuilder.create()
                .texOffs(100, 46).addBox(-3.5F, 14.0F, -2.0F, 1.0F, 4.0F, 1.0F), PartPose.ZERO);
        PartDefinition leftArm = root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                .texOffs(76, 70).addBox(0.0F, -2.0F, -3.0F, 4.0F, 16.0F, 6.0F),
                PartPose.offset(7.0F, 0.0F, 0.0F));
        leftArm.addOrReplaceChild("claw_l", CubeListBuilder.create()
                .texOffs(100, 52).addBox(2.5F, 14.0F, -2.0F, 1.0F, 4.0F, 1.0F), PartPose.ZERO);

        // LEGS - thick stance
        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                .texOffs(0, 86).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 16.0F, 6.0F),
                PartPose.offset(-3.5F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                .texOffs(28, 86).addBox(-3.0F, 0.0F, -3.0F, 6.0F, 16.0F, 6.0F),
                PartPose.offset(3.5F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(SwarmMotherEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // heavy lumbering gait
        this.rightLeg.xRot = Mth.cos(limbSwing * 0.5F) * 1.0F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 1.0F * limbSwingAmount;

        // swaying clawed arms, raised when aggressive
        float idle = Mth.cos(ageInTicks * 0.08F) * 0.1F;
        this.rightArm.xRot = Mth.cos(limbSwing * 0.5F + (float) Math.PI) * 0.8F * limbSwingAmount - 0.2F + idle;
        this.leftArm.xRot = Mth.cos(limbSwing * 0.5F) * 0.8F * limbSwingAmount - 0.2F - idle;
        this.rightArm.zRot = 0.15F;
        this.leftArm.zRot = -0.15F;
        if (entity.isAggressive()) {
            this.rightArm.xRot = -1.8F + Mth.sin(ageInTicks * 0.6F) * 0.4F;
            this.leftArm.xRot = -1.8F - Mth.sin(ageInTicks * 0.6F) * 0.4F;
        }

        // the egg sac breathes / pulses
        this.abdomen.xRot = 0.55F + Mth.sin(ageInTicks * 0.12F) * 0.06F;

        // mandibles snap
        ModelPart ml = this.head.getChild("mandible_left");
        ModelPart mr = this.head.getChild("mandible_right");
        float snap = Math.abs(Mth.sin(ageInTicks * 0.3F)) * 0.25F;
        ml.zRot = 0.2F + snap;
        mr.zRot = -0.2F - snap;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.head.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.body.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.rightArm.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.leftArm.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.rightLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
        this.leftLeg.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
