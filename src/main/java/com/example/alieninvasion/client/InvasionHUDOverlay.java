package com.example.alieninvasion.client;

import com.example.alieninvasion.logic.SurvivalManager;
import com.example.alieninvasion.registry.ModEffects;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

/**
 * Интерфейс вторжения (HUD).
 *
 * Дизайн: «родная выживательная телеметрия» — выглядит как часть ванильного
 * Minecraft (пиксельный грид, фаски виджетов, шрифт с тенью, сдержанная палитра:
 * нейтральная тёмная рамка + один акцент на состояние), но показывает доп.
 * информацию: день/угроза вторжения, заражение, облучение, статус костюма и алерты.
 * Атмосферные оверлеи (виньетка заражения, ТВ-помехи от радиации, затемнение,
 * виньетка ранения) рендерятся поверх как полноэкранные эффекты.
 */
public class InvasionHUDOverlay implements HudRenderCallback {

    // --- палитра -------------------------------------------------------------
    private static final int BODY = 0xCC0B0D10;       // тёмное полупрозрачное тело панели
    private static final int TROUGH = 0xFF15171A;     // жёлоб полосы (утопленный)
    private static final int FRAME = 0xFF05060A;      // тёмная окантовка
    private static final int TEXT = 0xFFE8ECEF;       // основной текст
    private static final int TEXT_DIM = 0xFF9AA4AC;   // приглушённый текст

    // --- раскладка левой колонки (единая ширина + измеренные высоты) ---------
    private static final int COLUMN_W = 112;          // общая ширина чипа костюма и гейджей
    private static final int BLOCK_GAP = 3;           // вертикальный отступ между блоками
    private static final int GAUGE_H = 15;            // высота гейджа: подпись (8) + полоса (5)

    public static void register() {
        HudRenderCallback.EVENT.register(new InvasionHUDOverlay());
    }

