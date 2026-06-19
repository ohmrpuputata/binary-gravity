package com.example.alieninvasion.logic;

import net.minecraft.world.level.Level;

public class SurvivalManager {

    /**
     * The single apocalypse clock. On the server it is the max of the world-time day
     * and the InvasionManager stage day, so both natural time progression AND
     * "/invasion set N" drive every day-gated system (world contamination, EMP,
     * anomalies, ore corruption...) consistently. After the Swarm Mother falls the
     * clock freezes — the world stops rotting once the invasion is over.
     * On the client (HUD) only world time is available.
     */
    public static int getDay(Level level) {
        int worldDay = (int) (level.getDayTime() / 24000L);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            com.example.alieninvasion.world.InvasionManager manager =
                    com.example.alieninvasion.world.InvasionManager.get(serverLevel);
            if (manager.isVictoryAchieved()) {
                return manager.getInvasionDays();
            }
            return Math.max(worldDay, manager.getInvasionDays());
        }
        return worldDay;
    }

    /**
     * Кислотный дождь — это СОБЫТИЕ, а не любой дождь. Опасен (разъедает, заражает,
     * требует маску/укрытие) только когда мир уже отравлен: с Дня 2 вторжения ИЛИ во
     * время радиационной бури. До этого дождь обычный и безвредный.
     */
    public static boolean isAcidRain(Level level) {
        return level.isRaining()
                && (getDay(level) >= 2 || com.example.alieninvasion.logic.RadiationManager.isStormActive());
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
