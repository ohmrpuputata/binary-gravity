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
        // После победы (Мать Роя пала) интерфейс вторжения полностью скрыт.
        if (ClientInvasionState.victoryShown) {
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
        // ALWAYS-ON READOUT: with a geiger in the inventory the numbers are
        // simply on screen - no clicking. Background field + accumulated dose.
        if (hasGeiger) {
            float fieldBg = com.example.alieninvasion.logic.RadiationFieldManager.getFieldLevel(mc.player.getUUID());
            int bgDisplay = Math.round(fieldBg * 10.0F); // field units -> "uSv/h" feel
            String fieldColor = fieldBg >= 18.0F ? "§c" : fieldBg >= 9.0F ? "§e" : "§a";
            String doseColor = dose >= 80.0F ? "§c" : dose >= 45.0F ? "§6" : "§a";
            guiGraphics.drawString(mc.font,
                    fieldColor + "Фон: " + bgDisplay + " мкЗв/ч  "
                            + doseColor + "| Доза: " + (int) dose + " рад",
                    5, screenH - 40, 0xFFFFFFFF, true);
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

        float exactDose = (float) com.example.alieninvasion.logic.RadiationManager.getDose(mc.player);
        float exactInf = (float) com.example.alieninvasion.logic.InfectionManager.getMeter(mc.player);
        float fieldNow = com.example.alieninvasion.logic.RadiationFieldManager.getFieldLevel(mc.player.getUUID());
        renderSuitHud(guiGraphics, mc, screenH, exactDose, exactInf, fieldNow);

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
        } else if (day >= 2) {
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

    private static void renderSuitHud(GuiGraphics gui, Minecraft mc, int screenH,
            float dose, float infection, float field) {
        net.minecraft.world.item.Item[][] sets = {
                {
                        com.example.alieninvasion.registry.ItemRegistry.COSMIC_HELMET,
                        com.example.alieninvasion.registry.ItemRegistry.COSMIC_CHESTPLATE,
                        com.example.alieninvasion.registry.ItemRegistry.COSMIC_LEGGINGS,
                        com.example.alieninvasion.registry.ItemRegistry.COSMIC_BOOTS
                },
                {
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_HELMET,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_CHESTPLATE,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_LEGGINGS,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_BOOTS
                },
                {
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_HELMET,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_CHESTPLATE,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_LEGGINGS,
                        com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_BOOTS
                },
                {
                        com.example.alieninvasion.registry.ItemRegistry.PLATINUM_HELMET,
                        com.example.alieninvasion.registry.ItemRegistry.PLATINUM_CHESTPLATE,
                        com.example.alieninvasion.registry.ItemRegistry.PLATINUM_LEGGINGS,
                        com.example.alieninvasion.registry.ItemRegistry.PLATINUM_BOOTS
                },
                {
                        com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_HELMET,
                        com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_CHESTPLATE,
                        com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_LEGGINGS,
                        com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_BOOTS
                }
        };
        String[] names = {
                "COSMIC // ORBITAL",
                "HAZMAT // SEALED",
                "CHEM // BIO-LAB",
                "PLATINUM // AEGIS",
                "PALLADIUM // SENTINEL"
        };
        int[] colors = {0xFFC786FF, 0xFFFFC247, 0xFF8BFF75, 0xFFE8F3FF, 0xFF79E8D0};
        net.minecraft.world.entity.EquipmentSlot[] slots = {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        };

        int best = -1;
        int pieceCount = 0;
        for (int set = 0; set < sets.length; set++) {
            int count = 0;
            for (int piece = 0; piece < slots.length; piece++) {
                if (mc.player.getItemBySlot(slots[piece]).is(sets[set][piece])) count++;
            }
            if (count > pieceCount) {
                best = set;
                pieceCount = count;
            }
        }
        if (best < 0) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int color = colors[best];
        int panelX = 5;
        int panelY = Math.max(48, screenH - 137);
        int panelW = 194;
        int panelH = 59;
        int pulse = 35 + (int) ((Math.sin(mc.level.getGameTime() / 7.0D) + 1.0D) * 18.0D);
        int faint = (pulse << 24) | (color & 0x00FFFFFF);

        gui.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xB0060A0D);
        gui.fill(panelX, panelY, panelX + panelW, panelY + 1, color);
        gui.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, faint);
        gui.fill(panelX, panelY, panelX + 2, panelY + panelH, faint);
        gui.fill(panelX + panelW - 2, panelY, panelX + panelW, panelY + panelH, faint);

        int scanX = panelX + 2 + (int) ((mc.level.getGameTime() % 90L) * (panelW - 5) / 90.0D);
        gui.fill(scanX, panelY + 1, scanX + 1, panelY + panelH - 1, faint);
        gui.drawString(mc.font, names[best], panelX + 7, panelY + 5, color, true);
        gui.drawString(mc.font, pieceCount == 4 ? "LINK: STABLE" : "LINK: PARTIAL",
                panelX + 112, panelY + 5, pieceCount == 4 ? 0xFF8CFF9B : 0xFFFFB35C, true);

        for (int piece = 0; piece < slots.length; piece++) {
            int bx = panelX + 8 + piece * 18;
            boolean online = mc.player.getItemBySlot(slots[piece]).is(sets[best][piece]);
            gui.fill(bx, panelY + 18, bx + 13, panelY + 27, online ? color : 0xFF242B30);
            gui.fill(bx + 2, panelY + 20, bx + 11, panelY + 25, online ? 0xFF10171B : 0xFF111315);
            gui.drawString(mc.font, "HCLB".substring(piece, piece + 1),
                    bx + 4, panelY + 19, online ? color : 0xFF60686D, false);
        }
        gui.drawString(mc.font, "MODULES " + pieceCount + "/4", panelX + 84, panelY + 19,
                0xFFD7E2E8, false);

        int integrity = suitIntegrity(mc.player, sets[best], slots);
        drawTechBar(gui, panelX + 8, panelY + 34, 112, integrity / 100.0F, color);
        gui.drawString(mc.font, "INTEGRITY " + integrity + "%", panelX + 126, panelY + 32,
                integrity < 25 ? 0xFFFF5555 : 0xFFD7E2E8, false);

        int radColor = dose >= 70.0F ? 0xFFFF4A38 : 0xFFC9F45A;
        int infColor = infection >= 70.0F ? 0xFF7DFF46 : 0xFF62C992;
        gui.drawString(mc.font, String.format("RAD %03.0f | INF %03.0f | FIELD %02.0f", dose, infection, field),
                panelX + 8, panelY + 47, dose >= infection ? radColor : infColor, false);

        if (mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(sets[best][0])) {
            drawHelmetFrame(gui, screenW, screenH, color, faint);
        }
    }

    private static int suitIntegrity(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.item.Item[] set,
            net.minecraft.world.entity.EquipmentSlot[] slots) {
        float total = 0.0F;
        int present = 0;
        for (int i = 0; i < slots.length; i++) {
            net.minecraft.world.item.ItemStack stack = player.getItemBySlot(slots[i]);
            if (!stack.is(set[i])) continue;
            present++;
            total += stack.isDamageableItem()
                    ? 1.0F - stack.getDamageValue() / (float) Math.max(1, stack.getMaxDamage())
                    : 1.0F;
        }
        return present == 0 ? 0 : Math.round(total * 100.0F / present);
    }

    private static void drawTechBar(GuiGraphics gui, int x, int y, int width, float fraction, int color) {
        gui.fill(x, y, x + width, y + 6, 0xFF172025);
        int filled = Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) * width);
        for (int px = 0; px < filled; px += 5) {
            gui.fill(x + px, y + 1, x + Math.min(px + 3, filled), y + 5, color);
        }
    }

    private static void drawHelmetFrame(GuiGraphics gui, int width, int height, int color, int faint) {
        int arm = 24;
        gui.fill(3, 3, 3 + arm, 5, faint);
        gui.fill(3, 3, 5, 3 + arm, faint);
        gui.fill(width - 3 - arm, 3, width - 3, 5, faint);
        gui.fill(width - 5, 3, width - 3, 3 + arm, faint);
        gui.fill(3, height - 5, 3 + arm, height - 3, faint);
        gui.fill(3, height - 3 - arm, 5, height - 3, faint);
        gui.fill(width - 3 - arm, height - 5, width - 3, height - 3, faint);
        gui.fill(width - 5, height - 3 - arm, width - 3, height - 3, faint);
        // Центральный прицел НЕ рисуем: он накладывался на ванильный, и в шлеме
        // было видно сразу два прицела. Оставляем только уголки визора по краям.
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
