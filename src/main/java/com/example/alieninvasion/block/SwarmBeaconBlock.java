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
            // Разрешаем призыв на 8-й день (или в креативе)
            if (day >= 8 || player.isCreative()) {
                ServerLevel sl = (ServerLevel) level;
                
                // Спавним Мать Роя
                com.example.alieninvasion.entity.SwarmMotherEntity boss = EntityRegistry.SWARM_MOTHER.create(sl);
                if (boss != null) {
                    boss.moveTo(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
                    boss.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.EVENT, null);
                    sl.addFreshEntity(boss);
                    
                    // Эффекты
                    sl.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.5F);
                    sl.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0F, 0.5F);
                    
                    // Черное небо: переключаем погоду на грозу
                    sl.setWeatherParameters(0, 24000, true, true);
                    
                    // Убираем маяк
                    level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    
                    // Сообщение
                    for (Player p : level.players()) {
                        p.sendSystemMessage(Component.literal("§4[!] Мать Роя призвана! Небо темнеет, воздух наполняется электричеством..."));
                    }
                }
            } else {
                player.displayClientMessage(Component.literal("§c[Маяк роя] Улей спит. Маяк можно активировать только на 8-й день вторжения (сейчас День " + day + ")."), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
