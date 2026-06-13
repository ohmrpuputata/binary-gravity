package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.world.InvasionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Сердце финального события: 1:40 (2000 тиков) до детонации планеты Роя.
 * Рой волнами лезет грызть реактор (прочность 100) — его нужно отбивать,
 * а перед самым взрывом — успеть удрать в портал.
 */
public class PlanetReactorBlockEntity extends BlockEntity {
    public static final int FUSE_TICKS = 2000; // 1:40
    public static final int MAX_INTEGRITY = 100;
    /** Последние 10 секунд: оборона окончена, только бегство. */
    public static final int NO_RETURN_TICKS = 200;

    private boolean armed = false;
    private boolean detonated = false;
    private int ticksLeft = FUSE_TICKS;
    private int integrity = MAX_INTEGRITY;

    private final ServerBossEvent bar = new ServerBossEvent(
            Component.literal("РЕАКТОР"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    public PlanetReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.PLANET_REACTOR_BLOCK_ENTITY, pos, state);
    }

    public void arm() {
        this.armed = true;
        this.setChanged();
        if (this.level instanceof ServerLevel sl) {
            // КРИТИЧНО: чанки реактора форсируются, иначе побег последнего игрока
            // в портал выгружал чанк, отсчёт замирал и планета не взрывалась.
            setChunksForced(sl, true);
            sl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 3.0F, 0.5F);
            sl.playSound(null, worldPosition, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.6F);
            AlienUtils.broadcastTitle(sl,
                    Component.literal("§4РЕАКТОР АКТИВИРОВАН"),
                    Component.literal("§c1:40 до детонации. Отбивайте его и бегите к порталу!"));
            sl.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                    "§6[Макс Максбетов] §fПошёл отсчёт, бомжи! Минута сорок! Держите рой подальше от моей малышки!"), false);
            summonMaxDefender(sl);
        }
    }

    /**
     * КО-ОП ДЛЯ КАЗУАЛОВ: Макс приходит через разрыв и прикрывает реактор — рвёт
     * волны роя, так что отбить детонацию сможет даже новичок. Спавним одного.
     */
    private void summonMaxDefender(ServerLevel sl) {
        boolean alreadyHere = !sl.getEntitiesOfClass(com.example.alieninvasion.entity.HunterEntity.class,
                new AABB(worldPosition).inflate(28.0D)).isEmpty();
        if (alreadyHere) {
            return;
        }
        com.example.alieninvasion.entity.HunterEntity max = EntityRegistry.HUNTER.create(sl);
        if (max == null) {
            return;
        }
        int hy = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, worldPosition.getX() + 2, worldPosition.getZ() + 2);
        max.moveTo(worldPosition.getX() + 2.5D, hy, worldPosition.getZ() + 2.5D, 180.0F, 0.0F);
        max.setupAsReactorDefender();
        max.setPersistenceRequired();
        sl.addFreshEntity(max);
        net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
        if (bolt != null) {
            bolt.moveTo(max.getX(), max.getY(), max.getZ());
            bolt.setVisualOnly(true);
            sl.addFreshEntity(bolt);
        }
        sl.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "§6[Макс Максбетов] §fЛадно, бомжи, прикрою. Держитесь рядом — со мной не пропадёте."), false);
    }

    public String statusLine() {
        if (!armed) return "§7[Реактор] Не активирован.";
        return "§c[Реактор] До детонации: " + format(ticksLeft) + " §7| Прочность: " + integrity + "%";
    }

    private static String format(int ticks) {
        int totalSec = ticks / 20;
        return (totalSec / 60) + ":" + String.format("%02d", totalSec % 60);
    }

    public void tickServer() {
        if (!(this.level instanceof ServerLevel sl) || !armed || detonated) {
            return;
        }
        ticksLeft--;
        this.setChanged();

        // Босс-бар таймера — только тем, кто В ЭТОМ измерении: сбежавшие через
        // портал не должны таскать зависшую плашку по Земле.
        bar.setProgress(Math.max(0.0F, ticksLeft / (float) FUSE_TICKS));
        bar.setName(Component.literal(ticksLeft <= NO_RETURN_TICKS
                ? "§4☢ РЕАКТОР НЕ ОСТАНОВИТЬ — БЕГИТЕ: " + format(Math.max(0, ticksLeft))
                : "§cДЕТОНАЦИЯ: " + format(Math.max(0, ticksLeft)) + "  §7|  ПРОЧНОСТЬ: " + integrity + "%"));
        if (sl.getGameTime() % 20L == 0L) {
            for (ServerPlayer p : java.util.List.copyOf(bar.getPlayers())) {
                if (p.level() != sl) {
                    bar.removePlayer(p);
                }
            }
            for (ServerPlayer p : sl.players()) {
                bar.addPlayer(p);
            }
        }

        // Вид и звук работающего реактора.
        if (sl.getGameTime() % 4L == 0L) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 1.2D, worldPosition.getZ() + 0.5D, 3, 0.3D, 0.4D, 0.3D, 0.05D);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, worldPosition.getX() + 0.5D,
                    worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D, 1, 0.1D, 0.2D, 0.1D, 0.01D);
        }
        // ТОЧКА НЕВОЗВРАТА (последние 10 секунд): реактор перегрет, рою его уже
        // не сгрызть — оборона окончена, остаётся только успеть в портал.
        if (ticksLeft == NO_RETURN_TICKS) {
            AlienUtils.broadcastTitle(sl,
                    Component.literal("§4ТОЧКА НЕВОЗВРАТА"),
                    Component.literal("§cРеактор не остановить. БЕГИТЕ К ПОРТАЛУ!"));
            sl.playSound(null, worldPosition, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.MASTER, 4.0F, 1.8F);
        }
        if (ticksLeft <= NO_RETURN_TICKS && ticksLeft % 20 == 0) {
            int sec = ticksLeft / 20;
            for (ServerPlayer p : sl.players()) {
                p.displayClientMessage(Component.literal("§4§l☢ " + sec + " ☢"), true);
            }
            sl.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.MASTER, 4.0F,
                    1.4F + (NO_RETURN_TICKS - ticksLeft) / (float) NO_RETURN_TICKS);
        } else if (ticksLeft % 100 == 0) {
            sl.playSound(null, worldPosition, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 2.0F, 0.6F);
        }

        // ВОЛНЫ: рой бросает всё на защиту планеты (до точки невозврата).
        if (ticksLeft % 150 == 0 && ticksLeft > 250) {
            spawnWave(sl);
        }

        // Грызут реактор: каждый пришелец вплотную снимает прочность. После точки
        // невозврата прочность уже не важна — взрыв неминуем.
        if (ticksLeft > NO_RETURN_TICKS && sl.getGameTime() % 20L == 0L) {
            int chewers = sl.getEntitiesOfClass(Mob.class, new AABB(worldPosition).inflate(3.5D),
                    m -> m.isAlive() && AlienUtils.isAlliedTo(null, m)).size();
            if (chewers > 0) {
                integrity -= chewers;
                sl.sendParticles(ParticleTypes.CRIT, worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 0.6D, worldPosition.getZ() + 0.5D, 6, 0.4D, 0.4D, 0.4D, 0.2D);
                sl.playSound(null, worldPosition, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 1.2F, 0.7F);
                if (integrity <= 0) {
                    fail(sl);
                    return;
                }
            }
        }

        if (ticksLeft <= 0) {
            detonate(sl);
        }
    }

    private void spawnWave(ServerLevel sl) {
        int count = 4 + sl.random.nextInt(3);
        for (int i = 0; i < count; i++) {
            float roll = sl.random.nextFloat();
            Mob mob = (roll < 0.5F ? EntityRegistry.ALIEN_GRUNT
                    : roll < 0.8F ? EntityRegistry.ALIEN_RAPTOR
                    : EntityRegistry.ALIEN_BREACHER).create(sl);
            if (mob == null) {
                continue;
            }
            double a = sl.random.nextDouble() * Math.PI * 2.0D;
            double dist = 18.0D + sl.random.nextDouble() * 10.0D;
            int x = worldPosition.getX() + (int) (Math.cos(a) * dist);
            int z = worldPosition.getZ() + (int) (Math.sin(a) * dist);
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            mob.moveTo(x + 0.5D, y, z + 0.5D, sl.random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.EVENT, null);
            ServerPlayer nearest = sl.getNearestPlayer(net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat(),
                    mob, mob.getX(), mob.getY(), mob.getZ()) instanceof ServerPlayer sp ? sp : null;
            if (nearest != null) {
                mob.setTarget(nearest);
            }
            sl.addFreshEntity(mob);
            sl.sendParticles(ParticleTypes.PORTAL, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 12, 0.3D, 0.5D, 0.3D, 0.1D);
        }
        sl.playSound(null, worldPosition, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 3.0F, 0.5F);
    }

    private void setChunksForced(ServerLevel sl, boolean forced) {
        net.minecraft.world.level.ChunkPos c = new net.minecraft.world.level.ChunkPos(worldPosition);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                sl.setChunkForced(c.x + dx, c.z + dz, forced);
            }
        }
    }

    private void fail(ServerLevel sl) {
        detonated = true; // блокируем дальнейшие тики до удаления
        setChunksForced(sl, false);
        bar.removeAllPlayers();
        AlienUtils.broadcastTitle(sl,
                Component.literal("§4РЕАКТОР УНИЧТОЖЕН"),
                Component.literal("§cРой отбил атаку. Поднимите реактор и попробуйте снова."));
        sl.getServer().getPlayerList().broadcastSystemMessage(Component.literal(
                "§6[Макс Максбетов] §fВЫ ЧЁ, ДАЛИ ИМ СОЖРАТЬ МОЙ РЕАКТОР?! Поднимайте и по новой, растяпы!"), false);
        sl.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2.0F, 1.4F);
        sl.destroyBlock(worldPosition, true); // дропает предмет — можно повторить
    }

    private void detonate(ServerLevel sl) {
        detonated = true;
        setChunksForced(sl, false);
        bar.removeAllPlayers();

        // Вспышка над всей ареной.
        for (int i = 0; i < 40; i++) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    worldPosition.getX() + (sl.random.nextDouble() - 0.5D) * 80.0D,
                    worldPosition.getY() + sl.random.nextDouble() * 30.0D,
                    worldPosition.getZ() + (sl.random.nextDouble() - 0.5D) * 80.0D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        sl.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.MASTER, 10.0F, 0.3F);
        sl.playSound(null, worldPosition, SoundEvents.WITHER_DEATH, SoundSource.MASTER, 6.0F, 0.4F);

        // Кто не успел уйти — сгорает во вспышке.
        for (ServerPlayer p : sl.players()) {
            p.sendSystemMessage(Component.literal("§4Вспышка пожирает горизонт. Вы не успели уйти..."));
            p.hurt(sl.damageSources().fellOutOfWorld(), 100000.0F);
        }
        // Планета начинает гореть.
        for (int i = 0; i < 120; i++) {
            int x = worldPosition.getX() + sl.random.nextInt(81) - 40;
            int z = worldPosition.getZ() + sl.random.nextInt(81) - 40;
            int y = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos fp = new BlockPos(x, y, z);
            if (sl.getBlockState(fp).isAir()) {
                sl.setBlockAndUpdate(fp, net.minecraft.world.level.block.Blocks.FIRE.defaultBlockState());
            }
        }
        sl.removeBlock(worldPosition, false);

        // Земля исцеляется.
        ServerLevel overworld = sl.getServer().getLevel(Level.OVERWORLD);
        if (overworld != null) {
            InvasionManager.get(overworld).onPlanetDestroyed(overworld);
        }
    }

    public void onBroken() {
        bar.removeAllPlayers();
    }

    @Override
    public void setRemoved() {
        bar.removeAllPlayers();
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Armed", armed);
        tag.putInt("TicksLeft", ticksLeft);
        tag.putInt("Integrity", integrity);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.armed = tag.getBoolean("Armed");
        this.ticksLeft = tag.contains("TicksLeft") ? tag.getInt("TicksLeft") : FUSE_TICKS;
        this.integrity = tag.contains("Integrity") ? tag.getInt("Integrity") : MAX_INTEGRITY;
    }
}
