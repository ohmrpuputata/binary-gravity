package com.example.alieninvasion.ai;

import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import java.util.EnumSet;

public class AlienAttackGoal extends Goal {
    private final Mob mob;
    private final double speedModifier;
    private int pathDelay;
    private BlockPos approxPos;
    private int searchTicks;

    public AlienAttackGoal(Mob mob, double speed) {
        this.mob = mob;
        this.speedModifier = speed;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) {
            Player player = this.mob.level().getNearestPlayer(this.mob, 128.0D);
            if (player != null && player.isAlive() && !player.isCreative() && !player.isSpectator()) {
                this.mob.setTarget(player);
                return true;
            }
            return false;
        }
        return true;
    }

    @Override
    public void start() {
        this.pathDelay = 0;
        this.searchTicks = 0;
        this.approxPos = null;
    }

    @Override
    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) return;

        this.mob.getLookControl().setLookAt(target, 30.0F, 30.0F);
        
        boolean isMarked = target.hasEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.MARKED));
        boolean hasLineOfSight = this.mob.getSensing().hasLineOfSight(target);
        double distSqr = this.mob.distanceToSqr(target);

        // If target is player and is marked, or we have line of sight and are close:
        if (isMarked || (hasLineOfSight && distSqr < 256.0D)) {
            // Path directly to the player's exact position
            if (--this.pathDelay <= 0) {
                this.pathDelay = 10 + this.mob.getRandom().nextInt(10);
                this.mob.getNavigation().moveTo(target, this.speedModifier);
            }
            
            // Try to hit the target if close
            if (distSqr < 4.0D) {
                this.mob.doHurtTarget(target);
                // Apply marked effect on hit
                if (target instanceof Player && !isMarked) {
                    target.addEffect(new MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.MARKED), 600, 0, false, true
                    ));
                    // Alert all allied aliens within 64 blocks
                    alertAllies(target);
                }
            }
        } else {
            // Path to approximate position to search (approx +-12 blocks of target)
            if (this.approxPos == null || ++this.searchTicks % 80 == 0 || this.mob.blockPosition().distSqr(this.approxPos) < 9.0D) {
                BlockPos targetPos = target.blockPosition();
                BlockPos mobPos = this.mob.blockPosition();
                double dx = targetPos.getX() - mobPos.getX();
                double dy = targetPos.getY() - mobPos.getY();
                double dz = targetPos.getZ() - mobPos.getZ();
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

                int ox = this.mob.getRandom().nextInt(25) - 12;
                int oy = this.mob.getRandom().nextInt(9) - 4;
                int oz = this.mob.getRandom().nextInt(25) - 12;

                if (dist > 24.0D) {
                    double scale = 24.0D / dist;
                    double tx = mobPos.getX() + dx * scale;
                    double ty = mobPos.getY() + dy * scale;
                    double tz = mobPos.getZ() + dz * scale;
                    this.approxPos = new BlockPos((int)tx + ox, (int)ty + oy, (int)tz + oz);
                } else {
                    this.approxPos = targetPos.offset(ox, oy, oz);
                }
                this.mob.getNavigation().moveTo(this.approxPos.getX(), this.approxPos.getY(), this.approxPos.getZ(), this.speedModifier);
            }
        }
    }

    private void alertAllies(LivingEntity target) {
        if (this.mob.level() instanceof ServerLevel sl) {
            sl.playSound(null, this.mob.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 1.5F, 1.3F);
            for (Mob ally : sl.getEntitiesOfClass(Mob.class, this.mob.getBoundingBox().inflate(64.0D),
                    e -> e != this.mob && com.example.alieninvasion.entity.AlienUtils.isAlliedTo(this.mob, e))) {
                ally.setTarget(target);
            }
        }
    }
}
