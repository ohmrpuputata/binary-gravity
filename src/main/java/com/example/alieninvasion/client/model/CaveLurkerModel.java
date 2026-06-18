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
 * Cave Lurker: a bespoke alien ambush-spider, NOT the vanilla spider layout. It has
 * a fused teardrop body (low cephalothorax + a raised, bulbous abdomen), a forward
 * maw with two ivory fangs, and eight long, double-jointed legs that scuttle in a
 * diagonal gait. UV (64x64) is painted by tools/generate_cave_lurker_texture.py.
 */
public class CaveLurkerModel<T extends Mob> extends EntityModel<T> {
    private final ModelPart thorax;
    private final ModelPart head;
    private final ModelPart abdomen;
    private final ModelPart jawLeft;
    private final ModelPart jawRight;
    private final ModelPart[] legs = new ModelPart[8];

    // Front legs fan well forward, rear legs sweep back; a wide, splayed stance.
    private static final float[] LEG_Z = {-3.5F, -1.2F, 1.2F, 3.5F};
    private static final float[] LEG_FAN = {0.95F, 0.4F, -0.4F, -0.95F};
    private static final float FEMUR_LIFT = -0.4F; // femur reaches OUT then slightly up

    public CaveLurkerModel(ModelPart root) {
        this.thorax = root.getChild("thorax");
        this.head = this.thorax.getChild("head");
        this.abdomen = this.thorax.getChild("abdomen");
        this.jawLeft = this.head.getChild("jaw_left");
        this.jawRight = this.head.getChild("jaw_right");
        for (int i = 0; i < 8; i++) {
            this.legs[i] = root.getChild("leg" + i);
        }
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Low front body. Pivot near the ground (y=14) so the legs reach down to 24.
        PartDefinition thorax = root.addOrReplaceChild("thorax", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -3.0F, -4.0F, 8.0F, 5.0F, 7.0F),
                PartPose.offset(0.0F, 14.0F, -2.0F));

        // Forward maw.
        PartDefinition head = thorax.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 33).addBox(-3.0F, -2.0F, -5.0F, 6.0F, 4.0F, 5.0F),
                PartPose.offset(0.0F, 0.5F, -4.0F));
        head.addOrReplaceChild("jaw_right", CubeListBuilder.create()
                        .texOffs(24, 33).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(-1.6F, 1.5F, -4.0F, 0.2F, 0.25F, 0.0F));
        head.addOrReplaceChild("jaw_left", CubeListBuilder.create().mirror()
                        .texOffs(24, 33).addBox(-0.5F, 0.0F, -3.0F, 1.0F, 3.0F, 3.0F),
                PartPose.offsetAndRotation(1.6F, 1.5F, -4.0F, 0.2F, -0.25F, 0.0F));

        // Raised, bulbous rear. Tilts up so it humps over the thorax.
        thorax.addOrReplaceChild("abdomen", CubeListBuilder.create()
                        .texOffs(0, 14).addBox(-4.5F, -4.0F, 0.0F, 9.0F, 7.0F, 10.0F),
                PartPose.offsetAndRotation(0.0F, -1.5F, 3.0F, -0.22F, 0.0F, 0.0F));

        for (int i = 0; i < 8; i++) {
            boolean left = i >= 4;
            int idx = i % 4;
            CubeListBuilder femur = CubeListBuilder.create().texOffs(40, 8);
            CubeListBuilder tibia = CubeListBuilder.create().texOffs(40, 15);
            if (left) {
                femur.mirror();
                tibia.mirror();
            }
            femur.addBox(0.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F);
            tibia.addBox(0.0F, -1.0F, -1.0F, 7.0F, 2.0F, 2.0F);
            float baseYaw = left ? LEG_FAN[idx] : (float) Math.PI - LEG_FAN[idx];
            PartDefinition leg = root.addOrReplaceChild("leg" + i, femur,
                    PartPose.offsetAndRotation((left ? 4.0F : -4.0F), 14.0F, LEG_Z[idx],
                            0.0F, baseYaw, FEMUR_LIFT));
            // Knee folds the tibia sharply down to plant the foot on the ground.
            leg.addOrReplaceChild("leg" + i + "_lower", tibia,
                    PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.6F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // Clamp the limb amount so a teleport/large dt doesn't fling the legs.
        float amount = Math.min(limbSwingAmount, 1.0F);

        this.head.yRot = netHeadYaw * ((float) Math.PI / 180F);
        this.head.xRot = headPitch * ((float) Math.PI / 180F);

        // Abdomen breathes / bobs as it moves.
        this.abdomen.xRot = -0.22F + Mth.cos(ageInTicks * 0.12F) * 0.05F
                + Mth.cos(limbSwing * 1.3F) * 0.06F * amount;

        // Eight-legged diagonal scuttle.
        for (int i = 0; i < 8; i++) {
            boolean left = i >= 4;
            int idx = i % 4;
            float baseYaw = left ? LEG_FAN[idx] : (float) Math.PI - LEG_FAN[idx];
            float phase = idx * 1.4F + (left ? (float) Math.PI : 0.0F);
            float sweep = Mth.cos(limbSwing * 1.3F + phase) * 0.35F * amount;
            float lift = Math.max(0.0F, Mth.sin(limbSwing * 1.3F + phase)) * 0.45F * amount;
            this.legs[i].yRot = baseYaw + sweep * (left ? 1.0F : -1.0F);
            this.legs[i].zRot = FEMUR_LIFT - lift + Mth.sin(ageInTicks * 0.1F + i) * 0.02F;
        }

        // Fangs flare when hunting, idle-twitch otherwise.
        if (entity.isAggressive()) {
            float chomp = Math.abs(Mth.sin(ageInTicks * 0.5F)) * 0.5F;
            this.jawRight.zRot = 0.25F + chomp;
            this.jawLeft.zRot = -0.25F - chomp;
        } else {
            float idle = Mth.sin(ageInTicks * 0.12F) * 0.06F;
            this.jawRight.zRot = 0.12F + idle;
            this.jawLeft.zRot = -0.12F - idle;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, int color) {
        thorax.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        for (ModelPart leg : legs) {
            leg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        }
    }
}
