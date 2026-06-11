package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienHumanoidModel;
import com.example.alieninvasion.entity.HiveTyrantEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// TYRANT build of the shared swarm-humanoid skeleton, with glowing eyes.
public class HiveTyrantRenderer extends MobRenderer<HiveTyrantEntity, AlienHumanoidModel<HiveTyrantEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/hive_tyrant.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/hive_tyrant_eyes.png");

    public HiveTyrantRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienHumanoidModel<>(context.bakeLayer(ModModelLayers.HIVE_TYRANT),
                AlienHumanoidModel.Variant.TYRANT), 1.0F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    protected void scale(HiveTyrantEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(HiveTyrantEntity entity) {
        return TEXTURE;
    }
}
