package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.SkyDroneModel;
import com.example.alieninvasion.entity.SkyDroneEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Recon quad-drone on its own model: spinning rotors, glowing sensor eye.
public class SkyDroneRenderer extends MobRenderer<SkyDroneEntity, SkyDroneModel<SkyDroneEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/sky_drone.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/sky_drone_eyes.png");

    public SkyDroneRenderer(EntityRendererProvider.Context context) {
        super(context, new SkyDroneModel<>(context.bakeLayer(ModModelLayers.SKY_DRONE)), 0.5F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    public ResourceLocation getTextureLocation(SkyDroneEntity entity) {
        return TEXTURE;
    }
}
