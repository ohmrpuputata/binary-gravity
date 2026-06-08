package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.AlienTrollEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// New alien: the Troll thief, rendered on the vanilla humanoid (zombie) layout
// wearing the converted 64x64 alien-troll skin from the pack.
public class AlienTrollRenderer extends MobRenderer<AlienTrollEntity, HumanoidModel<AlienTrollEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/alien_troll.png");

    public AlienTrollRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(AlienTrollEntity entity) {
        return TEXTURE;
    }
}
