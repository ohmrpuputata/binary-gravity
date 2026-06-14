package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.BioFilterMaskModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class BioFilterMaskRenderer implements ArmorRenderer {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/models/armor/bio_filter_mask.png");

    private BioFilterMaskModel model;

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack,
                       LivingEntity entity, EquipmentSlot slot, int light,
                       HumanoidModel<LivingEntity> contextModel) {
        if (slot != EquipmentSlot.HEAD) {
            return;
        }
        if (this.model == null) {
            this.model = new BioFilterMaskModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.BIO_FILTER_MASK));
        }
        contextModel.copyPropertiesTo(this.model);
        this.model.setAllVisible(false);
        this.model.head.visible = true;
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, this.model, TEXTURE);
    }
}
