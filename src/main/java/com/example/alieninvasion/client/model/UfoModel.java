package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.UfoEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/**
 * A REAL flying saucer: wide layered hull disc, glass command dome on top, a
 * rotating ring of running lights and a glowing tractor emitter underneath.
 * UV (128x64) is mirrored in generate_machine_textures.py.
 */
public class UfoModel extends EntityModel<UfoEntity> {
        private final ModelPart hull;
        private final ModelPart dome;
        private final ModelPart lights;
        private final ModelPart emitter;

        public UfoModel(ModelPart root) {
                this.hull = root.getChild("hull");
                this.dome = root.getChild("dome");
                this.lights = root.getChild("lights");
                this.emitter = root.getChild("emitter");
        }

        public static LayerDefinition createBodyLayer() {
                MeshDefinition mesh = new MeshDefinition();
                PartDefinition root = mesh.getRoot();

                // Main disc + tapered top/bottom plates.
                root.addOrReplaceChild("hull", CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-12.0F, -2.0F, -12.0F, 24.0F, 4.0F, 24.0F)
                                .texOffs(0, 29).addBox(-8.0F, -4.0F, -8.0F, 16.0F, 2.0F, 16.0F)
                                .texOffs(0, 29).addBox(-8.0F, 2.0F, -8.0F, 16.0F, 2.0F, 16.0F),
                        PartPose.offset(0.0F, 16.0F, 0.0F));

                // Glass command dome.
                root.addOrReplaceChild("dome", CubeListBuilder.create()
                                .texOffs(96, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 4.0F, 8.0F),
                        PartPose.offset(0.0F, 12.0F, 0.0F));

                // Running lights: eight studs around the rim, on a spinning carrier.
                PartDefinition lights = root.addOrReplaceChild("lights",
                        CubeListBuilder.create(), PartPose.offset(0.0F, 16.0F, 0.0F));
                for (int i = 0; i < 8; i++) {
                        double ang = i * Math.PI / 4.0;
                        lights.addOrReplaceChild("light" + i, CubeListBuilder.create()
                                        .texOffs(0, 48).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F),
                                PartPose.offset((float) (Math.cos(ang) * 12.5), 0.0F,
                                        (float) (Math.sin(ang) * 12.5)));
                }

                // Tractor emitter under the belly.
                root.addOrReplaceChild("emitter", CubeListBuilder.create()
                                .texOffs(16, 48).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 3.0F, 4.0F),
                        PartPose.offset(0.0F, 18.0F, 0.0F));

                return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        public void setupAnim(UfoEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                        float netHeadYaw, float headPitch) {
                // The light ring spins steadily; the hull banks in a slow figure-eight.
                this.lights.yRot = ageInTicks * 0.18F;
                this.hull.zRot = Mth.sin(ageInTicks * 0.05F) * 0.05F;
                this.hull.xRot = Mth.cos(ageInTicks * 0.04F) * 0.05F;
                this.dome.zRot = this.hull.zRot;
                this.dome.xRot = this.hull.xRot;
                this.emitter.y = 18.0F + Mth.sin(ageInTicks * 0.25F) * 0.4F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                        int packedOverlay, int color) {
                hull.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                dome.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                lights.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
                emitter.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
}
