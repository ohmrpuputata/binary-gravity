package com.example.alieninvasion.block;

import com.example.alieninvasion.logic.StructureLocationsData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * RADIO TRANSMITTER: tune into the static. On use it scans for the strongest
 * anomalous signal and reports the bearing to the nearest buried mothership or
 * monolith — the way to hunt the rare dungeons on purpose instead of by luck.
 */
public class RadioTransmitterBlock extends Block {
    public RadioTransmitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        // Radio crackle while "tuning".
        serverLevel.playSound(null, pos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 0.9F, 0.6F);
        serverLevel.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.5F, 1.8F);

        // One line per signal type: alien anomalies AND the survivor frequency.
        StructureLocationsData data = StructureLocationsData.get(serverLevel);
        boolean any = false;
        for (String[] type : new String[][]{
                {"mothership", "§dПогребённая матка"},
                {"monolith", "§5Монолит"},
                {"bunker", "§aБункер выживших"}}) {
            StructureLocationsData.Entry nearest = data.nearestOfType(type[0], pos);
            if (nearest == null) continue;
            any = true;
            BlockPos target = nearest.pos();
            int dist = (int) Math.sqrt(target.distSqr(pos));
            player.displayClientMessage(Component.literal(
                    "§b[Радио] " + type[1] + "§b — [" + target.getX() + ", " + target.getZ()
                            + "], ~" + dist + " бл. на " + bearing(pos, target) + "."), false);
        }
        if (!any) {
            player.displayClientMessage(Component.literal(
                    "§7[Радио] Только помехи... Сигналы появятся, когда мир сгенерирует свои тайны."), false);
        }
        return InteractionResult.CONSUME;
    }

    private static String bearing(BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double ang = Math.toDegrees(Math.atan2(dz, dx)); // 0 = восток
        if (ang < 0) ang += 360;
        String[] dirs = {"восток", "юго-восток", "юг", "юго-запад", "запад", "северо-запад", "север", "северо-восток"};
        return dirs[(int) Math.round(ang / 45.0) % 8];
    }
}
