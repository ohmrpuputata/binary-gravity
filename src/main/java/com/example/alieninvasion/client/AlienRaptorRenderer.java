package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienRaptorModel;
import com.example.alieninvasion.entity.AlienRaptorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// The swarm's pack hunter, with glowing amber eyes.
public class AlienRaptorRenderer extends MobRenderer<AlienRaptorEntity, AlienRaptorModel<AlienRaptorEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_raptor.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/alien_raptor_eyes.png");

    public AlienRaptorRenderer(EntityRendererProvider.Context context) {
        super(context, new AlienRaptorModel<>(context.bakeLayer(ModModelLayers.ALIEN_RAPTOR)), 0.55F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    public ResourceLocation getTextureLocation(AlienRaptorEntity entity) {
        return TEXTURE;
    }
}
