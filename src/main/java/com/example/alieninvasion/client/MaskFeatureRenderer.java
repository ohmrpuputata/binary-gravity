package com.example.alieninvasion.client;

import com.example.alieninvasion.AlienInvasionMod;
import com.example.alieninvasion.client.model.BioFilterMaskModel;
import com.example.alieninvasion.logic.MaskSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Рендер маски из слота-attachment на лице игрока (поверх шлема/головы). Модель —
 * та же {@link BioFilterMaskModel}, что и у предмета в руке/слоте головы; текстура
 * берётся по предмету маски (пока — био-фильтр; разные маски добавляются позже).
 */
public class MaskFeatureRenderer
        extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    private static final ResourceLocation DEFAULT_TEX = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/models/armor/bio_filter_mask.png");
    private static final ResourceLocation CLOTH_TEX = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/models/armor/cloth_respirator.png");
    private static final ResourceLocation GAS_TEX = ResourceLocation.fromNamespaceAndPath(
            AlienInvasionMod.MODID, "textures/models/armor/gas_mask.png");

    private static ResourceLocation texFor(ItemStack mask) {
        if (mask.is(com.example.alieninvasion.registry.ItemRegistry.GAS_MASK)) {
            return GAS_TEX;
        }
        if (mask.is(com.example.alieninvasion.registry.ItemRegistry.CLOTH_RESPIRATOR)) {
            return CLOTH_TEX;
        }
        return DEFAULT_TEX;
    }

    private BioFilterMaskModel model;

    public MaskFeatureRenderer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack mask = MaskSlot.get(player);
        if (mask.isEmpty() || player.isInvisible()) {
            return;
        }
        if (this.model == null) {
            this.model = new BioFilterMaskModel(
                    Minecraft.getInstance().getEntityModels().bakeLayer(ModModelLayers.BIO_FILTER_MASK));
        }
        this.model.head.visible = true;
        // Встаём в систему координат ГОЛОВЫ игрока и рисуем там часть-маску — так маска
        // садится на лицо и следует за поворотом головы (а не висит у тела).
        pose.pushPose();
        this.getParentModel().head.translateAndRotate(pose);
        this.model.head.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texFor(mask))),
                light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
    }
}
