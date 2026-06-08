package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.HiveTyrantEntity;
import net.minecraft.client.model.WardenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// New alien mini-boss "Hive Tyrant" using the vanilla Warden model layout with
// the Martian-recolored warden texture from the pack.
public class HiveTyrantRenderer extends MobRenderer<HiveTyrantEntity, WardenModel<HiveTyrantEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/hive_tyrant.png");

    public HiveTyrantRenderer(EntityRendererProvider.Context context) {
        super(context, new WardenModel<>(context.bakeLayer(ModelLayers.WARDEN)), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(HiveTyrantEntity entity) {
        return TEXTURE;
    }
}
