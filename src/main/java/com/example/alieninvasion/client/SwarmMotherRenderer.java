package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.SwarmMotherModel;
import com.example.alieninvasion.entity.SwarmMotherEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Swarm Mother: the final boss - a towering insectoid brood-queen.
public class SwarmMotherRenderer extends MobRenderer<SwarmMotherEntity, SwarmMotherModel> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/swarm_mother.png");

    public SwarmMotherRenderer(EntityRendererProvider.Context context) {
        super(context, new SwarmMotherModel(context.bakeLayer(ModModelLayers.SWARM_MOTHER)), 1.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(SwarmMotherEntity entity) {
        return TEXTURE;
    }
}
