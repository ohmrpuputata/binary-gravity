package com.example.alieninvasion.logic;

import net.minecraft.world.level.Level;

public class SurvivalManager {

    public static int getDay(Level level) {
        return (int) (level.getDayTime() / 24000L);
    }

    public static boolean isAlienInvasionActive(Level level) {
        // Invasion starts from the first night (approx 13000 ticks)
        return level.getDayTime() > 13000;
    }

    public static int getDifficultyLevel(Level level) {
        int day = getDay(level);
        return Math.min(day, 7); // Cap at 7
    }
}
