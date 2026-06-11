package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.InfestedWormModel;
import com.example.alieninvasion.entity.InfestedWormEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Segmented infection worm. Growth stages scale via the SCALE attribute.
public class InfestedWormRenderer extends MobRenderer<InfestedWormEntity, InfestedWormModel<InfestedWormEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/infested_worm.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/entity/infested_worm_eyes.png");

    public InfestedWormRenderer(EntityRendererProvider.Context context) {
        super(context, new InfestedWormModel<>(context.bakeLayer(ModModelLayers.INFESTED_WORM)), 0.4F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    public ResourceLocation getTextureLocation(InfestedWormEntity entity) {
        return TEXTURE;
    }
}
