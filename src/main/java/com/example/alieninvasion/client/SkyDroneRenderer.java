package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PhantomRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.Phantom;

// Sky Drone on the vanilla Phantom layout (swoop AI) with a custom alien texture.
public class SkyDroneRenderer extends PhantomRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/sky_drone.png");

    public SkyDroneRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Phantom entity) {
        return TEXTURE;
    }
}