    @Override
    public void onHudRender(GuiGraphics g, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (ClientInvasionState.victoryShown) {
            return;
        }

        Level level = mc.level;
        Font font = mc.font;
        int day = SurvivalManager.getDay(level);
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // ===================== АТМОСФЕРНЫЕ ОВЕРЛЕИ (без изменений) ===============
        boolean hasInfection = mc.player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
        if (hasInfection) {
            var effect = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
            if (effect != null && effect.getAmplifier() == 0) {
                long ticks = level.getGameTime();
                float pulse = (float) (Math.sin(ticks / 8.0D) * 0.5D + 0.5D);
                int alpha = (int) (pulse * 45);
                int color = (alpha << 24) | (0x00FF00 & 0x00FFFFFF);
                int thickness = 10;
                g.fill(0, 0, screenW, thickness, color);
                g.fill(0, screenH - thickness, screenW, screenH, color);
                g.fill(0, thickness, thickness, screenH - thickness, color);
                g.fill(screenW - thickness, thickness, screenW, screenH - thickness, color);
            }
        }

        float doseHud = (float) com.example.alieninvasion.logic.RadiationManager.getDose(mc.player);
        boolean hasRadiation = doseHud > 0.0F
                || mc.player.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION));

        int glitchLevel = com.example.alieninvasion.logic.RadiationManager.SCREEN_GLITCH
                .getOrDefault(mc.player.getUUID(), 0);
        if (glitchLevel >= 1) {
            long tGlitch = level.getGameTime();
            int gww = screenW;
            int gwh = screenH;
            if (glitchLevel >= 2) {
                java.util.Random baseRng = new java.util.Random(tGlitch);
                int noiseAlpha = 15 + baseRng.nextInt(30);
                g.fill(0, 0, gww, gwh, (noiseAlpha << 24));
                java.util.Random lineRng = new java.util.Random(tGlitch / 2);
                for (int i = 0; i < 18; i++) {
                    int barY = lineRng.nextInt(gwh);
                    int barH = lineRng.nextInt(5) == 0 ? 2 + lineRng.nextInt(5) : 1;
                    int barAlpha = 15 + lineRng.nextInt(70);
                    int barCol = lineRng.nextInt(3) == 0 ? 0xDDDDDD : 0x111111;
                    g.fill(0, barY, gww, barY + barH, (barAlpha << 24) | barCol);
                }
                int scanY = (int) ((tGlitch % 40) * gwh / 40.0);
                g.fill(0, scanY, gww, Math.min(scanY + 2, gwh), (50 << 24) | 0x888888);
                g.fill(0, Math.max(0, scanY - 1), gww, scanY, (20 << 24) | 0xFFFFFF);
                java.util.Random flashRng = new java.util.Random(tGlitch / 4);
                if (flashRng.nextInt(12) == 0) {
                    g.fill(0, 0, gww, gwh, (18 << 24) | 0xFFFFFF);
                }
                java.util.Random shiftRng = new java.util.Random(tGlitch / 3 + 77);
                for (int i = 0; i < 3; i++) {
                    int sy = shiftRng.nextInt(gwh);
                    int sw = 20 + shiftRng.nextInt(gww / 3);
                    int sx = shiftRng.nextInt(Math.max(1, gww - sw));
                    int sa = 20 + shiftRng.nextInt(50);
                    g.fill(sx, sy, sx + sw, sy + 1, (sa << 24) | 0xB8E600);
                }
            } else {
                java.util.Random rng = new java.util.Random(tGlitch / 3);
                int flicker = 4 + rng.nextInt(8);
                g.fill(0, 0, gww, gwh, (flicker << 24) | 0xB8E600);
                java.util.Random lineRng = new java.util.Random(tGlitch / 5);
                for (int i = 0; i < 3; i++) {
                    int ly = lineRng.nextInt(gwh);
                    int la = 8 + lineRng.nextInt(18);
                    g.fill(0, ly, gww, ly + 1, (la << 24) | 0xDDDDDD);
                }
            }
        }

        float infDark = (float) com.example.alieninvasion.logic.InfectionManager.getMeter(mc.player);
        if (infDark >= 75.0F) {
            int darkAlpha = Math.min(180, (int) ((infDark - 75.0F) / 25.0F * 180));
            g.fill(0, 0, screenW, screenH, (darkAlpha << 24));
        }

        float hpFrac = mc.player.getHealth() / Math.max(1.0F, mc.player.getMaxHealth());
        if (hpFrac < 0.25F) {
            long bt = level.getGameTime();
            float pulse = (float) (Math.sin(bt / 6.0D) * 0.5D + 0.5D);
            int bleedAlpha = (int) (pulse * 60);
            int bleedCol = (bleedAlpha << 24) | 0x6E0000;
            int bth = 14;
            g.fill(0, 0, screenW, bth, bleedCol);
            g.fill(0, screenH - bth, screenW, screenH, bleedCol);
            g.fill(0, bth, bth, screenH - bth, bleedCol);
            g.fill(screenW - bth, bth, screenW, screenH - bth, bleedCol);
        }

        // ===================== ДАННЫЕ (логика без изменений) =====================
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
        float inf = (float) com.example.alieninvasion.logic.InfectionManager.getMeter(mc.player);
        if (inf <= 0.0F && hasInfection) {
            var ie = mc.player.getEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
            int amp = ie != null ? ie.getAmplifier() : 0;
            inf = amp >= 2 ? 92.0F : amp == 1 ? 62.0F : 30.0F;
        }
        float field = GeigerAudio.clientField; // фон считается на клиенте (scanField), серверный не синкается

        // ===================== ЛЕВАЯ КОЛОНКА: костюм + гейджи ====================
        // Единый стек снизу вверх с измеренными высотами — блоки не налезают друг
        // на друга при любом наборе активных эффектов. Низ колонки поднимается над
        // ванильными барами (здоровье/броня/голод) и хотбаром, когда дотянулся бы до них.
        int colX = 6;
        boolean survivalBars = !mc.player.isCreative() && !mc.player.isSpectator()
                && !mc.player.isInvulnerable();
        int barsLeft = screenW / 2 - 91 - 4;           // левый край ванильных баров/хотбара
        int bottomAnchor = screenH - 6;
        if (colX + COLUMN_W > barsLeft) {              // колонка перекрыла бы бары — поднимаем над ними
            bottomAnchor = survivalBars ? screenH - 41 : screenH - 24;
        }

        int by = bottomAnchor;
        // Радиация (низ стека): фон + накопленная доза (зона опасности, живая скорость, тиры).
        by = drawRadiation(g, font, mc, colX, by, COLUMN_W, dose, field, hasGeiger);
        // Заражение.
        if (inf > 0.0F) {
            int infCol = inf >= 90 ? 0xFF4FBF2A : inf >= 60 ? 0xFF73C53E : 0xFF93D46A;
            String stage = inf >= 75 ? "§2черви" : inf >= 50 ? "§aлихорадка" : inf >= 25 ? "§7недомог." : "";
            String label = "ЗАРАЖЕНИЕ " + (int) inf + "%" + (stage.isEmpty() ? "" : " §8· " + stage);
            by -= BLOCK_GAP + GAUGE_H;
            drawGauge(g, font, colX, by, COLUMN_W, label, infCol,
                    inf / com.example.alieninvasion.logic.InfectionManager.MAX);
        }
        // Чип костюма (верх стека).
        by -= BLOCK_GAP;
        drawSuitChip(g, font, mc, colX, by);

        // ===================== ВЕРХНИЙ БАННЕР ВТОРЖЕНИЯ ==========================
        int threatCol;
        String threatName;
        if (day >= 5) {
            threatCol = 0xFFFF5A4A;
            threatName = "ТОТАЛЬНАЯ ВОЙНА";
        } else if (day >= 2) {
            threatCol = 0xFFFFB24A;
            threatName = "ШТУРМ";
        } else {
            threatCol = 0xFF8CD36A;
            threatName = "РАЗВЕДКА";
        }

        int bw = 178, bh = 34;
        int bx = (screenW - bw) / 2;
        int byTop = 4;
        drawPanel(g, bx, byTop, bw, bh, threatCol);

        // Заголовок: «ДЕНЬ N · <УГРОЗА>» по центру (день белый, угроза — акцент).
        String dayPart = "ДЕНЬ " + day + "  ·  ";
        int titleW = font.width(dayPart) + font.width(threatName);
        int tx = (screenW - titleW) / 2;
        g.drawString(font, dayPart, tx, byTop + 4, TEXT, true);
        g.drawString(font, threatName, tx + font.width(dayPart), byTop + 4, threatCol, true);

        // Сегментный прогресс к финалу (день N из 8): 8 ячеек.
        int segs = 8;
        int pbx = bx + 8, pby = byTop + 15, pbw = bw - 16, pbh = 4;
        int gap = 1;
        int cellW = (pbw - (segs - 1) * gap) / segs;
        for (int i = 0; i < segs; i++) {
            int cx = pbx + i * (cellW + gap);
            boolean filled = i < Math.min(day, segs);
            g.fill(cx, pby, cx + cellW, pby + pbh, FRAME);
            if (filled) {
                g.fill(cx, pby, cx + cellW, pby + pbh, threatCol);
                g.fill(cx, pby, cx + cellW, pby + 1, lighten(threatCol, 0.4f));
            } else {
                g.fill(cx + 1, pby + 1, cx + cellW - 1, pby + pbh - 1, TROUGH);
            }
        }

        // Таймер ночи / дня.
        String timeText;
        int timeCol;
        if (!level.isDay()) {
            long nightTicksLeft = Math.max(0, 23000 - (level.getDayTime() % 24000));
            long totalSeconds = nightTicksLeft / 20;
            timeText = String.format("рассвет %02d:%02d", totalSeconds / 60, totalSeconds % 60);
            timeCol = 0xFFFFE07A;
        } else {
            timeText = "день — затишье";
            timeCol = 0xFF8FC9D6;
        }
        g.drawString(font, timeText, bx + 8, byTop + 23, timeCol, true);
        int contam = Math.round(com.example.alieninvasion.logic.WorldContaminationManager.getTarget(day) * 100.0F);
        String cStr = "мир " + contam + "%";
        g.drawString(font, cStr, bx + bw - 8 - font.width(cStr), byTop + 23, 0xFFB98CFF, true);

        // ===================== АЛЕРТЫ (чипы под баннером) ========================
        int ay = byTop + bh + 4;
        if (hpFrac < 0.5F) {
            boolean heavy = hpFrac < 0.25F;
            ay = drawChip(g, font, screenW / 2, ay,
                    heavy ? 0xFFFF4040 : 0xFFFF8A8A,
                    heavy ? "КРОВОТЕЧЕНИЕ — вас выслеживают" : "ранение — вы оставляете кровь");
        }
        if (com.example.alieninvasion.logic.RadiationManager.isStormActive()) {
            ay = drawChip(g, font, screenW / 2, ay, 0xFFFF5A3A, "радиационная буря");
        }
        if (level.isRaining()) {
            ay = drawChip(g, font, screenW / 2, ay, 0xFF7ACBFF, "кислотный дождь");
        }
        if (mc.player.getTags().contains("EmpActive")) {
            ay = drawChip(g, font, screenW / 2, ay, 0xFFFFC247, "ЭМП-импульс активен");
        }
    }

    // ============================ КОМПОНЕНТЫ =================================

    /** Тёмная плашка с фаской: тело + светлый верх/лево, тёмный низ/право (как ванильные виджеты). */
    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h, int accent) {
        int top = blend(accent, FRAME, 0.45f);
        int bottom = darken(accent, 0.6f);
        g.fill(x, y, x + w, y + h, BODY);
        g.fill(x, y, x + w, y + 1, top);
        g.fill(x, y + h - 1, x + w, y + h, bottom);
        g.fill(x, y, x + 1, y + h, top);
        g.fill(x + w - 1, y, x + w, y + h, bottom);
    }

    /** Утопленный гейдж: подпись с пипом сверху, под ней бевел-полоса. y — верх элемента (h≈15). */
    private static void drawGauge(GuiGraphics g, Font font, int x, int y, int w,
            String label, int accent, float frac) {
        drawPip(g, x, y, accent);
        g.drawString(font, label, x + 8, y, TEXT, true);
        int barY = y + 10;
        int barH = 5;
        g.fill(x - 1, barY - 1, x + w + 1, barY + barH + 1, FRAME);
        g.fill(x, barY, x + w, barY + barH, TROUGH);
        int fw = Math.round(w * Math.max(0f, Math.min(1f, frac)));
        if (fw > 0) {
            g.fill(x, barY, x + fw, barY + barH, accent);
            g.fill(x, barY, x + fw, barY + 1, lighten(accent, 0.45f));
            g.fill(x, barY + barH - 1, x + fw, barY + barH, darken(accent, 0.4f));
        }
    }

    // --- точная радиация: трекинг скорости набора дозы (Δдозы/сек) клиентски ---
    private static float lastDose = -1.0F;
    private static long lastDoseTick = 0L;
    private static float doseRate = 0.0F;

    /**
     * Радиационный блок: фоновое поле (всегда при гейгере) + накопленная доза с
     * десятыми, ярлыком зоны опасности, ЖИВОЙ скоростью набора/спада и шкалой с
     * засечками тиров 25/50/75. Рисуется снизу вверх, возвращает новый курсор by.
     */
    private static int drawRadiation(GuiGraphics g, Font font, Minecraft mc, int x, int by, int w,
            float dose, float field, boolean hasGeiger) {
        if (!hasGeiger) {
            return by;
        }
        long nowT = mc.level.getGameTime();
        if (lastDose < 0.0F) {
            lastDose = dose;
            lastDoseTick = nowT;
        }
        long dtT = nowT - lastDoseTick;
        if (dtT >= 4) {
            doseRate = doseRate * 0.5F + ((dose - lastDose) * 20.0F / dtT) * 0.5F;
            lastDose = dose;
            lastDoseTick = nowT;
        }

        // Фоновое поле: аналоговый дозиметр-циферблат (дуга с зонами + стрелка + отсчёт).
        int fieldCol = field >= 18.0F ? 0xFFFF6A4A : field >= 9.0F ? 0xFFFFC247 : 0xFF8FD46A;
        by = drawGeigerDial(g, font, mc, x, by, field, fieldCol);
        by -= 2;

        // Накопленная доза: зона + скорость + бевел-шкала с тирами.
        if (dose > 0.0F) {
            int radCol = dose >= 75 ? 0xFFFF4A38 : dose >= 50 ? 0xFFFFA53A : 0xFFC8E84A;
            String zone = dose >= 100 ? "§4ЛЕТАЛ" : dose >= 75 ? "§cКРИТ" : dose >= 50 ? "§6ТЯЖ"
                    : dose >= 25 ? "§eОБЛУЧ" : "§aОК";
            String rate = Math.abs(doseRate) < 0.05F ? ""
                    : doseRate > 0 ? String.format(" §c+%.1f/с", doseRate)
                    : String.format(" §a%.1f/с", doseRate);
            by -= 15;
            drawPip(g, x, by, radCol);
            g.drawString(font, String.format("ДОЗА %.1f%% §7%s%s", dose, zone, rate), x + 8, by, TEXT, true);
            int barY = by + 10;
            int barH = 5;
            g.fill(x - 1, barY - 1, x + w + 1, barY + barH + 1, FRAME);
            g.fill(x, barY, x + w, barY + barH, TROUGH);
            int fw = Math.round(w * Math.min(1.0F, dose / 100.0F));
            if (fw > 0) {
                g.fill(x, barY, x + fw, barY + barH, radCol);
                g.fill(x, barY, x + fw, barY + 1, lighten(radCol, 0.45f));
                g.fill(x, barY + barH - 1, x + fw, barY + barH, darken(radCol, 0.4f));
            }
            for (int p : new int[]{25, 50, 75}) {
                int tx = x + Math.round(w * p / 100.0F);
                g.fill(tx, barY, tx + 1, barY + barH, 0x77000000);
            }
        }
        // Возвращаем верх блока; отступ до следующего блока добавляет вызывающий код.
        return by;
    }

    /**
     * Аналоговый циферблат дозиметра фонового поля: дуга с зонами (зелёный→жёлтый→
     * красный), стрелка к текущему значению, засечки, втулка и цифровой отсчёт справа.
     * «Блип»: стрелка/втулка ярче пару тиков после каждого щелчка счётчика. Рисуется
     * вверх от bottomY, возвращает верхнюю кромку (новый курсор стека).
     */
    private static int drawGeigerDial(GuiGraphics g, Font font, Minecraft mc, int x, int bottomY,
            float field, int col) {
        final int R = 19;
        final float MAX = 25.0F;
        int px = x + R + 1;           // ось стрелки (центр-низ дуги)
        int py = bottomY - 1;
        float frac = Math.max(0f, Math.min(1f, field / MAX));

        // Дуга: верхний полукруг. Пройденная часть — яркая зона, остальное приглушено.
        for (int i = 0; i <= 64; i++) {
            double tt = i / 64.0;
            double a = Math.PI * (1.0 - tt);
            float fv = (float) (tt * MAX);
            int zc = fv >= 18 ? 0xFFFF6A4A : fv >= 9 ? 0xFFFFC247 : 0xFF8FD46A;
            int ax = px + (int) Math.round(R * Math.cos(a));
            int ay = py - (int) Math.round(R * Math.sin(a));
            int c = tt <= frac ? zc : darken(zc, 0.6f);
            g.fill(ax - 1, ay - 1, ax + 1, ay + 1, c);
        }
        // Засечки 0 / ¼ / ½ / ¾ / max.
        for (int k = 0; k <= 4; k++) {
            double a = Math.PI * (1.0 - k / 4.0);
            int ix = px + (int) Math.round((R - 4) * Math.cos(a));
            int iy = py - (int) Math.round((R - 4) * Math.sin(a));
            g.fill(ix, iy, ix + 1, iy + 1, FRAME);
        }
        // Стрелка к текущему значению (+«блип» на свежем щелчке).
        boolean blip = mc.level != null
                && mc.level.getGameTime() - GeigerAudio.lastClickTick <= 2L;
        double a = Math.PI * (1.0 - frac);
        int nx = px + (int) Math.round((R - 2) * Math.cos(a));
        int ny = py - (int) Math.round((R - 2) * Math.sin(a));
        drawThinLine(g, px, py, nx, ny, blip ? 0xFFFFFFFF : 0xFFD8DEE3);
        // Втулка.
        g.fill(px - 2, py - 2, px + 3, py + 1, FRAME);
        g.fill(px - 1, py - 1, px + 2, py, blip ? lighten(col, 0.5f) : col);

        // Цифровой отсчёт справа от циферблата.
        int tx = px + R + 6;
        int ty = py - R + 3;
        g.drawString(font, "ФОН", tx, ty, TEXT_DIM, true);
        g.drawString(font, (int) field + " мкЗв/ч", tx, ty + 9, col, true);
        return py - R - 1;
    }

    /** «Линия» из 1px-квадратиков — у GuiGraphics нет нативного примитива линии. */
    private static void drawThinLine(GuiGraphics g, int x0, int y0, int x1, int y1, int col) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps == 0) {
            g.fill(x0, y0, x0 + 1, y0 + 1, col);
            return;
        }
        for (int i = 0; i <= steps; i++) {
            int xx = x0 + (x1 - x0) * i / steps;
            int yy = y0 + (y1 - y0) * i / steps;
            g.fill(xx, yy, xx + 1, yy + 1, col);
        }
    }

    /** Маленький бевел-пип (цветовой ключ состояния) 6x6. */
    private static void drawPip(GuiGraphics g, int x, int y, int accent) {
        g.fill(x, y, x + 6, y + 6, FRAME);
        g.fill(x + 1, y + 1, x + 5, y + 5, accent);
        g.fill(x + 1, y + 1, x + 4, y + 2, lighten(accent, 0.45f));
        g.fill(x + 4, y + 2, x + 5, y + 5, darken(accent, 0.4f));
    }

    /** Центрированный алерт-чип: плашка + пип + текст. Возвращает Y следующей строки. */
    private static int drawChip(GuiGraphics g, Font font, int centerX, int y, int accent, String text) {
        int tw = font.width(text);
        int w = tw + 16;
        int x = centerX - w / 2;
        drawPanel(g, x, y, w, 12, accent);
        drawPip(g, x + 4, y + 3, accent);
        g.drawString(font, text, x + 12, y + 2, TEXT, true);
        return y + 14;
    }

    /** Компактный чип костюма: бренд + куски + целостность + бонус сета + сенсор (в шлеме). */
    private static int drawSuitChip(GuiGraphics g, Font font, Minecraft mc, int x, int bottomY) {
        Level level = mc.level;
        net.minecraft.world.item.Item[][] sets = {
                {ItemReg.COSMIC_HELMET, ItemReg.COSMIC_CHESTPLATE, ItemReg.COSMIC_LEGGINGS, ItemReg.COSMIC_BOOTS},
                {ItemReg.ASTRAL_HELMET, ItemReg.ASTRAL_CHESTPLATE, ItemReg.ASTRAL_LEGGINGS, ItemReg.ASTRAL_BOOTS},
                {ItemReg.EMERADIUM_HELMET, ItemReg.EMERADIUM_CHESTPLATE, ItemReg.EMERADIUM_LEGGINGS, ItemReg.EMERADIUM_BOOTS},
                {ItemReg.ALIEN_HAZMAT_HELMET, ItemReg.ALIEN_HAZMAT_CHESTPLATE, ItemReg.ALIEN_HAZMAT_LEGGINGS, ItemReg.ALIEN_HAZMAT_BOOTS},
                {ItemReg.ALIEN_CHEM_HELMET, ItemReg.ALIEN_CHEM_CHESTPLATE, ItemReg.ALIEN_CHEM_LEGGINGS, ItemReg.ALIEN_CHEM_BOOTS},
                {ItemReg.PLATINUM_HELMET, ItemReg.PLATINUM_CHESTPLATE, ItemReg.PLATINUM_LEGGINGS, ItemReg.PLATINUM_BOOTS},
                {ItemReg.PALLADIUM_HELMET, ItemReg.PALLADIUM_CHESTPLATE, ItemReg.PALLADIUM_LEGGINGS, ItemReg.PALLADIUM_BOOTS}
        };
        String[] names = {"КОСМИЧЕСКИЙ", "АСТРАЛ-ПРИЗМА", "ЭМЕРАДИЙ", "ГЕРМОКОСТЮМ", "ХИМКОСТЮМ", "ПЛАТИНА", "ПАЛЛАДИЙ"};
        int[] colors = {0xFFC786FF, 0xFF72E7FF, 0xFF7DF0A0, 0xFFFFC247, 0xFF8BFF75, 0xFFE8F3FF, 0xFF79E8D0};
        // Коротко, чтобы строка бонуса умещалась в ширину чипа и не вылезала за рамку.
        String[] bonus = {"иммунитет к рад.", "имм. яд · щит", "лечит облуч·рад½",
                "рад/зараза ×⅓", "рад/зараза ×⅕", "радиация ×½", "зараза ×½"};
        net.minecraft.world.entity.EquipmentSlot[] slots = {
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET
        };

        int best = -1, pieceCount = 0;
        for (int set = 0; set < sets.length; set++) {
            int count = 0;
            for (int piece = 0; piece < slots.length; piece++) {
                if (com.example.alieninvasion.logic.ArmorProtection.hasCompatibleArmorPiece(
                        mc.player, slots[piece], sets[set][piece])) count++;
            }
            if (count > pieceCount) {
                best = set;
                pieceCount = count;
            }
        }
        if (best < 0) return bottomY;

        int color = colors[best];
        boolean full = pieceCount == 4;
        // Сенсор работает от визора шлема «крутого» костюма.
        boolean sensor = com.example.alieninvasion.logic.ArmorProtection.hasCompatibleArmorPiece(
                mc.player, net.minecraft.world.entity.EquipmentSlot.HEAD, sets[best][0]);
        int w = COLUMN_W;
        int h = 23 + (full ? 9 : 0) + (sensor ? 18 : 0);
        int y = bottomY - h;
        drawPanel(g, x, y, w, h, color);

        int row = y + 4;
        g.drawString(font, names[best], x + 6, row, color, true);
        String frac = pieceCount + "/4";
        g.drawString(font, frac, x + w - 6 - font.width(frac), row, full ? 0xFF8CFF9B : 0xFFFFB35C, true);
        row += 11;

        int integrity = suitIntegrity(mc.player, sets[best], slots);
        int intCol = integrity < 25 ? 0xFFFF5555 : color;
        int barX = x + 6, barW = w - 12, barH = 4;
        g.fill(barX - 1, row - 1, barX + barW + 1, row + barH + 1, FRAME);
        g.fill(barX, row, barX + barW, row + barH, TROUGH);
        int fw = Math.round(barW * (integrity / 100.0f));
        if (fw > 0) {
            g.fill(barX, row, barX + fw, row + barH, intCol);
            g.fill(barX, row, barX + fw, row + 1, lighten(intCol, 0.45f));
        }
        row += 8;

        if (full) {
            g.drawString(font, bonus[best], x + 6, row, 0xFFB7E0C2, true);
            row += 9;
        }

        // --- Сенсорный модуль (РОЙ / погода / температура / биом) ---
        if (sensor) {
            int aliens = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    mc.player.getBoundingBox().inflate(32.0),
                    e -> com.example.alieninvasion.entity.AlienUtils.isAlliedTo(null, e)).size();
            String weather;
            int wCol;
            if (com.example.alieninvasion.logic.RadiationManager.isStormActive()) { weather = "рад.буря"; wCol = 0xFFFF6A50; }
            else if (level.isThundering()) { weather = "гроза"; wCol = 0xFFB0B8FF; }
            else if (level.isRaining()) { weather = "кисл.дождь"; wCol = 0xFFFF8080; }
            else { weather = "ясно"; wCol = 0xFF9FE8FF; }
            g.drawString(font, "РОЙ: " + aliens, x + 6, row, aliens > 0 ? 0xFFFF8A6A : 0xFF8FE08F, true);
            g.drawString(font, weather, x + w - 6 - font.width(weather), row, wCol, true);
            row += 9;

            float baseTemp = level.getBiome(mc.player.blockPosition()).value().getBaseTemperature();
            int celsius = Math.round(baseTemp * 22.0F - 4.0F) - (level.isRaining() ? 4 : 0);
            String tempStr = "ТЕМП " + (celsius >= 0 ? "+" : "") + celsius + "°C";
            int tCol = celsius <= 0 ? 0xFF8FC8FF : celsius >= 30 ? 0xFFFF9A5A : 0xFFCAD4DA;
            g.drawString(font, tempStr, x + 6, row, tCol, true);
            String biome = biomeShort(level, mc.player.blockPosition());
            g.drawString(font, biome, x + w - 6 - font.width(biome), row, TEXT_DIM, true);
        }
        return h;
    }

    private static String biomeShort(Level level, net.minecraft.core.BlockPos pos) {
        var key = level.getBiome(pos).unwrapKey();
        if (key.isEmpty()) return "—";
        String p = key.get().location().getPath();
        return p.length() > 13 ? p.substring(0, 13) : p;
    }

    private static int suitIntegrity(net.minecraft.world.entity.player.Player player,
            net.minecraft.world.item.Item[] set,
            net.minecraft.world.entity.EquipmentSlot[] slots) {
        float total = 0.0F;
        int present = 0;
        for (int i = 0; i < slots.length; i++) {
            net.minecraft.world.item.ItemStack stack = player.getItemBySlot(slots[i]);
            if (!com.example.alieninvasion.logic.ArmorProtection.hasCompatibleArmorPiece(player, slots[i], set[i])) continue;
            present++;
            total += stack.isDamageableItem()
                    ? 1.0F - stack.getDamageValue() / (float) Math.max(1, stack.getMaxDamage())
                    : 1.0F;
        }
        return present == 0 ? 0 : Math.round(total * 100.0F / present);
    }

    private static boolean fullSet(net.minecraft.world.entity.player.Player p,
            net.minecraft.world.item.Item helmet, net.minecraft.world.item.Item chest,
            net.minecraft.world.item.Item legs, net.minecraft.world.item.Item boots) {
        return com.example.alieninvasion.logic.ArmorProtection.hasCompatibleSet(p, helmet, chest, legs, boots);
    }

    // ----- цветовые утилиты (ARGB) -----
    private static int lighten(int argb, float t) {
        return blend(argb, 0xFFFFFFFF, t);
    }

    private static int darken(int argb, float t) {
        return blend(argb, 0xFF000000, t);
    }

    private static int blend(int a, int b, float t) {
        int aa = (a >>> 24) & 0xFF, ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t);
        int gg = Math.round(ag + (bg - ag) * t);
        int bl = Math.round(ab + (bb - ab) * t);
        return (aa << 24) | (r << 16) | (gg << 8) | bl;
    }

    private static final class ItemReg {
        static final net.minecraft.world.item.Item COSMIC_HELMET = com.example.alieninvasion.registry.ItemRegistry.COSMIC_HELMET;
        static final net.minecraft.world.item.Item COSMIC_CHESTPLATE = com.example.alieninvasion.registry.ItemRegistry.COSMIC_CHESTPLATE;
        static final net.minecraft.world.item.Item COSMIC_LEGGINGS = com.example.alieninvasion.registry.ItemRegistry.COSMIC_LEGGINGS;
        static final net.minecraft.world.item.Item COSMIC_BOOTS = com.example.alieninvasion.registry.ItemRegistry.COSMIC_BOOTS;
        static final net.minecraft.world.item.Item ALIEN_HAZMAT_HELMET = com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_HELMET;
        static final net.minecraft.world.item.Item ALIEN_HAZMAT_CHESTPLATE = com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_CHESTPLATE;
        static final net.minecraft.world.item.Item ALIEN_HAZMAT_LEGGINGS = com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_LEGGINGS;
        static final net.minecraft.world.item.Item ALIEN_HAZMAT_BOOTS = com.example.alieninvasion.registry.ItemRegistry.ALIEN_HAZMAT_BOOTS;
        static final net.minecraft.world.item.Item ALIEN_CHEM_HELMET = com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_HELMET;
        static final net.minecraft.world.item.Item ALIEN_CHEM_CHESTPLATE = com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_CHESTPLATE;
        static final net.minecraft.world.item.Item ALIEN_CHEM_LEGGINGS = com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_LEGGINGS;
        static final net.minecraft.world.item.Item ALIEN_CHEM_BOOTS = com.example.alieninvasion.registry.ItemRegistry.ALIEN_CHEM_BOOTS;
        static final net.minecraft.world.item.Item PLATINUM_HELMET = com.example.alieninvasion.registry.ItemRegistry.PLATINUM_HELMET;
        static final net.minecraft.world.item.Item PLATINUM_CHESTPLATE = com.example.alieninvasion.registry.ItemRegistry.PLATINUM_CHESTPLATE;
        static final net.minecraft.world.item.Item PLATINUM_LEGGINGS = com.example.alieninvasion.registry.ItemRegistry.PLATINUM_LEGGINGS;
        static final net.minecraft.world.item.Item PLATINUM_BOOTS = com.example.alieninvasion.registry.ItemRegistry.PLATINUM_BOOTS;
        static final net.minecraft.world.item.Item PALLADIUM_HELMET = com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_HELMET;
        static final net.minecraft.world.item.Item PALLADIUM_CHESTPLATE = com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_CHESTPLATE;
        static final net.minecraft.world.item.Item PALLADIUM_LEGGINGS = com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_LEGGINGS;
        static final net.minecraft.world.item.Item PALLADIUM_BOOTS = com.example.alieninvasion.registry.ItemRegistry.PALLADIUM_BOOTS;
        static final net.minecraft.world.item.Item ASTRAL_HELMET = com.example.alieninvasion.registry.ItemRegistry.ASTRAL_PRISM_HELMET;
        static final net.minecraft.world.item.Item ASTRAL_CHESTPLATE = com.example.alieninvasion.registry.ItemRegistry.ASTRAL_PRISM_CHESTPLATE;
        static final net.minecraft.world.item.Item ASTRAL_LEGGINGS = com.example.alieninvasion.registry.ItemRegistry.ASTRAL_PRISM_LEGGINGS;
        static final net.minecraft.world.item.Item ASTRAL_BOOTS = com.example.alieninvasion.registry.ItemRegistry.ASTRAL_PRISM_BOOTS;
        static final net.minecraft.world.item.Item EMERADIUM_HELMET = com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_HELMET;
        static final net.minecraft.world.item.Item EMERADIUM_CHESTPLATE = com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_CHESTPLATE;
        static final net.minecraft.world.item.Item EMERADIUM_LEGGINGS = com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_LEGGINGS;
        static final net.minecraft.world.item.Item EMERADIUM_BOOTS = com.example.alieninvasion.registry.ItemRegistry.EMERADIUM_BOOTS;
    }
}
