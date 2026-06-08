package com.example.alieninvasion.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;

public class CustomEntitySpawnerItem extends Item {
    private final java.util.function.Supplier<EntityType<?>> entityTypeSupplier;
    // Optional hook to customise the freshly-spawned entity (e.g. flag a grunt as a
    // scavenger "worker"). Runs after finalizeSpawn and before the entity is added.
    private final java.util.function.Consumer<Entity> postSpawn;

    public CustomEntitySpawnerItem(java.util.function.Supplier<EntityType<?>> entityTypeSupplier, Item.Properties properties) {
        this(entityTypeSupplier, properties, null);
    }

    public CustomEntitySpawnerItem(java.util.function.Supplier<EntityType<?>> entityTypeSupplier, Item.Properties properties,
                                   java.util.function.Consumer<Entity> postSpawn) {
        super(properties);
        this.entityTypeSupplier = entityTypeSupplier;
        this.postSpawn = postSpawn;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().isClientSide && context.getLevel() instanceof ServerLevel sl) {
            BlockPos targetPos = context.getClickedPos().relative(context.getClickedFace());
            Entity entity = entityTypeSupplier.get().create(sl);
            if (entity != null) {
                entity.moveTo(targetPos.getX() + 0.5D, targetPos.getY(), targetPos.getZ() + 0.5D, context.getPlayer() != null ? context.getPlayer().getYRot() : 0.0F, 0.0F);
                if (entity instanceof Mob mob) {
                    mob.finalizeSpawn(sl, sl.getCurrentDifficultyAt(targetPos), MobSpawnType.SPAWNER, null);
                }
                if (this.postSpawn != null) {
                    this.postSpawn.accept(entity);
                }
                sl.addFreshEntity(entity);
                context.getItemInHand().shrink(1);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
