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
 * Recon quad-drone: an armoured core with a glowing sensor eye and four rotor
 * arms whose blades spin fast while the whole craft bobs on station.
 * UV (64x32) mirrored in generate_machine_textures.py.
 */
public class SkyDroneModel<T extends Mob> extends EntityModel<T> {
    private final ModelPart core;
    private final ModelPart[] arms = new ModelPart[4];
    private final ModelPart[] rotors = new ModelPart[4];

    public SkyDroneModel(ModelPart root) {
        this.core = root.getChild("core");
        for (int i = 0; i < 4; i++) {
            this.arms[i] = root.getChild("arm" + i);
            this.rotors[i] = this.arms[i].getChild("rotor" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition core = root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, -2.0F, -3.0F, 6.0F, 4.0F, 6.0F)
                        .texOffs(25, 0).addBox(-1.0F, -1.0F, -3.6F, 2.0F, 2.0F, 1.0F), // sensor eye
                PartPose.offset(0.0F, 14.0F, 0.0F));
        core.addOrReplaceChild("antenna", CubeListBuilder.create()
                        .texOffs(32, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 3.0F, 1.0F),
                PartPose.offset(2.0F, -2.0F, 2.0F));

        for (int i = 0; i < 4; i++) {
            double ang = Math.PI / 4 + i * Math.PI / 2; // diagonals
            float ax = (float) Math.cos(ang) * 3.0F;
            float az = (float) Math.sin(ang) * 3.0F;
            PartDefinition arm = root.addOrReplaceChild("arm" + i, CubeListBuilder.create()
                            .texOffs(0, 11).addBox(-0.5F, -0.5F, 0.0F, 1.0F, 1.0F, 5.0F),
                    PartPose.offsetAndRotation(ax, 13.0F, az, 0.0F, (float) (-ang + Math.PI / 2), 0.0F));
            arm.addOrReplaceChild("rotor" + i, CubeListBuilder.create()
                            .texOffs(0, 18).addBox(-3.0F, 0.0F, -0.5F, 6.0F, 1.0F, 1.0F),
                    PartPose.offset(0.0F, -1.0F, 4.5F));
        }
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // Rotors scream, hull bobs and banks slightly toward where it looks.
        for (int i = 0; i < 4; i++) {
            rotors[i].yRot = ageInTicks * 2.2F + i;
            arms[i].zRot = Mth.sin(ageInTicks * 0.15F + i) * 0.04F;
        }
        core.y = 14.0F + Mth.sin(ageInTicks * 0.12F) * 0.6F;
        core.zRot = netHeadYaw * ((float) Math.PI / 180F) * -0.05F;
        core.xRot = headPitch * ((float) Math.PI / 180F) * 0.3F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        core.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        for (ModelPart arm : arms) {
            arm.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
    }
}
