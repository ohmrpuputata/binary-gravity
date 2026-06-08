package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.HiveShamanEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Rare Hive Shaman rendered on the vanilla humanoid (zombie) layout, custom texture.
public class HiveShamanRenderer extends MobRenderer<HiveShamanEntity, HumanoidModel<HiveShamanEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/hive_shaman.png");

    public HiveShamanRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(HiveShamanEntity entity) {
        return TEXTURE;
    }
}
