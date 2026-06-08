package com.example.alieninvasion.client;

import com.example.alieninvasion.entity.TelekineticAlienEntity;
import net.minecraft.client.model.EndermanModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

import com.example.alieninvasion.AlienInvasionMod;

// Hybrid: the Telekinetic Alien now uses the vanilla Enderman model layout so
// it can wear the (recolored) enderman texture imported from the pack.
public class TelekineticAlienRenderer extends MobRenderer<TelekineticAlienEntity, EndermanModel<TelekineticAlienEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/telekinetic_alien.png");

    public TelekineticAlienRenderer(EntityRendererProvider.Context context) {
        super(context, new EndermanModel<>(context.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(TelekineticAlienEntity entity) {
        return TEXTURE;
    }
}
