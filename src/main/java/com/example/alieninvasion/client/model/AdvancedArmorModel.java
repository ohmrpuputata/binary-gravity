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

public class AdvancedArmorModel extends HumanoidModel<LivingEntity> {
    public enum Variant {
        COSMIC,
        HAZMAT,
        CHEM,
        PLATINUM
    }

    private final ModelPart helmetDetail;
    private final ModelPart chestDetail;
    private final ModelPart backpack;
    private final ModelPart rightShoulder;
    private final ModelPart leftShoulder;
    private final ModelPart rightKnee;
    private final ModelPart leftKnee;
    private final ModelPart rightBoot;
    private final ModelPart leftBoot;

    public AdvancedArmorModel(ModelPart root) {
        super(root);
        this.helmetDetail = this.head.getChild("detail");
        this.chestDetail = this.body.getChild("detail");
        this.backpack = this.body.getChild("backpack");
        this.rightShoulder = this.rightArm.getChild("shoulder");
        this.leftShoulder = this.leftArm.getChild("shoulder");
        this.rightKnee = this.rightLeg.getChild("knee");
        this.leftKnee = this.leftLeg.getChild("knee");
        this.rightBoot = this.rightLeg.getChild("boot");
        this.leftBoot = this.leftLeg.getChild("boot");
    }

