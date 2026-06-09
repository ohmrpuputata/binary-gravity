package com.example.alieninvasion.logic;

import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dose-based radiation survival mechanic.
 *
 * Instead of a binary "near a source -> get the effect" toggle, every player
 * carries an accumulating dose (rads). Standing in a uranium pocket, beside a
 * radiation crystal cluster, in toxic water, or out in a radioactive storm makes
 * the dose climb; getting clear lets the body slowly recover. The dose maps onto
 * escalating tiers of the RADIATION effect, so lingering is punished and a quick
 * dash past a source is survivable.
 *
 * Mitigation:
 *  - Full hazmat suit or a borer with a Toxic Seal module: blocks intake entirely.
 *  - Bio-filter mask (head slot): halves intake.
 *  - Rad pills / portable purifier: actively flush accumulated dose.
 *
 * Dose is kept in a static map (like the mod's other per-player session state); it
 * naturally clears over time and on death, so it is not persisted to disk.
 */
public final class RadiationManager {
    private RadiationManager() {
    }

    public static final float MAX_DOSE = 100.0F;
    private static final float GAIN = 0.45F;    // dose per intensity unit per second

    private static final Map<UUID, Float>   DOSE           = new ConcurrentHashMap<>();
    private static final Map<UUID, Float>   DOSE_MULT      = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAST_DOSE_TIER = new ConcurrentHashMap<>();
    public  static final Map<UUID, Boolean> SCREEN_GLITCH  = new ConcurrentHashMap<>();

    // Radioactive storm: a world-wide fallout event (lifecycle ticked from ModEvents).
    private static int stormTicks = 0;

    public static float getDose(UUID id) {
        return DOSE.getOrDefault(id, 0.0F);
    }

    public static float getDose(Player player) {
        return getDose(player.getUUID());
    }

    private static void setDose(UUID id, float dose) {
        dose = Math.max(0.0F, Math.min(MAX_DOSE, dose));
        if (dose <= 0.01F) {
            DOSE.remove(id);
        } else {
            DOSE.put(id, dose);
        }
    }

    /** Set per-player dose intake multiplier (1.0 = normal, 0.5 = half speed). */
    public static void setDoseMultiplier(Player player, float mult) {
        if (mult == 1.0F) DOSE_MULT.remove(player.getUUID());
        else DOSE_MULT.put(player.getUUID(), mult);
    }

    /** Cap dose at max — used by protective armor set bonuses. */
    public static void capDose(Player player, float max) {
        UUID id = player.getUUID();
        float d = DOSE.getOrDefault(id, 0.0F);
        if (d > max) DOSE.put(id, max);
    }

    /** Flush accumulated dose (rad pills, purifier, cures). */
    public static void reduceDose(Player player, float amount) {
        setDose(player.getUUID(), getDose(player) - amount);
    }

    public static void clearDose(Player player) {
        UUID id = player.getUUID();
        DOSE.remove(id);
        LAST_DOSE_TIER.remove(id);
    }

    public static boolean isStormActive() {
        return stormTicks > 0;
    }

    /** Numeric "field" reading for the Geiger counter (weighted block scan). */
    public static int scanField(Level level, BlockPos center) {
        int danger = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -8; x <= 8; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -8; z <= 8; z++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    danger += sourceWeight(level.getBlockState(cursor));
                }
            }
        }
        if (isStormActive() && level.canSeeSky(center)) {
            danger += 6;
        }
        return Math.min(99, danger);
    }

    private static int sourceWeight(BlockState state) {
        if (state.is(ModBlocks.RADIATION_CRYSTAL_CLUSTER) || state.is(ModBlocks.PURE_RADIATION_BLOCK)
                || state.is(ModBlocks.PURE_RADIATION_CRYSTAL_ORE)) {
            return 5;
        }
        if (state.is(ModBlocks.TOXIC_WATER) || state.is(ModBlocks.TOXIC_BARREL)
                || state.is(ModBlocks.INFESTED_STONE) || state.is(ModBlocks.ALIEN_RESIDUE)) {
            return 1;
        }
        return 0;
    }

    // Distance-attenuated exposure used to drive dose: closer/denser = faster climb.
    private static float scanExposure(Level level, BlockPos center) {
        float exposure = 0.0F;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -4; x <= 4; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -4; z <= 4; z++) {
                    int w = sourceWeight(level.getBlockState(cursor.set(
                            center.getX() + x, center.getY() + y, center.getZ() + z)));
                    if (w == 0) continue;
                    int distSq = x * x + y * y + z * z;
                    float falloff = distSq <= 4 ? 1.0F : distSq <= 16 ? 0.5F : 0.25F;
                    exposure += w * falloff;
                }
            }
        }
        if (isStormActive() && level.canSeeSky(center)) {
            exposure += 4.0F; // fallout from open sky during the storm
        }
        return Math.min(12.0F, exposure);
    }

    public static boolean hasFullHazmat(Player player) {
        return false; // Hazmat armor replaced in Phase 3 with new Химзащита/Химдоспех sets
    }

    public static boolean hasFullLightHazmat(Player player) {
        return false;
    }

    public static void addDose(Player player, float amount) {
        float mult = DOSE_MULT.getOrDefault(player.getUUID(), 1.0F);
        setDose(player.getUUID(), getDose(player) + amount * mult);
    }

    private static void removeAllDoseEffects(ServerPlayer player) {
        player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION));
        player.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);
        player.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
        player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
        player.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);
        player.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
    }

    /** Re-applies all active tier effects after milk — called from mixin. */
    public static void reapplyDoseEffects(ServerPlayer player) {
        float dose = getDose(player);
        int tier = dose >= MAX_DOSE ? 4 : dose >= 75.0F ? 3 : dose >= 50.0F ? 2 : dose >= 25.0F ? 1 : 0;
        if (tier < 1) return;
        var irrH = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION);
        player.addEffect(new MobEffectInstance(irrH, 100, 0, false, true));
        if (tier >= 2) player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,            100, 1, false, true));
        if (tier >= 3) {
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,          100, 0, false, true));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 100, 0, false, true));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,      100, 0, false, true));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER,            100, 0, false, true));
        }
        if (tier >= 4) player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 100, 0, false, true));
    }

    /** Per-second player update. Call once a second (e.g. tickCount % 20 == 0). */
    public static void tickPlayer(ServerLevel level, ServerPlayer player) {
        UUID id = player.getUUID();
        if (player.isCreative() || player.isSpectator()) {
            clearDose(player);
            SCREEN_GLITCH.remove(id);
            removeAllDoseEffects(player);
            return;
        }

        boolean masked = player.getItemBySlot(EquipmentSlot.HEAD).is(ItemRegistry.BIO_FILTER_MASK);
        float exposure = scanExposure(level, player.blockPosition());
        if (masked) exposure *= 0.5F;

        float dose = getDose(player);
        if (exposure > 0.0F) {
            dose += exposure * GAIN;
        }
        setDose(id, dose);
        dose = getDose(player);

        int newTier  = dose >= MAX_DOSE ? 4 : dose >= 75.0F ? 3 : dose >= 50.0F ? 2 : dose >= 25.0F ? 1 : 0;
        int prevTier = LAST_DOSE_TIER.getOrDefault(id, 0);

        // Tier decreased — strip effects that no longer apply
        if (newTier < prevTier) {
            if (newTier < 4) player.removeEffect(net.minecraft.world.effect.MobEffects.DARKNESS);
            if (newTier < 3) {
                player.removeEffect(net.minecraft.world.effect.MobEffects.WEAKNESS);
                player.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
                player.removeEffect(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN);
                player.removeEffect(net.minecraft.world.effect.MobEffects.HUNGER);
            }
            if (newTier < 2) player.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);
            if (newTier < 1) player.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION));
            LAST_DOSE_TIER.put(id, newTier);
        } else if (newTier > prevTier) {
            LAST_DOSE_TIER.put(id, newTier);
        }

        // Apply all cumulative effects for active tiers
        var irrH = BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.IRRADIATION);
        if (newTier >= 1) player.addEffect(new MobEffectInstance(irrH, 60, 0, false, true));
        if (newTier >= 2) player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER,            60, 1, false, true));
        if (newTier >= 3) {
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,          60, 0, false, true));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 0, false, true));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,      60, 0, false, true));
            player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.HUNGER,            60, 0, false, true));
        }
        if (newTier >= 4) player.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 60, 0, false, true));

        if (newTier >= 2) SCREEN_GLITCH.put(id, true);
        else              SCREEN_GLITCH.remove(id);

        // HUD feedback: readout + Geiger clicks
        if (exposure > 0.0F || dose >= 25.0F) {
            String color = dose >= 75.0F ? "§4" : dose >= 50.0F ? "§c" : dose >= 25.0F ? "§e" : "§a";
            player.displayClientMessage(Component.literal(
                    color + "☢ Облучение: " + (int) dose + "%"), true);
            if (exposure > 0.0F && level.random.nextFloat() < Math.min(0.8F, 0.1F + exposure * 0.06F)) {
                level.playSound(null, player.blockPosition(), SoundEvents.STONE_BUTTON_CLICK_ON,
                        SoundSource.PLAYERS, 0.25F, 1.7F + level.random.nextFloat() * 0.6F);
            }
        }
    }

    /** World-tick lifecycle for the radioactive-storm event. */
    public static void tickStorm(ServerLevel level) {
        if (stormTicks > 0) {
            stormTicks--;
            if (stormTicks == 0) {
                for (ServerPlayer p : level.players()) {
                    p.displayClientMessage(Component.literal("§a[!] Радиоактивная буря утихла."), false);
                }
            }
            return;
        }
        // Day 5+, late at night, rare: a fallout storm sweeps the surface.
        if (level.isNight() && level.getGameTime() % 24000 == 15000
                && SurvivalManager.getDay(level) >= 5 && level.random.nextFloat() < 0.18F
                && !level.players().isEmpty()) {
            stormTicks = 3000; // ~2.5 minutes
            for (ServerPlayer p : level.players()) {
                p.displayClientMessage(Component.literal(
                        "§4[!] РАДИОАКТИВНАЯ БУРЯ! Найдите укрытие или наденьте костюм химзащиты."), false);
                level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER,
                        SoundSource.AMBIENT, 1.0F, 0.45F);
            }
        }
    }
}
