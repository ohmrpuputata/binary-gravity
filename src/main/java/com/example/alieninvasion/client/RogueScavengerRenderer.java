package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.entity.RogueScavengerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Выживший-NPC рендерится как обычный ИГРОК: player-модель, надетое снаряжение
 * поверх, и СЛУЧАЙНЫЙ скин из набора (по entity.getSkin()). Без светящихся глаз —
 * это живой человек, а не заражённый.
 */
public class RogueScavengerRenderer
        extends HumanoidMobRenderer<RogueScavengerEntity, HumanoidModel<RogueScavengerEntity>> {

    private static final ResourceLocation[] SKINS = {
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "textures/entity/scavenger_0.png"),
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "textures/entity/scavenger_1.png"),
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "textures/entity/scavenger_2.png"),
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "textures/entity/scavenger_3.png"),
    };

    public RogueScavengerRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(RogueScavengerEntity entity) {
        return SKINS[Math.floorMod(entity.getSkin(), SKINS.length)];
    }
}
