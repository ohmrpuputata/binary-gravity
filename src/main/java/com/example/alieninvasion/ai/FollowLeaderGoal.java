package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.IAlienUnit;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import java.util.EnumSet;
import java.util.List;

/**
 * Юнит следует за ближайшим командиром (isAlienLeader() == true).
 * Если передан конкретный leaderClass — ищет только среди него;
 * если null — ищет любого лидера роя через IAlienUnit.
 */
public class FollowLeaderGoal extends Goal {
    private final Mob mob;
    private final Class<? extends Mob> leaderClass; // null = любой IAlienUnit-командир
    private Mob leader;
    private final double speedModifier;

    /** Следовать за командиром конкретного класса (обратная совместимость). */
    public FollowLeaderGoal(Mob mob, Class<? extends Mob> leaderClass, double speedModifier) {
        this.mob = mob;
        this.leaderClass = leaderClass;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /** Следовать за ближайшим командиром любого типа (role.isLeader()). */
    public FollowLeaderGoal(Mob mob, double speedModifier) {
        this(mob, null, speedModifier);
    }

    @Override
    public boolean canUse() {
        if (this.leader != null && this.leader.isAlive()) return true;

        if (leaderClass != null) {
            List<? extends Mob> leaders = mob.level().getEntitiesOfClass(
                    leaderClass, mob.getBoundingBox().inflate(15.0D));
            if (!leaders.isEmpty()) {
                this.leader = leaders.get(0);
                return true;
            }
        } else {
            List<Mob> candidates = mob.level().getEntitiesOfClass(
                    Mob.class, mob.getBoundingBox().inflate(20.0D),
                    e -> e != mob && e instanceof IAlienUnit u && u.isAlienLeader() && e.isAlive());
            if (!candidates.isEmpty()) {
                // предпочитаем командира с наивысшим приоритетом
                candidates.sort((a, b) -> {
                    int pa = (a instanceof IAlienUnit u) ? u.getAlienRole().getPriority() : 0;
                    int pb = (b instanceof IAlienUnit u) ? u.getAlienRole().getPriority() : 0;
                    return Integer.compare(pb, pa);
                });
                this.leader = candidates.get(0);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return leader != null && leader.isAlive() && mob.distanceToSqr(leader) < 256.0D;
    }

    @Override
    public void start() {}

    @Override
    public void tick() {
        if (leader != null) {
            if (mob.distanceToSqr(leader) > 10.0D) {
                mob.getNavigation().moveTo(leader, speedModifier);
            } else {
                mob.getNavigation().stop();
            }
        }
    }
}
