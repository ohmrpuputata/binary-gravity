package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.UfoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

public class UfoModel extends EntityModel<UfoEntity> {
        private final ModelPart core;
        private final ModelPart innerRing;
        private final ModelPart midRing;
        private final ModelPart outerRing;

        public UfoModel(ModelPart root) {
                this.core = root.getChild("core");
                this.innerRing = root.getChild("inner_ring");
                this.midRing = root.getChild("mid_ring");
                this.outerRing = root.getChild("outer_ring");
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition meshdefinition = new MeshDefinition();
                PartDefinition partdefinition = meshdefinition.getRoot();

                // CORE: Pulsating energy ball (cube)
                partdefinition.addOrReplaceChild("core", CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F)
                                .texOffs(0, 16).addBox(-3.0F, -8.0F, -3.0F, 6.0F, 16.0F, 6.0F), // Vertical axis
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                // INNER RING: Spikes
                partdefinition.addOrReplaceChild("inner_ring", CubeListBuilder.create()
                                .texOffs(32, 0).addBox(-6.0F, -1.0F, -6.0F, 12.0F, 2.0F, 12.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                // MID RING: Tech Details
                PartDefinition midRing = partdefinition.addOrReplaceChild("mid_ring", CubeListBuilder.create()
                                .texOffs(0, 48).addBox(-10.0F, -2.0F, -10.0F, 20.0F, 4.0F, 20.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                midRing.addOrReplaceChild("thruster1",
                                CubeListBuilder.create().texOffs(0, 38).addBox(-2.0F, 2.0F, 8.0F, 4.0F, 4.0F, 4.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));
                midRing.addOrReplaceChild("thruster2",
                                CubeListBuilder.create().texOffs(0, 38).addBox(-2.0F, 2.0F, -12.0F, 4.0F, 4.0F, 4.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));
                midRing.addOrReplaceChild("thruster3",
                                CubeListBuilder.create().texOffs(0, 38).addBox(8.0F, 2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));
                midRing.addOrReplaceChild("thruster4",
                                CubeListBuilder.create().texOffs(0, 38).addBox(-12.0F, 2.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                // OUTER RING: Large, rotating
                partdefinition.addOrReplaceChild("outer_ring", CubeListBuilder.create()
                                .texOffs(0, 80).addBox(-14.0F, -1.0F, -14.0F, 28.0F, 2.0F, 28.0F),
                                PartPose.offset(0.0F, 0.0F, 0.0F));

                return LayerDefinition.create(meshdefinition, 128, 128); // Increased texture size
        }

        @Override
        public void setupAnim(UfoEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                        float netHeadYaw, float headPitch) {
                // Clear all rotations to keep model stable
                this.core.yRot = 0.0F;
                this.core.xRot = 0.0F;
                this.innerRing.xRot = 0.0F;
                this.innerRing.yRot = 0.0F;
                this.midRing.yRot = 0.0F;
                this.outerRing.xRot = 0.0F;
                this.outerRing.zRot = 0.0F;

                // Reset scales to defaults
                this.core.xScale = 1.0F;
                this.core.yScale = 1.0F;
                this.core.zScale = 1.0F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                        int packedOverlay,
                        int color) {
                core.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                innerRing.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                midRing.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                outerRing.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
}
