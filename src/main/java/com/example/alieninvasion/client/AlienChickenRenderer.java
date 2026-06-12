package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienChickenModel;
import com.example.alieninvasion.entity.AlienChickenEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// The swarm scout-bird on its own model: dome head, antennae, fluttering wings.
public class AlienChickenRenderer extends MobRenderer<AlienChickenEntity, AlienChickenModel<AlienChickenEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_chicken.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_chicken_eyes.png");

    public AlienChickenRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienChickenModel<>(context.bakeLayer(ModModelLayers.ALIEN_CHICKEN)), 0.3F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    public ResourceLocation getTextureLocation(AlienChickenEntity entity) {
        return TEXTURE;
    }
}