    public static LayerDefinition createBodyLayer(Variant variant) {
        MeshDefinition mesh = HumanoidModel.createMesh(new CubeDeformation(0.68F), 0.0F);
        PartDefinition root = mesh.getRoot();

        root.getChild("head").addOrReplaceChild("detail", helmet(variant), PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("detail", chest(variant), PartPose.ZERO);
        root.getChild("body").addOrReplaceChild("backpack", backpack(variant), PartPose.ZERO);
        root.getChild("right_arm").addOrReplaceChild("shoulder",
                shoulder(variant, false), PartPose.ZERO);
        root.getChild("left_arm").addOrReplaceChild("shoulder",
                shoulder(variant, true), PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("knee",
                knee(variant, false), PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("knee",
                knee(variant, true), PartPose.ZERO);
        root.getChild("right_leg").addOrReplaceChild("boot",
                boot(variant, false), PartPose.ZERO);
        root.getChild("left_leg").addOrReplaceChild("boot",
                boot(variant, true), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static CubeListBuilder helmet(Variant variant) {
        CubeListBuilder builder = CubeListBuilder.create();
        return switch (variant) {
            case COSMIC -> builder.texOffs(0, 32)
                    .addBox(-4.0F, -6.5F, -5.0F, 8.0F, 2.0F, 1.0F)
                    .texOffs(18, 32).addBox(-1.0F, -10.5F, -1.0F, 2.0F, 4.0F, 2.0F)
                    .texOffs(26, 32).addBox(-5.0F, -7.0F, -2.0F, 1.0F, 3.0F, 4.0F)
                    .texOffs(26, 32).mirror().addBox(4.0F, -7.0F, -2.0F, 1.0F, 3.0F, 4.0F);
            case HAZMAT -> builder.texOffs(0, 32)
                    .addBox(-4.0F, -7.0F, -5.1F, 8.0F, 5.0F, 1.0F)
                    .texOffs(18, 32).addBox(-5.3F, -5.5F, -3.0F, 1.0F, 3.0F, 3.0F)
                    .texOffs(18, 32).mirror().addBox(4.3F, -5.5F, -3.0F, 1.0F, 3.0F, 3.0F);
            case CHEM -> builder.texOffs(0, 32)
                    .addBox(-4.0F, -6.5F, -5.0F, 8.0F, 2.0F, 1.0F)
                    .texOffs(18, 32).addBox(-2.5F, -4.5F, -5.4F, 5.0F, 3.0F, 1.0F)
                    .texOffs(30, 32).addBox(-5.2F, -6.0F, -2.0F, 1.0F, 4.0F, 4.0F);
            case PLATINUM -> builder.texOffs(0, 32)
                    .addBox(-4.0F, -6.8F, -5.0F, 8.0F, 2.0F, 1.0F)
                    .texOffs(18, 32).addBox(-3.0F, -9.5F, -1.0F, 6.0F, 2.0F, 2.0F)
                    .texOffs(34, 32).addBox(-0.75F, -11.0F, -0.75F, 1.5F, 3.0F, 1.5F);
        };
    }

    private static CubeListBuilder chest(Variant variant) {
        CubeListBuilder builder = CubeListBuilder.create();
        return switch (variant) {
            case COSMIC -> builder.texOffs(0, 42)
                    .addBox(-3.0F, 1.0F, -3.3F, 6.0F, 6.0F, 1.0F)
                    .texOffs(14, 42).addBox(-1.0F, 2.0F, -3.7F, 2.0F, 4.0F, 1.0F);
            case HAZMAT -> builder.texOffs(0, 42)
                    .addBox(-2.5F, 2.0F, -3.2F, 5.0F, 4.0F, 1.0F)
                    .texOffs(14, 42).addBox(-1.5F, 6.0F, -3.0F, 3.0F, 2.0F, 1.0F);
            case CHEM -> builder.texOffs(0, 42)
                    .addBox(-3.0F, 1.0F, -3.2F, 6.0F, 7.0F, 1.0F)
                    .texOffs(16, 42).addBox(1.0F, 2.0F, -3.7F, 2.0F, 5.0F, 1.0F);
            case PLATINUM -> builder.texOffs(0, 42)
                    .addBox(-3.5F, 0.5F, -3.3F, 7.0F, 7.0F, 1.0F)
                    .texOffs(16, 42).addBox(-2.0F, 2.0F, -3.7F, 4.0F, 3.0F, 1.0F);
        };
    }

    private static CubeListBuilder backpack(Variant variant) {
        CubeListBuilder builder = CubeListBuilder.create();
        return switch (variant) {
            case COSMIC -> builder.texOffs(32, 42)
                    .addBox(-2.0F, 1.0F, 2.5F, 4.0F, 8.0F, 1.0F);
            case HAZMAT -> builder.texOffs(32, 42)
                    .addBox(-3.0F, 1.0F, 2.5F, 6.0F, 8.0F, 2.0F);
            case CHEM -> builder.texOffs(32, 42)
                    .addBox(-3.5F, 0.5F, 2.5F, 7.0F, 9.0F, 2.0F);
            case PLATINUM -> builder.texOffs(32, 42)
                    .addBox(-2.5F, 2.0F, 2.5F, 5.0F, 6.0F, 1.0F);
        };
    }

    private static CubeListBuilder shoulder(Variant variant, boolean mirror) {
        CubeListBuilder builder = CubeListBuilder.create().texOffs(48, 32);
        if (mirror) builder.mirror();
        return switch (variant) {
            case COSMIC -> builder.addBox(mirror ? -0.5F : -5.5F, -3.0F, -3.0F, 6.0F, 3.0F, 6.0F);
            case HAZMAT -> builder.addBox(mirror ? -0.5F : -4.5F, -2.0F, -2.8F, 5.0F, 3.0F, 5.0F);
            case CHEM -> builder.addBox(mirror ? -0.5F : -5.0F, -2.5F, -3.0F, 5.5F, 4.0F, 6.0F);
            case PLATINUM -> builder.addBox(mirror ? -0.5F : -5.5F, -3.0F, -3.2F, 6.0F, 4.0F, 6.5F);
        };
    }

    private static CubeListBuilder knee(Variant variant, boolean mirror) {
        CubeListBuilder builder = CubeListBuilder.create().texOffs(0, 52);
        if (mirror) builder.mirror();
        float width = variant == Variant.CHEM || variant == Variant.PLATINUM ? 4.5F : 4.0F;
        return builder.addBox(-2.0F, 4.5F, -3.1F, width, 4.0F, 1.0F);
    }

    private static CubeListBuilder boot(Variant variant, boolean mirror) {
        CubeListBuilder builder = CubeListBuilder.create().texOffs(20, 52);
        if (mirror) builder.mirror();
        float front = variant == Variant.COSMIC ? -4.5F : -4.0F;
        float depth = variant == Variant.COSMIC ? 2.5F : 2.0F;
        return builder.addBox(-2.0F, 9.0F, front, 4.0F, 3.0F, depth);
    }

    public void setVisibleForSlot(EquipmentSlot slot) {
        setAllVisible(false);
        this.helmetDetail.visible = false;
        this.chestDetail.visible = false;
        this.backpack.visible = false;
        this.rightShoulder.visible = false;
        this.leftShoulder.visible = false;
        this.rightKnee.visible = false;
        this.leftKnee.visible = false;
        this.rightBoot.visible = false;
        this.leftBoot.visible = false;

        switch (slot) {
            case HEAD -> {
                this.head.visible = true;
                this.helmetDetail.visible = true;
            }
            case CHEST -> {
                this.body.visible = true;
                this.rightArm.visible = true;
                this.leftArm.visible = true;
                this.chestDetail.visible = true;
                this.backpack.visible = true;
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
