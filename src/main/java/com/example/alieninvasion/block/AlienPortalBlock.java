package com.example.alieninvasion.block;

import com.example.alieninvasion.entity.AlienUtils;
import com.example.alieninvasion.logic.HomeworldManager;
import com.example.alieninvasion.world.InvasionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Разрыв пространства: портал между Землёй и родным миром Роя.
 * Появляется после гибели Матери Роя. Вход — просто шагнуть внутрь.
 */
public class AlienPortalBlock extends Block {
    private static final Map<UUID, Long> COOLDOWN = new ConcurrentHashMap<>();
    private static final long COOLDOWN_TICKS = 100L;

    public AlienPortalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            return;
        }
        long now = level.getGameTime();
        Long last = COOLDOWN.get(player.getUUID());
        if (last != null && now - last < COOLDOWN_TICKS) {
            return;
        }
        COOLDOWN.put(player.getUUID(), now);

        MinecraftServer server = level.getServer();
        if (server == null) {
            return;
        }

        if (level.dimension().equals(HomeworldManager.HOMEWORLD)) {
            // Домой, на Землю — выходим рядом с порталом победы.
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld == null) {
                return;
            }
            BlockPos anchor = InvasionManager.get(overworld).getOverworldPortalPos();
            if (anchor == null) {
                anchor = overworld.getSharedSpawnPos();
            }
            int y = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING, anchor.getX() + 3, anchor.getZ() + 2);
            playWhoosh(level, pos);
            player.teleportTo(overworld, anchor.getX() + 3.5D, y, anchor.getZ() + 2.5D,
                    player.getYRot(), player.getXRot());
            playWhoosh(overworld, player.blockPosition());
        } else {
            // В родной мир Роя — улей (лор) и площадка у ГОРОДА строятся при первом входе.
            ServerLevel homeworld = server.getLevel(HomeworldManager.HOMEWORLD);
            if (homeworld == null) {
                player.displayClientMessage(Component.literal("§cРазрыв нестабилен... измерение недоступно."), true);
                return;
            }
            HomeworldManager.ensureArena(homeworld);      // главный улей — теперь просто часть мира
            HomeworldManager.ensureCityArena(homeworld);  // площадка прибытия + обратный портал у города
            BlockPos arrival = HomeworldManager.CITY_ARRIVAL;
            int y = HomeworldManager.cityArrivalY(homeworld);
            playWhoosh(level, pos);
            player.teleportTo(homeworld, arrival.getX() + 0.5D, y, arrival.getZ() + 0.5D, 180.0F, 0.0F);
            playWhoosh(homeworld, player.blockPosition());
            AlienUtils.broadcastTitle(homeworld,
                    Component.literal("§5СТОЛИЦА РОЯ"),
                    Component.literal("§dЗдесь бьётся сердце вторжения. Здесь его и оборвём."));
            player.sendSystemMessage(Component.literal(
                    "§dПеред вами ГИГАНТСКИЙ город Роя. Несите бомбу к центральному шпилю — и поставьте её там."));
        }
    }

    private static void playWhoosh(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.5F, 0.55F);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5D) * 0.4D, -0.2D, (random.nextDouble() - 0.5D) * 0.4D);
        }
        if (random.nextInt(80) == 0) {
            level.playLocalSound(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS, 0.4F, 0.5F, false);
        }
    }
}
