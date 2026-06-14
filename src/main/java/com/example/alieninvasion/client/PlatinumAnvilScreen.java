package com.example.alieninvasion.client;

import com.example.alieninvasion.block.PlatinumAnvilMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Client GUI for the Platinum Anvil.
 * Rendered entirely in code — no external texture file required.
 */
public class PlatinumAnvilScreen extends AbstractContainerScreen<PlatinumAnvilMenu> {

    /* ---- Minecraft GUI palette ---- */
    private static final int BG          = 0xFFC6C6C6;
    private static final int LIGHT       = 0xFFFFFFFF;
    private static final int DARK        = 0xFF555555;
    private static final int SLOT_DARK   = 0xFF373737;
    private static final int SLOT_LIGHT  = 0xFFFFFFFF;
    private static final int SLOT_BG     = 0xFF8B8B8B;
    private static final int LABEL_COLOR = 0x404040;

    /* ---- slot background positions (top-left of 18×18 area) ---- */
    private static final int GRID_X0 = 30,  GRID_Y0 = 17;
    private static final int CAT_X   = 48, CAT_Y   = 35; // Center of the 3x3 grid (30+18, 17+18)
    private static final int RES_X   = 120, RES_Y  = 31; // result uses a 26×26 frame (item slot is at 125)

    private static final Component CATALYST_TIP = Component.literal("Поместите Осколок");


    public PlatinumAnvilScreen(PlatinumAnvilMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth  = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 73;
    }

    /* ================================================================ */

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float pt) {
        super.render(gfx, mouseX, mouseY, pt);
        this.renderTooltip(gfx, mouseX, mouseY);

        // Ghost-slot tooltips (only when slot is empty)
        if (menu.getSlot(4).getItem().isEmpty()
                && isHovering(CAT_X, CAT_Y, 18, 18, mouseX, mouseY)) {
            gfx.renderTooltip(font, CATALYST_TIP, mouseX, mouseY);
        }

    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(font, this.title,                this.titleLabelX,     this.titleLabelY,     LABEL_COLOR, false);
        gfx.drawString(font, this.playerInventoryTitle,  this.inventoryLabelX, this.inventoryLabelY, LABEL_COLOR, false);
    }

    /* ================================================================ */

    @Override
    protected void renderBg(GuiGraphics gfx, float pt, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;

        /* ---- main panel ---- */
        drawPanel(gfx, x, y, imageWidth, imageHeight);

        /* ---- crafting grid 3×3 ---- */
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                drawSlot(gfx, x + GRID_X0 + c * 18, y + GRID_Y0 + r * 18);

        /* ---- arrow between grid and result ---- */
        gfx.drawString(font, "\u25B6", x + 99, y + 39, LABEL_COLOR, false); // ▶

        /* ---- result slot (26×26 frame) ---- */
        drawResultSlot(gfx, x + RES_X, y + RES_Y);

        /* ---- player inventory (3 rows + hotbar) ---- */
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 9; c++)
                drawSlot(gfx, x + 7 + c * 18, y + 83 + r * 18);
        for (int c = 0; c < 9; c++)
            drawSlot(gfx, x + 7 + c * 18, y + 141);

        /* ---- ghost items ---- */
        if (menu.getSlot(4).getItem().isEmpty()) {
            renderGhost(gfx, new ItemStack(Items.AMETHYST_SHARD), x + 49, y + 36);
        }

    }

    /* ================================================================
     *  Drawing helpers
     * ================================================================ */

    /** Raised 3-D panel (standard MC container background). */
    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, BG);
        // highlight
        g.fill(x, y, x + w - 1, y + 1, LIGHT);
        g.fill(x, y + 1, x + 1, y + h - 1, LIGHT);
        // shadow
        g.fill(x + 1, y + h - 1, x + w, y + h, DARK);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, DARK);
    }

    /** Standard 18×18 inset slot outline. */
    private static void drawSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 1, SLOT_DARK);
        g.fill(x, y + 1, x + 1, y + 17, SLOT_DARK);
        g.fill(x + 1, y + 17, x + 18, y + 18, SLOT_LIGHT);
        g.fill(x + 17, y + 1, x + 18, y + 17, SLOT_LIGHT);
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_BG);
    }

    /** Decorative 26×26 result slot. */
    private static void drawResultSlot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 26, y + 1, SLOT_DARK);
        g.fill(x, y + 1, x + 1, y + 25, SLOT_DARK);
        g.fill(x + 1, y + 25, x + 26, y + 26, SLOT_LIGHT);
        g.fill(x + 25, y + 1, x + 26, y + 25, SLOT_LIGHT);
        g.fill(x + 1, y + 1, x + 25, y + 25, SLOT_BG);
    }

    /** Render an item at reduced opacity (ghost silhouette). */
    private static void renderGhost(GuiGraphics gfx, ItemStack stack, int x, int y) {
        gfx.renderFakeItem(stack, x, y);
        // overlay semi-transparent slot bg → faded look
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gfx.fill(x, y, x + 16, y + 16, 0xA08B8B8B);
        RenderSystem.disableBlend();
    }
}
