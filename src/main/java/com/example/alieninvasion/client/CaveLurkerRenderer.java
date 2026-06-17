package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.CaveLurkerModel;
import com.example.alieninvasion.entity.CaveLurkerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

// Cave Lurker: a bespoke alien ambush-spider (no longer the vanilla spider layout),
// with bioluminescent eyes that glow in the dark of the caves it haunts.
public class CaveLurkerRenderer extends MobRenderer<CaveLurkerEntity, CaveLurkerModel<CaveLurkerEntity>> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/cave_lurker.png");
    private static final ResourceLocation EYES = ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID,
            "textures/entity/cave_lurker_eyes.png");

    public CaveLurkerRenderer(EntityRendererProvider.Context context) {
        super(context, new CaveLurkerModel<>(context.bakeLayer(ModModelLayers.CAVE_LURKER)), 0.6F);
        this.addLayer(new AlienEyesLayer<>(this, EYES));
    }

    @Override
    public ResourceLocation getTextureLocation(CaveLurkerEntity entity) {
        return TEXTURE;
    }
}
