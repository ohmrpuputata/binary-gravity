package com.example.alieninvasion.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.world.entity.Mob;

/** Generic full-bright eye glow for the swarm — every alien's eyes shine in the dark. */
public class AlienEyesLayer<T extends Mob, M extends EntityModel<T>> extends EyesLayer<T, M> {
    private final RenderType renderType;

    public AlienEyesLayer(RenderLayerParent<T, M> parent, net.minecraft.resources.ResourceLocation texture) {
        super(parent);
        this.renderType = RenderType.eyes(texture);
    }

    @Override
    public RenderType renderType() {
        return renderType;
    }
}
