package com.example.alieninvasion.client.model;

import com.example.alieninvasion.entity.MeteorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

/**
 * Tumbling meteor: an irregular boulder — core with offset rocky lumps on every
 * face — that slowly somersaults as it falls. UV (64x64) mirrored in
 * generate_machine_textures.py.
 */
public class MeteorModel extends EntityModel<MeteorEntity> {
    private final ModelPart rock;

    public MeteorModel(ModelPart root) {
        this.rock = root.getChild("rock");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("rock", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F)
                        .texOffs(0, 21).addBox(-7.0F, -3.0F, -3.0F, 3.0F, 6.0F, 6.0F)
                        .texOffs(0, 21).addBox(4.0F, -2.0F, -4.0F, 3.0F, 5.0F, 6.0F)
                        .texOffs(19, 21).addBox(-3.0F, 4.0F, -3.0F, 6.0F, 3.0F, 6.0F)
                        .texOffs(19, 21).addBox(-2.0F, -7.5F, -2.0F, 5.0F, 3.0F, 5.0F)
                        .texOffs(41, 21).addBox(-3.0F, -3.0F, 4.0F, 5.0F, 5.0F, 3.0F)
                        .texOffs(41, 21).addBox(-2.0F, -1.0F, -7.0F, 4.0F, 4.0F, 3.0F),
                PartPose.offset(0.0F, 16.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(MeteorEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        this.rock.xRot = ageInTicks * 0.11F;
        this.rock.yRot = ageInTicks * 0.07F;
        this.rock.zRot = ageInTicks * 0.05F;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        rock.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
