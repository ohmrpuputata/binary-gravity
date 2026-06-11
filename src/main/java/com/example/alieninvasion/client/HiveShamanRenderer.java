package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienHumanoidModel;
import com.example.alieninvasion.entity.HiveShamanEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// SHAMAN build of the shared swarm-humanoid skeleton, with glowing eyes.
public class HiveShamanRenderer extends MobRenderer<HiveShamanEntity, AlienHumanoidModel<HiveShamanEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/hive_shaman.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/hive_shaman_eyes.png");

    public HiveShamanRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienHumanoidModel<>(context.bakeLayer(ModModelLayers.HIVE_SHAMAN),
                AlienHumanoidModel.Variant.SHAMAN), 0.5F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    protected void scale(HiveShamanEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.1F, 1.1F, 1.1F);
    }

    @Override
    public ResourceLocation getTextureLocation(HiveShamanEntity entity) {
        return TEXTURE;
    }
}
