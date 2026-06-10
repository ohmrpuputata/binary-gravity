package com.example.alieninvasion.client;

import com.example.alieninvasion.logic.SurvivalManager;
import com.example.alieninvasion.registry.ModEffects;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

/**
 * Интерфейс вторжения (HUD):
 * Отображает текущий день, уровень угрозы (Разведка / Штурм / Тотальная война),
 * время до окончания ночи, статус кислотного дождя и ЭМП-бури.
 * Рендерит пульсирующую зеленую виньетку при первой стадии заражения.
 */
public class InvasionHUDOverlay implements HudRenderCallback {

    public static void register() {
        HudRenderCallback.EVENT.register(new InvasionHUDOverlay());
    }

    @Override
    public void onHudRender(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }

        Level level = mc.level;
        int day = SurvivalManager.getDay(level);

        // Пульсирующие зелёные вены заражения на стадии 1
        boolean hasInfection = mc.player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
        if (hasInfection) {
            var effect = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
            if (effect != null && effect.getAmplifier() == 0) {
                long ticks = level.getGameTime();
                float pulse = (float) (Math.sin(ticks / 8.0D) * 0.5D + 0.5D);
                int alpha = (int) (pulse * 45);
                int color = (alpha << 24) | (0x00FF00 & 0x00FFFFFF);

                int w = mc.getWindow().getGuiScaledWidth();
                int h = mc.getWindow().getGuiScaledHeight();

                int thickness = 10;
                guiGraphics.fill(0, 0, w, thickness, color);
                guiGraphics.fill(0, h - thickness, w, h, color);
                guiGraphics.fill(0, thickness, thickness, h - thickness, color);
                guiGraphics.fill(w - thickness, thickness, w, h - thickness, color);
            }
        }

