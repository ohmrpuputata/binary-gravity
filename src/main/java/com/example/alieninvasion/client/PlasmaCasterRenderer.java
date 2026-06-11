package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienHumanoidModel;
import com.example.alieninvasion.entity.PlasmaCasterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// CASTER build of the shared swarm-humanoid skeleton, with glowing eyes.
public class PlasmaCasterRenderer extends MobRenderer<PlasmaCasterEntity, AlienHumanoidModel<PlasmaCasterEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/plasma_caster.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/plasma_caster_eyes.png");

    public PlasmaCasterRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienHumanoidModel<>(context.bakeLayer(ModModelLayers.PLASMA_CASTER),
                AlienHumanoidModel.Variant.CASTER), 0.5F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    protected void scale(PlasmaCasterEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.05F, 1.05F, 1.05F);
    }

    @Override
    public ResourceLocation getTextureLocation(PlasmaCasterEntity entity) {
        return TEXTURE;
    }
}
