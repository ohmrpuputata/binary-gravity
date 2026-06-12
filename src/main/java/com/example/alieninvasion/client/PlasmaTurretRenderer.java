package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.block.PlasmaTurretBlockEntity;
import com.example.alieninvasion.client.model.PlasmaTurretModel;
import com.example.alieninvasion.entity.AlienUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Renders the turret's rotating head: the yoke yaws to track the nearest alien
 * and the barrels elevate toward it; with no target it slowly sweeps the horizon
 * like a radar. Smoothly interpolated client-side, no extra network traffic.
 */
public class PlasmaTurretRenderer implements BlockEntityRenderer<PlasmaTurretBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/block/plasma_turret_head.png");

    private final ModelPart yoke;
    private final ModelPart cradle;

    public PlasmaTurretRenderer(BlockEntityRendererProvider.Context ctx) {
        ModelPart root = ctx.bakeLayer(ModModelLayers.PLASMA_TURRET);
        this.yoke = root.getChild(PlasmaTurretModel.YOKE);
        this.cradle = this.yoke.getChild(PlasmaTurretModel.CRADLE);
    }

    @Override
    public void render(PlasmaTurretBlockEntity be, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;

        float yaw;
        float pitch = 0.0F;
        LivingEntity target = nearestAlien(be);
        if (target != null) {
            double dx = target.getX() - (be.getBlockPos().getX() + 0.5);
            double dy = target.getEyeY() - (be.getBlockPos().getY() + 1.2);
            double dz = target.getZ() - (be.getBlockPos().getZ() + 0.5);
            double horiz = Math.sqrt(dx * dx + dz * dz);
            yaw = (float) (Mth.atan2(dz, dx)) - (float) (Math.PI / 2.0);
            pitch = (float) -Mth.atan2(dy, horiz);
            pitch = Mth.clamp(pitch, -1.2F, 0.4F);
        } else {
            // Idle radar sweep.
            yaw = (be.getLevel().getGameTime() + partialTick) * 0.03F;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        this.yoke.yRot = yaw;
        this.cradle.xRot = pitch;
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        // Full-bright is wrong for a block; use the passed light so it sits in the world.
        this.yoke.render(poseStack, vc, light, overlay);
        poseStack.popPose();
    }

    private LivingEntity nearestAlien(PlasmaTurretBlockEntity be) {
        AABB box = new AABB(be.getBlockPos()).inflate(16.0D);
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        Vec3 c = Vec3.atCenterOf(be.getBlockPos());
        for (LivingEntity e : be.getLevel().getEntitiesOfClass(LivingEntity.class, box,
                en -> en.isAlive() && AlienUtils.isAlliedTo(null, en))) {
            double d = e.distanceToSqr(c);
            if (d < bestSq) { bestSq = d; best = e; }
        }
        return best;
    }
}
