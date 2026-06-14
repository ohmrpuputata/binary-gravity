package com.example.alieninvasion.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

public class BioFilterMaskModel extends HumanoidModel<LivingEntity> {
    public BioFilterMaskModel(ModelPart root) {
        super(root);
        setAllVisible(false);
        this.head.visible = true;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));

        head.addOrReplaceChild("face_mask",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.5F, -5.0F, -5.15F, 7.0F, 4.0F, 1.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("respirator",
                CubeListBuilder.create().texOffs(0, 10)
                        .addBox(-1.5F, -3.8F, -6.0F, 3.0F, 2.8F, 1.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("left_filter",
                CubeListBuilder.create().texOffs(12, 10)
                        .addBox(3.0F, -3.9F, -5.25F, 2.0F, 2.5F, 2.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("right_filter",
                CubeListBuilder.create().texOffs(20, 10).mirror()
                        .addBox(-5.0F, -3.9F, -5.25F, 2.0F, 2.5F, 2.0F),
                PartPose.ZERO);
        head.addOrReplaceChild("upper_strap",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-4.25F, -6.1F, -4.15F, 8.5F, 1.0F, 0.8F),
                PartPose.ZERO);
        head.addOrReplaceChild("side_strap",
                CubeListBuilder.create().texOffs(20, 17)
                        .addBox(-4.35F, -5.2F, -3.5F, 0.7F, 3.5F, 7.0F)
                        .mirror().addBox(3.65F, -5.2F, -3.5F, 0.7F, 3.5F, 7.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }
}
