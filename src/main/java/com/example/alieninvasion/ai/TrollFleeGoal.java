package com.example.alieninvasion.ai;

import com.example.alieninvasion.entity.AlienTrollEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

// When the troll is carrying stolen loot, it sprints away from the nearest
// player to go stash it. Overrides normal combat movement.
public class TrollFleeGoal extends Goal {
    private final AlienTrollEntity troll;
    private final double speedModifier;
    private Player threat;

    public TrollFleeGoal(AlienTrollEntity troll, double speedModifier) {
        this.troll = troll;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.troll.hasLoot()) {
            return false;
        }
        this.threat = this.troll.level().getNearestPlayer(this.troll, 16.0D);
        return this.threat != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.troll.hasLoot() && this.threat != null && this.threat.isAlive()
                && !this.troll.getNavigation().isDone();
    }

    @Override
    public void start() {
        flee();
    }

    @Override
    public void tick() {
        if (this.troll.getNavigation().isDone()) {
            flee();
        }
    }

    private void flee() {
        Vec3 away = DefaultRandomPos.getPosAway(this.troll, 16, 7, this.threat.position());
        if (away != null) {
            this.troll.getNavigation().moveTo(away.x, away.y, away.z, this.speedModifier);
        }
    }
}
