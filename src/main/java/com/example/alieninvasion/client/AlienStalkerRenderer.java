package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienHumanoidModel;
import com.example.alieninvasion.entity.AlienStalkerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// STALKER build of the shared swarm-humanoid skeleton, with glowing eyes.
public class AlienStalkerRenderer extends MobRenderer<AlienStalkerEntity, AlienHumanoidModel<AlienStalkerEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_stalker.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_stalker_eyes.png");

    public AlienStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienHumanoidModel<>(context.bakeLayer(ModModelLayers.ALIEN_STALKER),
                AlienHumanoidModel.Variant.STALKER), 0.5F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    protected void scale(AlienStalkerEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.0F, 1.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienStalkerEntity entity) {
        return TEXTURE;
    }
}
