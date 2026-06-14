package com.example.alieninvasion.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class PalladiumArmorModel extends HumanoidModel<LivingEntity> {
    private final ModelPart visor;
    private final ModelPart reactor;
    private final ModelPart rightShoulder;
    private final ModelPart leftShoulder;
    private final ModelPart rightKnee;
    private final ModelPart leftKnee;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public PalladiumArmorModel(ModelPart root) {
        super(root);
        this.visor = this.head.getChild("visor");
        this.reactor = this.body.getChild("reactor");
        this.rightShoulder = this.rightArm.getChild("shoulder");
        this.leftShoulder = this.leftArm.getChild("shoulder");
        this.rightKnee = this.rightLeg.getChild("knee");
        this.leftKnee = this.leftLeg.getChild("knee");
        this.rightBoot = this.rightLeg.getChild("boot");
        this.leftBoot = this.leftLeg.getChild("boot");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.72F), 0.0F);
        PartDefinition root = mesh.getRoot();

        root.getChild("head").addOrReplaceChild("visor",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-4.0F, -6.5F, -5.0F, 8.0F, 2.0F, 1.0F, new CubeDeformation(0.05F))
                        .texOffs(18, 32)
                        .addBox(-5.0F, -7.0F, -3.0F, 1.0F, 5.0F, 6.0F)
                        .texOffs(18, 32)
                        .mirror()
                        .addBox(4.0F, -7.0F, -3.0F, 1.0F, 5.0F, 6.0F),
                PartPose.ZERO);

        root.getChild("body").addOrReplaceChild("reactor",
                CubeListBuilder.create()
                        .texOffs(32, 32)
                        .addBox(-2.5F, 1.5F, -3.2F, 5.0F, 5.0F, 1.0F)
                        .texOffs(32, 38)
                        .addBox(-1.0F, 2.5F, -3.5F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        root.getChild("right_arm").addOrReplaceChild("shoulder",
                CubeListBuilder.create().texOffs(40, 32)
                        .addBox(-4.5F, -2.5F, -3.0F, 5.0F, 4.0F, 6.0F),
                PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("shoulder",
                CubeListBuilder.create().texOffs(40, 32).mirror()
                        .addBox(-0.5F, -2.5F, -3.0F, 5.0F, 4.0F, 6.0F),
                PartPose.ZERO);

        root.getChild("right_leg").addOrReplaceChild("knee",
                CubeListBuilder.create().texOffs(0, 43)
                        .addBox(-2.0F, 4.5F, -3.0F, 4.0F, 4.0F, 1.0F),
                PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("knee",
                CubeListBuilder.create().texOffs(0, 43).mirror()
                        .addBox(-2.0F, 4.5F, -3.0F, 4.0F, 4.0F, 1.0F),
                PartPose.ZERO);

        root.getChild("right_leg").addOrReplaceChild("boot",
                CubeListBuilder.create().texOffs(12, 43)
                        .addBox(-2.0F, 9.0F, -4.0F, 4.0F, 3.0F, 2.0F),
                PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("boot",
                CubeListBuilder.create().texOffs(12, 43).mirror()
                        .addBox(-2.0F, 9.0F, -4.0F, 4.0F, 3.0F, 2.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setVisibleForSlot(EquipmentSlot slot) {
        setAllVisible(false);
        this.visor.visible = false;
        this.reactor.visible = false;
        this.rightShoulder.visible = false;
        this.leftShoulder.visible = false;
        this.rightKnee.visible = false;
        this.leftKnee.visible = false;
        this.rightBoot.visible = false;
        this.leftBoot.visible = false;

        switch (slot) {
            case HEAD -> {
                this.head.visible = true;
                this.visor.visible = true;
            }
            case CHEST -> {
                this.body.visible = true;
                this.rightArm.visible = true;
                this.leftArm.visible = true;
                this.reactor.visible = true;
                this.rightShoulder.visible = true;
                this.leftShoulder.visible = true;
            }
            case LEGS -> {
                this.body.visible = true;
                this.rightLeg.visible = true;
                this.leftLeg.visible = true;
                this.rightKnee.visible = true;
                this.leftKnee.visible = true;
            }
            case FEET -> {
                this.rightLeg.visible = true;
                this.leftLeg.visible = true;
                this.rightBoot.visible = true;
                this.leftBoot.visible = true;
            }
            default -> {
            }
        }
    }
}
