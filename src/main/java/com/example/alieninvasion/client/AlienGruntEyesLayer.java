package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AlienGruntModel;
import com.example.alieninvasion.entity.AlienGruntEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;

/** Full-bright glow for the grunt's eyes and antenna tips — they shine in the dark. */
public class AlienGruntEyesLayer extends EyesLayer<AlienGruntEntity, AlienGruntModel> {
    private static final RenderType EYES = RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "textures/entity/alien_grunt_eyes.png"));

    public AlienGruntEyesLayer(RenderLayerParent<AlienGruntEntity, AlienGruntModel> parent) {
        super(parent);
    }

    @Override
    public RenderType renderType() {
        return EYES;
    }
}
