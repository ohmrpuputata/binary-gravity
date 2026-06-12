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
 * The swarm's mockery of a chicken: a plump alien bird with a bulbous dome head,
 * two big glossy eyes, twitching antennae instead of a comb, membrane wings that
 * never stop fluttering, and a stub tail. UV (64x32) is mirrored in
 * generate_breacher_chicken_textures.py.
 */
public class AlienChickenModel<T extends Mob> extends EntityModel<T> {
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightWing;
    private final ModelPart leftWing;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public AlienChickenModel(ModelPart root) {
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightWing = root.getChild("right_wing");
        this.leftWing = root.getChild("left_wing");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -6.0F, -2.5F, 5.0F, 5.0F, 5.0F)   // dome
                        .texOffs(21, 0).addBox(-1.5F, -3.0F, -4.5F, 3.0F, 2.0F, 2.0F), // beak
                PartPose.offset(0.0F, 15.0F, -4.0F));
        head.addOrReplaceChild("antenna_r", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(-1.2F, -6.0F, 0.0F, 0.0F, 0.0F, -0.3F));
        head.addOrReplaceChild("antenna_l", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offsetAndRotation(1.2F, -6.0F, 0.0F, 0.0F, 0.0F, 0.3F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 11).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 8.0F),
                PartPose.offset(0.0F, 17.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create()
                        .texOffs(29, 11).addBox(-1.5F, -2.0F, 0.0F, 3.0F, 3.0F, 2.0F),
                PartPose.offsetAndRotation(0.0F, -1.0F, 4.0F, 0.5F, 0.0F, 0.0F));

        root.addOrReplaceChild("right_wing", CubeListBuilder.create()
                        .texOffs(0, 26).addBox(-1.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F),
                PartPose.offset(-3.0F, 14.0F, 0.0F));
        root.addOrReplaceChild("left_wing", CubeListBuilder.create().mirror()
                        .texOffs(0, 26).addBox(0.0F, 0.0F, -3.0F, 1.0F, 4.0F, 6.0F),
                PartPose.offset(3.0F, 14.0F, 0.0F));

        root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(15, 26).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 4.0F, 3.0F),
                PartPose.offset(-1.5F, 20.0F, 1.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().mirror()
                        .texOffs(15, 26).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 4.0F, 3.0F),
                PartPose.offset(1.5F, 20.0F, 1.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float) Math.PI) * 1.4F * limbSwingAmount;

        // Membrane wings: nervous constant flutter, frantic while moving.
        float flap = ageInTicks * (0.6F + limbSwingAmount * 1.4F);
        this.rightWing.zRot = 0.2F + Mth.cos(flap) * 0.6F * (0.3F + limbSwingAmount);
        this.leftWing.zRot = -0.2F - Mth.cos(flap) * 0.6F * (0.3F + limbSwingAmount);

        this.head.getChild("antenna_r").zRot = -0.3F + Mth.sin(ageInTicks * 0.3F) * 0.2F;
        this.head.getChild("antenna_l").zRot = 0.3F + Mth.sin(ageInTicks * 0.3F + 1.3F) * 0.2F;
        this.body.getChild("tail").xRot = 0.5F + Mth.sin(ageInTicks * 0.2F) * 0.15F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        head.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        rightWing.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leftWing.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
