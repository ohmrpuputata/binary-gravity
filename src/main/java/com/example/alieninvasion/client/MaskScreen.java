package com.example.alieninvasion.client;

import com.example.alieninvasion.block.MaskMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Экран слота маски. Фон рисуется программно (тёмная панель + утопленные ячейки),
 * чтобы не тащить отдельную текстуру GUI и совпадать со стилем HUD.
 */
public class MaskScreen extends AbstractContainerScreen<MaskMenu> {
    public MaskScreen(MaskMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        // Панель с фаской.
        g.fill(x - 1, y - 1, x + imageWidth + 1, y + imageHeight + 1, 0xFF05060A);
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF15171C);
        g.fill(x, y, x + imageWidth, y + 1, 0xFF323742);
        g.fill(x, y + imageHeight - 1, x + imageWidth, y + imageHeight, 0xFF05060A);
        // Утопленные ячейки под все слоты меню (позиции берём у самих слотов).
        for (Slot s : this.menu.slots) {
            int sx = x + s.x;
            int sy = y + s.y;
            g.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0xFF05060A);
            g.fill(sx, sy, sx + 16, sy + 16, 0xFF2A2E36);
        }
        // Акцент-рамка вокруг ячейки маски + подпись.
        Slot mask = this.menu.slots.get(0);
        int mx = x + mask.x;
        int my = y + mask.y;
        g.fill(mx - 2, my - 2, mx + 18, my - 1, 0xFF72E7FF);
        g.fill(mx - 2, my + 17, mx + 18, my + 18, 0xFF72E7FF);
        g.fill(mx - 2, my - 1, mx - 1, my + 17, 0xFF72E7FF);
        g.fill(mx + 17, my - 1, mx + 18, my + 17, 0xFF72E7FF);
        g.drawString(this.font, Component.translatable("container.alien-invasion.mask.slot"),
                mx - 2, my - 13, 0xFF9AA4AC, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }
}
