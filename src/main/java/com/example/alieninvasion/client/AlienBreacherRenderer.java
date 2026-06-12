package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienHumanoidModel;
import com.example.alieninvasion.entity.AlienBreacherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// BREACHER build of the shared swarm skeleton: armoured walker with wedge-drill
// arms on both hands - the dedicated wall-chewer.
public class AlienBreacherRenderer extends MobRenderer<AlienBreacherEntity, AlienHumanoidModel<AlienBreacherEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_breacher.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_breacher_eyes.png");

    public AlienBreacherRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienHumanoidModel<>(context.bakeLayer(ModModelLayers.ALIEN_BREACHER),
                AlienHumanoidModel.Variant.BREACHER), 0.7F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    protected void scale(AlienBreacherEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.35F, 1.35F, 1.35F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienBreacherEntity entity) {
        return TEXTURE;
    }
}
