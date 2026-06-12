package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.PalladiumArmorModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class PalladiumArmorRenderer implements ArmorRenderer {
    private static final ResourceLocation LAYER_1 = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/models/armor/palladium_layer_1.png");
    private static final ResourceLocation LAYER_2 = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/models/armor/palladium_layer_2.png");

    private PalladiumArmorModel model;

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack,
                       LivingEntity entity, EquipmentSlot slot, int light,
                       HumanoidModel<LivingEntity> contextModel) {
        if (this.model == null) {
            this.model = new PalladiumArmorModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.PALLADIUM_ARMOR));
        }

        contextModel.copyPropertiesTo(this.model);
        this.model.setVisibleForSlot(slot);
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, this.model,
                slot == EquipmentSlot.LEGS ? LAYER_2 : LAYER_1);
    }
}
