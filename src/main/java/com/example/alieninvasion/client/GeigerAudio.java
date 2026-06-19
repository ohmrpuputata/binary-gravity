package com.example.alieninvasion.client;

import com.example.alieninvasion.logic.RadiationFieldManager;
import com.example.alieninvasion.logic.RadiationManager;
import com.example.alieninvasion.registry.ItemRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/**
 * Клиентский «треск» счётчика Гейгера. Щелчки случайны (Пуассон) с частотой,
 * растущей от фонового поля и накопленной дозы — как у настоящего прибора:
 * редкие тики на фоне, плотный треск в горячей зоне. Работает, пока счётчик
 * лежит в инвентаре. Последний тик щелчка выставляется для «блипа» стрелки в HUD.
 */
public final class GeigerAudio {
    private static final Random RNG = new Random();
    public static long lastClickTick = Long.MIN_VALUE;
    /** Локальный фон, посчитанный НА КЛИЕНТЕ (серверный LAST_FIELD сюда не синкается).
     *  Читает дозиметр-циферблат в HUD. Обновляется не каждый кадр (см. tick). */
    public static volatile float clientField = 0.0F;

    private GeigerAudio() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(GeigerAudio::tick);
    }

    private static void tick(Minecraft mc) {
        Player p = mc.player;
        if (p == null || mc.level == null) {
            return;
        }
        // Фон берём из СИНКНУТОГО серверного значения (RADIATION_FIELD) — это та же
        // реальная экспозиция, что копит дозу, плюс буря. Клиентский scanField раньше
        // завышал фон дальними блоками и не знал про бурю — отсюда «неправильный фон».
        clientField = p.getAttachedOrElse(com.example.alieninvasion.registry.ModAttachments.RADIATION_FIELD, 0);
        if (mc.isPaused() || !p.getInventory().contains(new ItemStack(ItemRegistry.GEIGER_COUNTER))) {
            return;
        }

        float field = clientField;
        float dose = (float) RadiationManager.getDose(p);
        // щелчков/сек: лёгкий фон всегда + вклад поля и дозы, с потолком против спама
        float cps = Math.min(20.0F, 0.7F + field * 0.6F + dose * 0.12F);

        float perTick = cps / 20.0F;             // ожидаемое число щелчков за игровой тик
        int rolls = 1 + (int) perTick;           // при высокой интенсивности >1 за тик
        float pEach = perTick / rolls;
        for (int i = 0; i < rolls; i++) {
            if (RNG.nextFloat() < pEach) {
                // Только визуальный «блип» стрелки/втулки дозиметра в HUD. Сам звук
                // щелчка проигрывает GeigerCounterItem на сервере (надёжный путь),
                // поэтому здесь звук НЕ играем — иначе был бы двойной щелчок.
                lastClickTick = mc.level.getGameTime();
            }
        }
    }
}
