package com.example.alieninvasion.client;

import com.example.alieninvasion.entity.AlienBruteEntity;
import net.minecraft.client.model.IronGolemModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.example.alieninvasion.AlienInvasionMod;

// Hybrid: the Brute now uses the vanilla Iron Golem model layout so it can wear
// the Clash Royale golem texture imported from the pack.
public class AlienBruteRenderer extends MobRenderer<AlienBruteEntity, IronGolemModel<AlienBruteEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/alien_brute.png");

    public AlienBruteRenderer(EntityRendererProvider.Context context) {
        super(context, new IronGolemModel<>(context.bakeLayer(ModelLayers.IRON_GOLEM)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienBruteEntity entity) {
        return TEXTURE;
    }
}
