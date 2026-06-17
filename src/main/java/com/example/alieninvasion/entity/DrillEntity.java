package com.example.alieninvasion.entity;

import com.example.alieninvasion.logic.AlienEvolution;
import com.example.alieninvasion.registry.EntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

// A burrowing drill pod dropped to reach players who hide deep underground. It
// chews straight down through terrain to the target depth, then breaches with a
// squad.
public class DrillEntity extends Entity {
    private int life;
    private int difficulty;
    private int targetY = Integer.MIN_VALUE;
    private int lastCarveY = Integer.MAX_VALUE;
    private int empTicks;

    // The drill is now a destructible machine: burst its casing (or freeze it with an
    // EMP first and then break it) to stop the breach before the squad ever arrives.
    private static final float MAX_HEALTH = 40.0F;
    private float health = MAX_HEALTH;
    private boolean breaching;

    public void setEmpTicks(int ticks) {
        this.empTicks = ticks;
    }

    public DrillEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public void setTargetY(int targetY) {
        this.targetY = targetY;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.life = tag.getInt("Life");
        this.difficulty = tag.getInt("Difficulty");
        this.targetY = tag.getInt("TargetY");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Life", this.life);
        tag.putInt("Difficulty", this.difficulty);
        tag.putInt("TargetY", this.targetY);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        // Machines don't drown, suffocate or take fall damage - but real weapon fire
        // (and its own breach blast is ignored via the breaching guard) chews the
        // casing apart. Killing it in the air cancels the breach entirely.
        if (this.level().isClientSide || this.isRemoved() || this.breaching || amount <= 0.0F
                || source.is(net.minecraft.world.damagesource.DamageTypes.DROWN)
                || source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)
                || source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            return false;
        }
        this.health -= amount;
        this.hurtMarked = true;
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.6, this.getZ(),
                    6, 0.3, 0.3, 0.3, 0.1);
            sl.playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                    this.getSoundSource(), 0.6F, 1.7F);
        }
        if (this.health <= 0.0F) {
            destroyByDamage();
        }
        return true;
    }

    private void destroyByDamage() {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(),
                    8, 0.5, 0.5, 0.5, 0.1);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(), this.getZ(),
                    24, 0.6, 0.6, 0.6, 0.05);
            sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                    this.getSoundSource(), 1.0F, 0.8F);
        }
        // Destroyed in the air: the breach never fires, so the squad never lands.
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(),
                    0, 0.02, 0);
            return;
        }

        if (this.getTags().contains("EmpActive")) {
            if (this.level() instanceof ServerLevel sl && this.tickCount % 5 == 0) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(), this.getZ(), 2, 0.2, 0.2, 0.2, 0.05);
            }
            if (this.empTicks <= 0) {
                this.empTicks = 160;
            }
            this.empTicks--;
            if (this.empTicks <= 0) {
                this.removeTag("EmpActive");
            }
            return;
        }

        // Manually descend (phasing through terrain it carves). Much faster than
        // before: the drill is a sudden, rare strike with a short counter window, not
        // a slow inevitability you can casually outrun.
        this.setPos(this.getX(), this.getY() - 0.7, this.getZ());
        int by = Mth.floor(this.getY());
        if (by != this.lastCarveY) {
            carve(by);
            this.lastCarveY = by;
            // Throttle the grind SFX so the faster descent doesn't machine-gun it.
            if (by % 3 == 0) {
                this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
                        this.getSoundSource(), 0.7F, 0.5F);
            }
        }
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY(), this.getZ(), 6, 0.3, 0.2, 0.3, 0.1);
        }

        this.life++;
        boolean reached = this.targetY != Integer.MIN_VALUE && this.getY() <= this.targetY + 1;
        if (reached || this.getY() < this.level().getMinBuildHeight() + 4 || this.life > 800) {
            breach();
        }
    }

    private void carve(int y) {
        int cx = this.getBlockX();
        int cz = this.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                BlockState s = this.level().getBlockState(p);
                if (!s.isAir() && s.getDestroySpeed(this.level(), p) >= 0) { // skip unbreakable (bedrock = -1)
                    this.level().destroyBlock(p, false);
                }
            }
        }
    }

    private void breach() {
        if (!(this.level() instanceof ServerLevel sl)) {
            this.discard();
            return;
        }
        this.breaching = true; // ignore our own breach blast so we don't "die" mid-breach
        sl.explode(this, this.getX(), this.getY(), this.getZ(), 1.5F, false, Level.ExplosionInteraction.BLOCK);

        net.minecraft.world.entity.player.Player nearestPlayer = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (net.minecraft.server.level.ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.isAlive() && !p.isCreative()) {
                double d = this.distanceToSqr(p);
                if (d < nearestDistSq) {
                    nearestDistSq = d;
                    nearestPlayer = p;
                }
            }
        }

        // Spawn 1 worker grunt
        AlienGruntEntity worker = EntityRegistry.ALIEN_GRUNT.create(sl);
        if (worker != null) {
            worker.setScavenger(true);
            spawnSquadMember(sl, worker);
        }

        // Spawn 2 normal grunts
        for (int i = 0; i < 2; i++) {
            AlienGruntEntity grunt = EntityRegistry.ALIEN_GRUNT.create(sl);
            if (grunt != null) {
                spawnSquadMember(sl, grunt);
                if (nearestPlayer != null) {
                    grunt.setTarget(nearestPlayer);
                }
            }
        }

        // Spawn 1 chicken
        AlienChickenEntity chicken = EntityRegistry.ALIEN_CHICKEN.create(sl);
        if (chicken != null) {
            spawnSquadMember(sl, chicken);
            if (nearestPlayer != null) {
                chicken.setTarget(nearestPlayer);
            }
        }

        // Spawn 1 breacher
        AlienBreacherEntity breacher = EntityRegistry.ALIEN_BREACHER.create(sl);
        if (breacher != null) {
            spawnSquadMember(sl, breacher);
            if (nearestPlayer != null) {
                breacher.setTarget(nearestPlayer);
            }
        }

        this.discard();
    }

    private void spawnSquadMember(ServerLevel sl, Mob mob) {
        mob.moveTo(this.getX() + (this.random.nextDouble() - 0.5) * 2.0, this.getY(),
                this.getZ() + (this.random.nextDouble() - 0.5) * 2.0, this.random.nextFloat() * 360F, 0);
        AlienEvolution.evolve(mob, this.difficulty);
        sl.addFreshEntity(mob);
    }
}
