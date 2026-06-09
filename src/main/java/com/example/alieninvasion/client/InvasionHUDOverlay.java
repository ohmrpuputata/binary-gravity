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

        // Рендерим пульсирующие вены заражения на стадии 1
        boolean hasInfection = mc.player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
        if (hasInfection) {
            var effect = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
            if (effect != null && effect.getAmplifier() == 0) { // Стадия 1
                long ticks = level.getGameTime();
                float pulse = (float) (Math.sin(ticks / 8.0D) * 0.5D + 0.5D); // 0.0 to 1.0
                int alpha = (int) (pulse * 45); // до 45 прозрачности
                int color = (alpha << 24) | (0x00FF00 & 0x00FFFFFF); // зеленый
                
                int w = mc.getWindow().getGuiScaledWidth();
                int h = mc.getWindow().getGuiScaledHeight();
                
                // Рисуем рамку по краям экрана
                int thickness = 10;
                guiGraphics.fill(0, 0, w, thickness, color); // верх
                guiGraphics.fill(0, h - thickness, w, h, color); // низ
                guiGraphics.fill(0, thickness, thickness, h - thickness, color); // лево
                guiGraphics.fill(w - thickness, thickness, w, h - thickness, color); // право
            }
        }

        // Рендерим радиационную виньетку (жёлто-зелёное мерцание как счётчик Гейгера)
        boolean hasRadiation = mc.player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
        if (hasRadiation) {
            var radEffect = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
            int radAmplifier = radEffect != null ? radEffect.getAmplifier() : 0;
            long ticks = level.getGameTime();

            // Rapid flickering Geiger-counter pattern — сочетание трёх волн
            double flicker1 = Math.sin(ticks / 2.0D) * 0.5D + 0.5D;
            double flicker2 = Math.sin(ticks / 5.0D + 1.7D) * 0.5D + 0.5D;
            double flicker3 = Math.sin(ticks / 13.0D) * 0.5D + 0.5D;
            float pulse = (float) (flicker1 * 0.4D + flicker2 * 0.35D + flicker3 * 0.25D);

            int baseAlpha = 25 + radAmplifier * 20;
            int alpha = Math.min((int) (pulse * baseAlpha), 120);

            // Жёлто-зелёный радиоактивный цвет (R=184, G=230, B=0)
            int rCol = (int) (184 * pulse + 50 * (1.0 - pulse));
            int gCol = (int) (230 * pulse + 200 * (1.0 - pulse));
            int color = (alpha << 24) | ((rCol & 0xFF) << 16) | ((gCol & 0xFF) << 8);

            int rw = mc.getWindow().getGuiScaledWidth();
            int rh = mc.getWindow().getGuiScaledHeight();

            int thickness = 8 + radAmplifier * 4;
            guiGraphics.fill(0, 0, rw, thickness, color);
            guiGraphics.fill(0, rh - thickness, rw, rh, color);
            guiGraphics.fill(0, thickness, thickness, rh - thickness, color);
            guiGraphics.fill(rw - thickness, thickness, rw, rh - thickness, color);

            // Помехи-полосы при активном screen_glitch флаге
            boolean glitch = com.example.alieninvasion.logic.RadiationManager.SCREEN_GLITCH
                    .getOrDefault(mc.player.getUUID(), false);
            if (glitch) {
                java.util.Random flickerRng = new java.util.Random(ticks / 3);
                for (int i = 0; i < 2 + radAmplifier; i++) {
                    int barY = flickerRng.nextInt(rh);
                    int barH = 1 + flickerRng.nextInt(3);
                    int barAlpha = 10 + flickerRng.nextInt(30);
                    guiGraphics.fill(0, barY, rw, barY + barH, (barAlpha << 24) | 0xB8E600);
                }
            }

            // Индикатор ☢ РАДИАЦИЯ в углу экрана
            String radText = "\u2622 \u0420\u0410\u0414\u0418\u0410\u0426\u0418\u042F";
            int textAlpha = (int) (pulse * 255);
            int textColor = (textAlpha << 24) | 0xB8E600;
            guiGraphics.drawString(mc.font, radText, 5, rh - 15, textColor, true);
        }

        // Screen darkening at infection >= 75%
        float infDark = (float) com.example.alieninvasion.logic.InfectionManager.getMeter(mc.player);
        if (infDark >= 75.0F) {
            int darkAlpha = (int) ((infDark - 75.0F) / 25.0F * 180);
            darkAlpha = Math.min(180, darkAlpha);
            int darkColor = (darkAlpha << 24);
            int dw = mc.getWindow().getGuiScaledWidth();
            int dh = mc.getWindow().getGuiScaledHeight();
            guiGraphics.fill(0, 0, dw, dh, darkColor);
        }

        // Heavy-bleed red vignette: you're badly hurt and leaving a blood trail.
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

        // Dose bar (rad). Single-player reads the real dose from RadiationManager
        // (shared JVM); on a dedicated server it estimates from the effect level so
        // the bar still shows something.
        float dose = (float) com.example.alieninvasion.logic.RadiationManager.getDose(mc.player);
        if (dose <= 0.0F && hasRadiation) {
            var re = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
            int amp = re != null ? re.getAmplifier() : 0;
            dose = amp >= 2 ? 85.0F : amp == 1 ? 55.0F : 25.0F;
        }
        if (dose > 0.0F) {
            int rh2 = mc.getWindow().getGuiScaledHeight();
            int barX = 5, barY = rh2 - 30, barW = 80, barH = 6;
            float frac = Math.min(1.0F, dose / com.example.alieninvasion.logic.RadiationManager.MAX_DOSE);
            int fill = (int) (barW * frac);
            int barColor = dose >= 80 ? 0xFFFF3020 : dose >= 45 ? 0xFFFF8C30 : 0xFFB8E600;
            guiGraphics.fill(barX - 1, barY - 1, barX + barW + 1, barY + barH + 1, 0xFF0A0F0A);
            guiGraphics.fill(barX, barY, barX + fill, barY + barH, barColor);
            guiGraphics.drawString(mc.font, "☢ " + (int) dose + "%", barX, barY - 10, barColor, true);
        }

        // ☣ Infection scale (sits just above the dose bar). Single-player reads the
        // real meter from InfectionManager; otherwise estimates from the effect level.
        float inf = (float) com.example.alieninvasion.logic.InfectionManager.getMeter(mc.player);
        if (inf <= 0.0F && hasInfection) {
            var ie = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
            int amp = ie != null ? ie.getAmplifier() : 0;
            inf = amp >= 2 ? 92.0F : amp == 1 ? 62.0F : 30.0F;
        }
        if (inf > 0.0F) {
            int rh3 = mc.getWindow().getGuiScaledHeight();
            int bX = 5, bY = rh3 - 44, bW = 80, bH = 6;
            float frac = Math.min(1.0F, inf / com.example.alieninvasion.logic.InfectionManager.MAX);
            int fill = (int) (bW * frac);
            int col = inf >= 90 ? 0xFF3FB000 : inf >= 60 ? 0xFF6FC23A : 0xFF9AD46A;
            guiGraphics.fill(bX - 1, bY - 1, bX + bW + 1, bY + bH + 1, 0xFF0A0F0A);
            guiGraphics.fill(bX, bY, bX + fill, bY + bH, col);
            guiGraphics.drawString(mc.font, "☣ " + (int) inf + "%", bX, bY - 10, col, true);
        }

        // HUD вторжения рендерится вверху экрана по центру
        int w = mc.getWindow().getGuiScaledWidth();
        int centerX = w / 2;
        int y = 5;

        // Фон для HUD
        guiGraphics.fill(centerX - 90, y, centerX + 90, y + 36, 0x80000000); // 50% темный фон
        
        // Рисуем рамку
        guiGraphics.fill(centerX - 91, y, centerX - 90, y + 36, 0xFF5D8A00); // левая зеленая грань
        guiGraphics.fill(centerX + 90, y, centerX + 91, y + 36, 0xFF5D8A00); // правая зеленая грань
        guiGraphics.fill(centerX - 91, y - 1, centerX + 91, y, 0xFF5D8A00); // верхняя зеленая грань
        guiGraphics.fill(centerX - 91, y + 36, centerX + 91, y + 37, 0xFF5D8A00); // нижняя зеленая грань

        // Текст дня
        String dayText = "ВТОРЖЕНИЕ: ДЕНЬ " + day;
        guiGraphics.drawString(mc.font, dayText, centerX - mc.font.width(dayText) / 2, y + 3, 0xFFFFFF, true);

        // Уровень угрозы
        String threatText = "УГРОЗА: ";
        int threatColor = 0x55FF55; // зелёный разведка
        if (day >= 5) {
            threatText += "ТОТАЛЬНАЯ ВОЙНА";
            threatColor = 0xFF5555; // красный
        } else if (day >= 3) {
            threatText += "ШТУРМ";
            threatColor = 0xFFAA00; // оранжевый
        } else {
            threatText += "РАЗВЕДКА";
        }
        guiGraphics.drawString(mc.font, threatText, centerX - mc.font.width(threatText) / 2, y + 13, threatColor, true);

        // Ночной таймер или дневной индикатор
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

        // Дополнительные индикаторы (погода / ЭМП)
        int alertY = y + 42;
        // Bleeding / being-hunted alert.
        if (hpFrac < 0.5F) {
            boolean heavyBleed = hpFrac < 0.25F;
            String bleedText = heavyBleed ? "🩸 КРОВОТЕЧЕНИЕ — вас выслеживают!"
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
