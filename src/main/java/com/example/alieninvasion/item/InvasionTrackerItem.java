package com.example.alieninvasion.item;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Invasion Tracker - a proper recon tool now. On use it:
 *   - finds the nearest alien structure (hive / orbital beacon / stash / swarm
 *     beacon) and reports distance, compass bearing and whether it's above/below;
 *   - draws a glowing particle trail toward it;
 *   - flags every nearby alien with GLOWING so you can see threats through walls;
 *   - also points you to the nearest Cosmic Ore vein.
 */
public class InvasionTrackerItem extends Item {
    public InvasionTrackerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.sidedSuccess(stack, true);
        }

        ServerLevel sl = (ServerLevel) level;
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();

        BlockPos nearestStructure = null;
        String structureName = "";
        double bestStruct = Double.MAX_VALUE;
        BlockPos nearestOre = null;
        double bestOre = Double.MAX_VALUE;

        int rXZ = 128, rY = 72;
        for (int x = -rXZ; x <= rXZ; x += 4) {
            for (int y = -rY; y <= rY; y += 4) {
                for (int z = -rXZ; z <= rXZ; z += 4) {
                    mut.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockState s = sl.getBlockState(mut);
                    String name = structureNameFor(s);
                    if (name != null) {
                        double d = origin.distSqr(mut);
                        if (d < bestStruct) { bestStruct = d; nearestStructure = mut.immutable(); structureName = name; }
                    } else if (s.is(ModBlocks.PLATINUM_ORE) || s.is(ModBlocks.PALLADIUM_ORE)) {
                        double d = origin.distSqr(mut);
                        if (d < bestOre) { bestOre = d; nearestOre = mut.immutable(); }
                    }
                }
            }
        }

        // Tag nearby aliens so they glow through walls (huge in fights / co-op).
        int marked = 0;
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(48.0D),
                e -> AlienUtils.isAlliedTo(null, e))) {
            mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0, false, false));
            marked++;
        }

        boolean foundAnything = false;
        if (nearestStructure != null) {
            foundAnything = true;
            double dist = Math.sqrt(bestStruct);
            int dy = nearestStructure.getY() - origin.getY();
            String vert = dy > 4 ? " §7(выше)" : dy < -4 ? " §7(ниже)" : "";
            String bearing = bearing(nearestStructure.getX() - origin.getX(), nearestStructure.getZ() - origin.getZ());
            player.displayClientMessage(Component.literal("§a[Сканер] " + structureName + ": §f"
                    + (int) dist + "м §7на §e" + bearing + vert), false);
            trail(sl, player.getEyePosition(), Vec3.atCenterOf(nearestStructure), ParticleTypes.HAPPY_VILLAGER);
            sl.playSound(null, origin, SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1.0F, 1.6F);
        }
        if (nearestOre != null) {
            foundAnything = true;
            double dist = Math.sqrt(bestOre);
            String bearing = bearing(nearestOre.getX() - origin.getX(), nearestOre.getZ() - origin.getZ());
            player.displayClientMessage(Component.literal("§6[Руда] Космическая руда: §f"
                    + (int) dist + "м §7на §e" + bearing), true);
            trail(sl, player.getEyePosition(), Vec3.atCenterOf(nearestOre), ParticleTypes.FLAME);
        }
        if (marked > 0) {
            player.displayClientMessage(Component.literal("§b[Сканер] Подсвечено пришельцев: §f" + marked), true);
        }
        if (!foundAnything && marked == 0) {
            player.displayClientMessage(Component.literal("§c[Сканер] Поблизости чисто."), true);
            sl.playSound(null, origin, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.8F, 0.5F);
        }

        player.getCooldowns().addCooldown(this, 60);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static String structureNameFor(BlockState s) {
        if (s.is(ModBlocks.SWARM_BEACON)) return "Маяк Роя (БОСС)";
        if (s.is(ModBlocks.ALIEN_BEACON)) return "Маяк / Город пришельцев";
        if (s.is(ModBlocks.ALIEN_HIVE)) return "Улей пришельцев";
        if (s.is(ModBlocks.ALIEN_STASH)) return "Тайник пришельцев";
        return null;
    }

    private static void trail(ServerLevel sl, Vec3 from, Vec3 to, net.minecraft.core.particles.SimpleParticleType type) {
        Vec3 dir = to.subtract(from).normalize();
        for (int i = 1; i <= 18; i++) {
            Vec3 at = from.add(dir.scale(i * 0.9));
            sl.sendParticles(type, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static String bearing(double dx, double dz) {
        double ang = (Math.toDegrees(Math.atan2(dx, -dz)) + 360) % 360;
        String[] pts = {"С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ"};
        return pts[(int) Math.round(ang / 45.0) % 8];
    }
}
