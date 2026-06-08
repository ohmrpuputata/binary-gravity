package com.example.alieninvasion.client;

import com.example.alieninvasion.entity.UfoEntity;
import com.example.alieninvasion.client.model.UfoModel;
import com.example.alieninvasion.AlienInvasionMod;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class UfoRenderer extends MobRenderer<UfoEntity, UfoModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/ufo.png");

    public UfoRenderer(EntityRendererProvider.Context context) {
        super(context, new UfoModel(context.bakeLayer(ModModelLayers.UFO)), 1.0F);
    }

    @Override
    protected void scale(UfoEntity entity, PoseStack poseStack, float partialTick) {
        float s = switch (entity.getVariant()) {
            case UfoEntity.DESTROYER -> 1.5F;
            case UfoEntity.CARRIER -> 1.9F;
            default -> 1.0F;
        };
        poseStack.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(UfoEntity entity) {
        return TEXTURE;
    }
}
