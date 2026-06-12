package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.AdvancedArmorModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class AdvancedArmorRenderer implements ArmorRenderer {
    private final ModelLayerLocation modelLayer;
    private final ResourceLocation layerOne;
    private final ResourceLocation layerTwo;
    private AdvancedArmorModel model;

    public AdvancedArmorRenderer(ModelLayerLocation modelLayer, String textureName) {
        this.modelLayer = modelLayer;
        this.layerOne = ResourceLocation.fromNamespaceAndPath(
                AlienInvasionMod.MODID, "textures/models/armor/" + textureName + "_layer_1.png");
        this.layerTwo = ResourceLocation.fromNamespaceAndPath(
                AlienInvasionMod.MODID, "textures/models/armor/" + textureName + "_layer_2.png");
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack,
                       LivingEntity entity, EquipmentSlot slot, int light,
                       HumanoidModel<LivingEntity> contextModel) {
        if (this.model == null) {
            this.model = new AdvancedArmorModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(this.modelLayer));
        }
        contextModel.copyPropertiesTo(this.model);
        this.model.setVisibleForSlot(slot);
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, this.model,
                slot == EquipmentSlot.LEGS ? this.layerTwo : this.layerOne);
    }
}
