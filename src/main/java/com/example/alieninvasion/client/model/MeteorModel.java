package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.MeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class MeteorModel extends EntityModel<MeteorEntity> {
    private final ModelPart body;

    public MeteorModel(ModelPart root) {
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F)     // Core
                .texOffs(0, 32).addBox(-4.0F, -11.0F, -3.0F, 8.0F, 3.0F, 6.0F)      // Top crag
                .texOffs(28, 32).addBox(-3.0F, 8.0F, -4.0F, 6.0F, 3.0F, 8.0F)       // Bottom crag
                .texOffs(56, 0).addBox(-11.0F, -3.0F, -4.0F, 3.0F, 6.0F, 8.0F)      // Left crag
                .texOffs(56, 14).addBox(8.0F, -4.0F, -3.0F, 4.0F, 8.0F, 6.0F)       // Right crag
                .texOffs(80, 0).addBox(-4.0F, -4.0F, 8.0F, 8.0F, 8.0F, 3.0F)        // Back crag
                .texOffs(80, 14).addBox(-3.0F, -5.0F, -11.0F, 6.0F, 10.0F, 3.0F),   // Front crag
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(MeteorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
            int packedOverlay, int color) {
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
