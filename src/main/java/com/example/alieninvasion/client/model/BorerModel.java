package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.BorerVehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class BorerModel extends EntityModel<BorerVehicleEntity> {
    private final ModelPart body;
    private final ModelPart drill;

    public BorerModel(ModelPart root) {
        this.body = root.getChild("body");
        this.drill = root.getChild("drill");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5.0F, -4.0F, -5.0F, 10.0F, 7.0F, 12.0F)
                .texOffs(0, 20).addBox(-6.0F, 1.0F, -5.5F, 2.0F, 3.0F, 11.0F)
                .texOffs(26, 20).addBox(4.0F, 1.0F, -5.5F, 2.0F, 3.0F, 11.0F)
                .texOffs(38, 0).addBox(-3.0F, -6.0F, -2.0F, 6.0F, 2.0F, 5.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        root.addOrReplaceChild("drill", CubeListBuilder.create()
                .texOffs(0, 34).addBox(-3.0F, -3.0F, -10.0F, 6.0F, 6.0F, 5.0F)
                .texOffs(22, 34).addBox(-2.0F, -2.0F, -14.0F, 4.0F, 4.0F, 4.0F)
                .texOffs(38, 34).addBox(-1.0F, -1.0F, -17.0F, 2.0F, 2.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 48);
    }

    @Override
    public void setupAnim(BorerVehicleEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        this.drill.zRot = ageInTicks * 0.75F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
            int packedOverlay, int color) {
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.drill.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
