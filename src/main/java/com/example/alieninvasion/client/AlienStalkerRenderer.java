package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.AlienStalkerEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Rare Stalker rendered on the vanilla spider layout with a custom alien texture.
public class AlienStalkerRenderer extends MobRenderer<AlienStalkerEntity, SpiderModel<AlienStalkerEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/alien_stalker.png");

    public AlienStalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.6F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienStalkerEntity entity) {
        return TEXTURE;
    }
}
