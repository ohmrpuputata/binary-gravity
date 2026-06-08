package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.CaveLurkerEntity;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Cave Lurker on the vanilla spider layout with a custom texture.
public class CaveLurkerRenderer extends MobRenderer<CaveLurkerEntity, SpiderModel<CaveLurkerEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/cave_lurker.png");

    public CaveLurkerRenderer(EntityRendererProvider.Context context) {
        super(context, new SpiderModel<>(context.bakeLayer(ModelLayers.SPIDER)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(CaveLurkerEntity entity) {
        return TEXTURE;
    }
}
