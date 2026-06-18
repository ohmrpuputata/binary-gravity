package com.example.alieninvasion.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Screenshot;

/**
 * Dev-only screenshot harness. Inert unless "ai_preview.flag" exists in the client run
 * directory. Once the player reaches the preview stage (Y > 110) it switches to a
 * front third-person camera (to show the worn armor + emissive glow) and grabs a
 * burst of screenshots into run/screenshots/.
 */
public final class PreviewHarness {
    private static boolean active;
    private static boolean checked;
    private static boolean staged;
    private static int stagedTicks;
    private static int shots;
    private static final int MAX_SHOTS = 4;

    private PreviewHarness() {}

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.level == null || mc.player == null) {
                return;
            }
            if (!checked) {
                checked = true;
                active = new java.io.File("ai_preview.flag").exists();
            }
            if (!active || shots >= MAX_SHOTS) {
                return;
            }
            if (!staged) {
                if (mc.player.getY() > 110.0) {
                    staged = true;
                    stagedTicks = 0;
                }
                return;
            }
            stagedTicks++;
            if (stagedTicks == 30) {
                try {
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                } catch (Throwable ignored) {
                }
            }
            if (stagedTicks >= 50 && (stagedTicks - 50) % 25 == 0) {
                try {
                    Screenshot.grab(mc.gameDirectory, "ai_preview_" + shots + ".png",
                            mc.getMainRenderTarget(), c -> {});
                } catch (Throwable ignored) {
                }
                shots++;
            }
        });
    }
}
