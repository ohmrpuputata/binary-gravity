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

        // Помехи — вблизи источника: 1=лёгкие, 2=сильные (как старый телевизор)
        int glitchLevel = com.example.alieninvasion.logic.RadiationManager.SCREEN_GLITCH
                .getOrDefault(mc.player.getUUID(), 0);
        if (glitchLevel >= 1) {
            long tGlitch = level.getGameTime();
            int gww = mc.getWindow().getGuiScaledWidth();
            int gwh = mc.getWindow().getGuiScaledHeight();

            if (glitchLevel >= 2) {
                // Сильные помехи — полная TV-статика
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
            } else {
                // Лёгкие помехи — едва заметное мерцание яркости + 2-3 тонкие полосы
                java.util.Random rng = new java.util.Random(tGlitch / 3);
                int flicker = 4 + rng.nextInt(8);
                guiGraphics.fill(0, 0, gww, gwh, (flicker << 24) | 0xB8E600);

                java.util.Random lineRng = new java.util.Random(tGlitch / 5);
                for (int i = 0; i < 3; i++) {
                    int ly = lineRng.nextInt(gwh);
                    int la = 8 + lineRng.nextInt(18);
                    guiGraphics.fill(0, ly, gww, ly + 1, (la << 24) | 0xDDDDDD);
                }
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

        // Полоска Облучения (левый нижний угол). РЕАЛИЗМ: дозу не чувствуешь -
        // шкала видна ТОЛЬКО со счётчиком Гейгера в инвентаре ЛИБО в броне со
        // встроенным дозиметром (гермокостюм / платина / космический).
        boolean hasGeiger = mc.player.getInventory().contains(
                new net.minecraft.world.item.ItemStack(com.example.alieninvasion.registry.ItemRegistry.GEIGER_COUNTER))
                || fullSet(mc.player, com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_HELMET,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_CHESTPLATE,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_LEGGINGS,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_BOOTS)
                || fullSet(mc.player, com.example.alieninvasion.registry.ItemRegistry.PLATINUM_HELMET,
                        com.example.alieninvasion.registry.ItemRegistry.PLATINUM_CHESTPLATE,
                        com.example.alieninvasion.registry.ItemRegistry.PLATINUM_LEGGINGS,
                        com.example.alieninvasion.registry.ItemRegistry.PLATINUM_BOOTS)
                || fullSet(mc.player, com.example.alieninvasion.registry.ItemRegistry.COSMIC_HELMET,
                        com.example.alieninvasion.registry.ItemRegistry.COSMIC_CHESTPLATE,
                        com.example.alieninvasion.registry.ItemRegistry.COSMIC_LEGGINGS,
                        com.example.alieninvasion.registry.ItemRegistry.COSMIC_BOOTS);
        float dose = (float) com.example.alieninvasion.logic.RadiationManager.getDose(mc.player);
        if (dose <= 0.0F && hasRadiation) {
            var re = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
            int amp = re != null ? re.getAmplifier() : 0;
            dose = amp >= 2 ? 85.0F : amp == 1 ? 55.0F : 25.0F;
        }
        if (dose > 0.0F && hasGeiger) {
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

        // ТЕЛЕМЕТРИЯ КОСТЮМА: полный комплект брони превращает HUD в приборную
        // панель — каждый сет показывает СВОИ точные данные (встроенные датчики).
        String suitTitle = null;
        int suitColor = 0xFFFFFFFF;
        java.util.List<String> suitLines = new java.util.ArrayList<>();
        float exactDose = (float) com.example.alieninvasion.logic.RadiationManager.getDose(mc.player);
        float exactInf = (float) com.example.alieninvasion.logic.InfectionManager.getMeter(mc.player);
        float fieldNow = com.example.alieninvasion.logic.RadiationFieldManager.getFieldLevel(mc.player.getUUID());
        int stage = exactInf >= 75 ? 3 : exactInf >= 50 ? 2 : exactInf >= 25 ? 1 : 0;
        if (fullSet(mc.player, com.example.alieninvasion.registry.ItemRegistry.COSMIC_HELMET,
                com.example.alieninvasion.registry.ItemRegistry.COSMIC_CHESTPLATE,
                com.example.alieninvasion.registry.ItemRegistry.COSMIC_LEGGINGS,
                com.example.alieninvasion.registry.ItemRegistry.COSMIC_BOOTS)) {
            suitTitle = "КОСМИЧЕСКИЙ КОСТЮМ";
            suitColor = 0xFFC9A0FF;
            suitLines.add("§dПолный иммунитет: ☢ и ☣ нейтрализованы");
            suitLines.add(String.format("§7Фон поля: %.0f  |  Ночное зрение: ВКЛ", fieldNow));
        } else if (fullSet(mc.player, com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_HELMET,
                com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_CHESTPLATE,
                com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_LEGGINGS,
                com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_BOOTS)) {
            suitTitle = "ГЕРМОКОСТЮМ (дозиметр)";
            suitColor = 0xFFFFC94A;
            suitLines.add(String.format("§e☢ Доза: %.1f%%  |  Поле: %.0f ед.", exactDose, fieldNow));
            suitLines.add("§7Набор дозы и заражения ×0.33");
        } else if (fullSet(mc.player, com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_HELMET,
                com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_CHESTPLATE,
                com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_LEGGINGS,
                com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_BOOTS)) {
            suitTitle = "БИОКОСТЮМ (анализатор)";
            suitColor = 0xFF8FE06A;
            suitLines.add(String.format("§a☣ Заражение: %.1f%%  |  Стадия: %d/3", exactInf, stage));
            suitLines.add("§7Набор дозы и заражения ×0.2");
        } else if (fullSet(mc.player, com.example.alieninvasion.registry.ItemRegistry.PLATINUM_HELMET,
                com.example.alieninvasion.registry.ItemRegistry.PLATINUM_CHESTPLATE,
                com.example.alieninvasion.registry.ItemRegistry.PLATINUM_LEGGINGS,
                com.example.alieninvasion.registry.ItemRegistry.PLATINUM_BOOTS)) {
            suitTitle = "ПЛАТИНОВАЯ БРОНЯ";
            suitColor = 0xFFE3E6EE;
            suitLines.add(String.format("§f☢ Доза: %.1f%% (потолок 70%%)", exactDose));
        } else if (fullSet(mc.player, com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_HELMET,
                com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_CHESTPLATE,
                com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_LEGGINGS,
                com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_BOOTS)) {
            suitTitle = "ПАЛЛАДИЕВАЯ БРОНЯ";
            suitColor = 0xFF9FE6CF;
            suitLines.add(String.format("§b☣ Заражение: %.1f%% (потолок 70%%)", exactInf));
        }
        if (suitTitle != null) {
            int px = 5, py = screenH - 92 - suitLines.size() * 10;
            guiGraphics.fill(px - 2, py - 3, px + 150, py + 11 + suitLines.size() * 10, 0x90000000);
            guiGraphics.drawString(mc.font, suitTitle, px, py, suitColor, true);
            for (int i = 0; i < suitLines.size(); i++) {
                guiGraphics.drawString(mc.font, suitLines.get(i), px, py + 11 + i * 10, 0xFFFFFFFF, true);
            }
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

    private static boolean fullSet(net.minecraft.world.entity.player.Player p,
            net.minecraft.world.item.Item helmet, net.minecraft.world.item.Item chest,
            net.minecraft.world.item.Item legs, net.minecraft.world.item.Item boots) {
        return p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(helmet)
                && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(chest)
                && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS).is(legs)
                && p.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(boots);
    }
}