        float doseHud = (float) com.example.alieninvasion.logic.RadiationManager.getDose(mc.player);
        boolean hasRadiation = doseHud > 0.0F
                || mc.player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION));

        // Помехи как старый телевизор — активны вблизи источника при стремительном росте (SCREEN_GLITCH)
        boolean glitch = com.example.alieninvasion.logic.RadiationManager.SCREEN_GLITCH
                .getOrDefault(mc.player.getUUID(), false);
        if (glitch) {
            long tGlitch = level.getGameTime();
            int gww = mc.getWindow().getGuiScaledWidth();
            int gwh = mc.getWindow().getGuiScaledHeight();

            java.util.Random baseRng = new java.util.Random(tGlitch);
            int noiseAlpha = 15 + baseRng.nextInt(30);
            guiGraphics.fill(0, 0, gww, gwh, (noiseAlpha << 24));

            java.util.Random lineRng = new java.util.Random(tGlitch / 2);
            for (int i = 0; i < 18; i++) {
                int barY = lineRng.nextInt(gwh);
                int barH = lineRng.nextInt(5) == 0 ? 2 + lineRng.nextInt(5) : 1;
                int barAlpha = 15 + lineRng.nextInt(70);
                int barCol = lineRng.nextInt(3) == 0 ? 0xDDDDDD : 0x111111;
                guiGraphics.fill(0, barY, gww, barY + barH, (barAlpha << 24) | barCol);
            }

            int scanY = (int) ((tGlitch % 40) * gwh / 40.0);
            guiGraphics.fill(0, scanY, gww, Math.min(scanY + 2, gwh), (50 << 24) | 0x888888);
            guiGraphics.fill(0, Math.max(0, scanY - 1), gww, scanY, (20 << 24) | 0xFFFFFF);

            java.util.Random flashRng = new java.util.Random(tGlitch / 4);
            if (flashRng.nextInt(12) == 0) {
                guiGraphics.fill(0, 0, gww, gwh, (18 << 24) | 0xFFFFFF);
            }

            java.util.Random shiftRng = new java.util.Random(tGlitch / 3 + 77);
            for (int i = 0; i < 3; i++) {
                int sy = shiftRng.nextInt(gwh);
                int sw = 20 + shiftRng.nextInt(gww / 3);
                int sx = shiftRng.nextInt(Math.max(1, gww - sw));
                int sa = 20 + shiftRng.nextInt(50);
                guiGraphics.fill(sx, sy, sx + sw, sy + 1, (sa << 24) | 0xB8E600);
            }
        }

        // Затемнение экрана при заражении >= 75%
        float infDark = (float) com.example.alieninvasion.logic.InfectionManager.getMeter(mc.player);
        if (infDark >= 75.0F) {
            int darkAlpha = Math.min(180, (int) ((infDark - 75.0F) / 25.0F * 180));
            int dw = mc.getWindow().getGuiScaledWidth();
            int dh = mc.getWindow().getGuiScaledHeight();
            guiGraphics.fill(0, 0, dw, dh, (darkAlpha << 24));
        }

        // Красная виньетка при тяжёлом ранении
        float hpFrac = mc.player.getHealth() / Math.max(1.0F, mc.player.getMaxHealth());
        if (hpFrac < 0.25F) {
            long bt = level.getGameTime();
            float pulse = (float) (Math.sin(bt / 6.0D) * 0.5D + 0.5D);
            int bleedAlpha = (int) (pulse * 60);
            int bleedCol = (bleedAlpha << 24) | 0x6E0000;
            int bw = mc.getWindow().getGuiScaledWidth();
            int bh = mc.getWindow().getGuiScaledHeight();
            int bth = 14;
            guiGraphics.fill(0, 0, bw, bth, bleedCol);
            guiGraphics.fill(0, bh - bth, bw, bh, bleedCol);
            guiGraphics.fill(0, bth, bth, bh - bth, bleedCol);
            guiGraphics.fill(bw - bth, bth, bw, bh - bth, bleedCol);
        }

        int screenH = mc.getWindow().getGuiScaledHeight();

        // Полоска Облучения (левый нижний угол)
        float dose = (float) com.example.alieninvasion.logic.RadiationManager.getDose(mc.player);
        if (dose <= 0.0F && hasRadiation) {
            var re = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
            int amp = re != null ? re.getAmplifier() : 0;
            dose = amp >= 2 ? 85.0F : amp == 1 ? 55.0F : 25.0F;
        }
        if (dose > 0.0F) {
            int barX = 5, barY = screenH - 28, barW = 102, barH = 9;
            float frac = Math.min(1.0F, dose / com.example.alieninvasion.logic.RadiationManager.MAX_DOSE);
            int fill = (int) (barW * frac);
            int barColor = dose >= 80 ? 0xFFFF3020 : dose >= 45 ? 0xFFFF8C30 : 0xFFB8E600;
            guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF0A0F0A);
            guiGraphics.fill(barX, barY, barX + fill, barY + barH, barColor);
            guiGraphics.drawString(mc.font, "☢ Облучение  " + (int) dose + "%", barX, barY - 10, barColor, true);
        }

        // Полоска Заражения (над полоской Облучения)
        float inf = (float) com.example.alieninvasion.logic.InfectionManager.getMeter(mc.player);
        if (inf <= 0.0F && hasInfection) {
            var ie = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
            int amp = ie != null ? ie.getAmplifier() : 0;
            inf = amp >= 2 ? 92.0F : amp == 1 ? 62.0F : 30.0F;
        }
        if (inf > 0.0F) {
            int bX = 5, bY = screenH - 58, bW = 102, bH = 9;
            float frac = Math.min(1.0F, inf / com.example.alieninvasion.logic.InfectionManager.MAX);
            int fill = (int) (bW * frac);
            int col = inf >= 90 ? 0xFF3FB000 : inf >= 60 ? 0xFF6FC23A : 0xFF9AD46A;
            guiGraphics.fill(bX - 1, bY - 1, bX + bW + 1, bY + bH + 1, 0xFF0A0F0A);
            guiGraphics.fill(bX, bY, bX + fill, bY + bH, col);
            guiGraphics.drawString(mc.font, "☣ Заражение  " + (int) inf + "%", bX, bY - 10, col, true);
        }

        // HUD вторжения (вверху по центру)
        int w = mc.getWindow().getGuiScaledWidth();
        int centerX = w / 2;
        int y = 5;

        guiGraphics.fill(centerX - 90, y, centerX + 90, y + 36, 0x80000000);
        guiGraphics.fill(centerX - 91, y, centerX - 90, y + 36, 0xFF5D8A00);
        guiGraphics.fill(centerX + 90, y, centerX + 91, y + 36, 0xFF5D8A00);
        guiGraphics.fill(centerX - 91, y - 1, centerX + 91, y, 0xFF5D8A00);
        guiGraphics.fill(centerX - 91, y + 36, centerX + 91, y + 37, 0xFF5D8A00);

        String dayText = "ВТОРЖЕНИЕ: ДЕНЬ " + day;
        guiGraphics.drawString(mc.font, dayText, centerX - mc.font.width(dayText) / 2, y + 3, 0xFFFFFF, true);

        String threatText = "УГРОЗА: ";
        int threatColor = 0x55FF55;
        if (day >= 5) {
            threatText += "ТОТАЛЬНАЯ ВОЙНА";
            threatColor = 0xFF5555;
        } else if (day >= 3) {
            threatText += "ШТУРМ";
            threatColor = 0xFFAA00;
        } else {
            threatText += "РАЗВЕДКА";
        }
        guiGraphics.drawString(mc.font, threatText, centerX - mc.font.width(threatText) / 2, y + 13, threatColor, true);

        if (!level.isDay()) {
            long nightTicksLeft = Math.max(0, 23000 - (level.getDayTime() % 24000));
            long totalSeconds = nightTicksLeft / 20;
            long minutes = totalSeconds / 60;
            long seconds = totalSeconds % 60;
            String timeText = String.format("До конца ночи: %02d:%02d", minutes, seconds);
            guiGraphics.drawString(mc.font, timeText, centerX - mc.font.width(timeText) / 2, y + 23, 0xFFFF55, true);
        } else {
            String timeText = "Идет подготовка (День)";
            guiGraphics.drawString(mc.font, timeText, centerX - mc.font.width(timeText) / 2, y + 23, 0x55FFFF, true);
        }

        int alertY = y + 42;
        if (hpFrac < 0.5F) {
            boolean heavyBleed = hpFrac < 0.25F;
            String bleedText = heavyBleed
                    ? "🩸 КРОВОТЕЧЕНИЕ — вас выслеживают!"
                    : "🩸 Вы ранены и оставляете следы крови";
            guiGraphics.drawString(mc.font, bleedText, centerX - mc.font.width(bleedText) / 2, alertY,
                    heavyBleed ? 0xFF3030 : 0xFF7878, true);
            alertY += 10;
        }
        if (com.example.alieninvasion.logic.RadiationManager.isStormActive()) {
            String stormText = "☢ РАДИАЦИОННАЯ БУРЯ";
            guiGraphics.drawString(mc.font, stormText, centerX - mc.font.width(stormText) / 2, alertY, 0xFFFF3020, true);
            alertY += 10;
        }
        if (level.isRaining()) {
            String rainText = "§c[!] ИДЕТ КИСЛОТНЫЙ ДОЖДЬ";
            guiGraphics.drawString(mc.font, rainText, centerX - mc.font.width(rainText) / 2, alertY, 0xFF5555, true);
            alertY += 10;
        }
        if (mc.player.getTags().contains("EmpActive")) {
            String empText = "§c[!] АКТИВЕН ЭМП-ИМПУЛЬС";
            guiGraphics.drawString(mc.font, empText, centerX - mc.font.width(empText) / 2, alertY, 0xFFAA00, true);
        }
    }
}
