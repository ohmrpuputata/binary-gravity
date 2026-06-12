package com.example.alieninvasion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import com.example.alieninvasion.logic.SurvivalManager;
import com.example.alieninvasion.registry.EntityRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;

/**
 * Маяк роя (Swarm Beacon):
 * Призывает босса Мать Роя на 8-й день вторжения при активации.
 */
public class SwarmBeaconBlock extends Block {
    public SwarmBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide) {
            int day = SurvivalManager.getDay(level);
            int requiredDay = com.example.alieninvasion.world.InvasionManager.VICTORY_DAY;
            if (day >= requiredDay || player.isCreative()) {
                ServerLevel sl = (ServerLevel) level;

                // Одна королева на поле боя: повторная активация маяка рядом с живой
                // Матерью не плодит вторую (и не тратит маяк).
                if (!sl.getEntitiesOfClass(com.example.alieninvasion.entity.SwarmMotherEntity.class,
                        new net.minecraft.world.phys.AABB(pos).inflate(192.0D)).isEmpty()) {
                    player.displayClientMessage(Component.literal(
                            "§c[Маяк роя] Мать Роя уже на поле боя."), true);
                    return InteractionResult.SUCCESS;
                }

                // КИНЕМАТОГРАФИЧНЫЙ ПРИЗЫВ: Мать Роя появляется высоко в небе и
                // медленно спускается на луче света под раскаты грома, а над ней
                // зависает корабль-носитель. Бой начинается с её приземления.
                com.example.alieninvasion.entity.SwarmMotherEntity boss = EntityRegistry.SWARM_MOTHER.create(sl);
                if (boss != null) {
                    boss.moveTo(pos.getX() + 0.5D, pos.getY() + 45.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
                    boss.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.EVENT, null);
                    boss.beginDescent();
                    sl.addFreshEntity(boss);

                    // Эскорт: носитель зависает над точкой призыва.
                    com.example.alieninvasion.entity.UfoEntity carrier = EntityRegistry.UFO.create(sl);
                    if (carrier != null) {
                        carrier.moveTo(pos.getX() + 0.5D, pos.getY() + 58.0D, pos.getZ() + 0.5D,
                                sl.random.nextFloat() * 360.0F, 0.0F);
                        carrier.setVariant(com.example.alieninvasion.entity.UfoEntity.CARRIER);
                        sl.addFreshEntity(carrier);
                    }

                    // Кольцо визуальных молний вокруг маяка.
                    for (int i = 0; i < 4; i++) {
                        net.minecraft.world.entity.LightningBolt bolt =
                                net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
                        if (bolt != null) {
                            double a = i * (Math.PI / 2.0D);
                            bolt.moveTo(pos.getX() + 0.5D + Math.cos(a) * 5.0D, pos.getY(),
                                    pos.getZ() + 0.5D + Math.sin(a) * 5.0D);
                            bolt.setVisualOnly(true);
                            sl.addFreshEntity(bolt);
                        }
                    }

                    sl.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.5F);
                    sl.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0F, 0.5F);
                    sl.playSound(null, pos, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 3.0F, 0.6F);

                    // Черное небо: переключаем погоду на грозу
                    sl.setWeatherParameters(0, 24000, true, true);

                    // Убираем маяк
                    level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());

                    com.example.alieninvasion.entity.AlienUtils.broadcastTitle(sl,
                            Component.literal("§4МАТЬ РОЯ ПРИЗВАНА"),
                            Component.literal("§cОна спускается с орбиты..."));
                    for (Player p : level.players()) {
                        p.sendSystemMessage(Component.literal(
                                "§4[!] Мать Роя призвана! Небо темнеет, воздух наполняется электричеством..."));
                    }
                }
            } else {
                player.displayClientMessage(Component.literal("§c[Маяк роя] Улей спит. Маяк можно активировать только на "
                        + requiredDay + "-й день вторжения (сейчас День " + day + ")."), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
