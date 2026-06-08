package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModBlocks;
import com.example.alieninvasion.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Player tunneling machine. It is deliberately weak as surface transport and
 * strong as a fueled cave borer.
 */
public class BorerVehicleEntity extends Mob {
    private static final int MAX_FUEL = 1800;
    private static final int BATTERY_FUEL = 700;
    private static final int FUEL_CELL_FUEL = 300;
    private static final int DRILL_RADIUS = 1;

    private int fuel;
    private int empTicks;

    public BorerVehicleEntity(EntityType<? extends Mob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 70.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.34D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.STEP_HEIGHT, 0.8D);
    }

    public void setEmpTicks(int ticks) {
        this.empTicks = ticks;
    }

    @Override
    protected void registerGoals() {
        // Player controlled.
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof Player p ? p : null;
    }

    // Seat the rider DOWN INSIDE the hull (a cockpit), not floating on the roof.
    // The offset is in entity-local space and is rotated by the machine's yaw, so
    // the driver stays in the cab as the borer turns.
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dims, float scale) {
        return new Vec3(0.0D, 0.35D * scale, -0.15D * scale);
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 travelVector) {
        double strafe = player.xxa * 0.18D;
        double forward = player.zza * 0.45D;
        if (this.fuel <= 0) {
            strafe *= 0.2D;
            forward *= 0.2D;
        }
        return new Vec3(strafe, 0.0D, forward);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        double base = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
        if (this.fuel <= 0) {
            return (float) (base * 0.25D);
        }
        return (float) (base * (hasDrillableBlockAhead(player) ? 1.65D : 0.45D));
    }

    @Override
    protected void tickRidden(Player player, Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        this.setRot(player.getYRot(), player.getXRot() * 0.35F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        if (isRiderJumping(player) && this.fuel > 0 && hasDrillableBlockAhead(player)) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.08D, 0.0D));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.getTags().contains("EmpActive")) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8D, 0.5D, 0.8D));
            if (this.level() instanceof ServerLevel sl && this.tickCount % 5 == 0) {
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 0.5D, this.getZ(),
                        3, 0.3D, 0.3D, 0.3D, 0.05D);
            }
            this.hurtMarked = true;
            if (this.empTicks <= 0) {
                this.empTicks = 160;
            }
            this.empTicks--;
            if (this.empTicks <= 0) {
                this.removeTag("EmpActive");
            }
            return;
        }

        if (!this.level().isClientSide && this.getControllingPassenger() instanceof Player rider) {
            drillAhead(rider);
            tickRiderModules(rider);
        }
    }

    private void drillAhead(Player rider) {
        Vec3 v = this.getDeltaMovement();
        // Drill while actively driving (W/S), boosting up (Space), or already moving.
        // NEVER bind drilling to sneak - that is the dismount key, so it ejected the
        // driver. To dig DOWN, look down and press W.
        boolean moving = v.horizontalDistanceSqr() > 0.001D || isRiderJumping(rider) || Math.abs(rider.zza) > 0.0F;
        if (!moving) {
            return;
        }

        if (this.fuel <= 0) {
            if (this.tickCount % 40 == 0) {
                rider.displayClientMessage(Component.literal(
                        "§c[Бур] Нет топлива. Заправьте Shift+ПКМ батареей или топливной ячейкой."), true);
            }
            return;
        }

        Direction face = drillDirection(rider);
        int broken = 0;
        for (BlockPos p : drillMask(this.blockPosition().relative(face), face)) {
            BlockState s = this.level().getBlockState(p);
            if (!canBorerBreak(p, s, rider)) {
                continue;
            }
            this.level().destroyBlock(p, true, rider);
            broken++;
            this.fuel = Math.max(0, this.fuel - fuelCostFor(s, rider));
            if (this.fuel <= 0) {
                break;
            }
        }

        if (broken > 0) {
            if (this.level() instanceof ServerLevel sl && this.tickCount % 4 == 0) {
                Vec3 nose = Vec3.atCenterOf(this.blockPosition().relative(face));
                sl.sendParticles(ParticleTypes.CRIT, nose.x, nose.y, nose.z, 8, 0.35D, 0.35D, 0.35D, 0.12D);
                sl.sendParticles(ParticleTypes.SMOKE, nose.x, nose.y, nose.z, 5, 0.25D, 0.25D, 0.25D, 0.04D);
                sl.playSound(null, this.blockPosition(), SoundEvents.NETHERITE_BLOCK_BREAK, SoundSource.NEUTRAL,
                        0.4F, 0.6F);
            }
            if (this.tickCount % 20 == 0) {
                rider.displayClientMessage(Component.literal("§6[Бур] Топливо: " + this.fuel + "/" + MAX_FUEL), true);
            }
        } else if (this.tickCount % 8 == 0) {
            this.fuel = Math.max(0, this.fuel - 1);
        }
    }

    private Direction drillDirection(Player rider) {
        if (isRiderJumping(rider)) {
            return Direction.UP;
        }
        // Direction comes from where the driver LOOKS (not sneak, which dismounts).
        Vec3 look = rider.getViewVector(1.0F);
        if (look.y > 0.65D) {
            return Direction.UP;
        }
        if (look.y < -0.65D) {
            return Direction.DOWN;
        }
        return Direction.getNearest(look.x, 0.0D, look.z);
    }

    private java.util.List<BlockPos> drillMask(BlockPos center, Direction face) {
        java.util.List<BlockPos> out = new java.util.ArrayList<>();
        Direction.Axis axis = face.getAxis();
        for (int a = -DRILL_RADIUS; a <= DRILL_RADIUS; a++) {
            for (int b = -DRILL_RADIUS; b <= DRILL_RADIUS; b++) {
                switch (axis) {
                    case X -> out.add(center.offset(0, a, b));
                    case Y -> out.add(center.offset(a, 0, b));
                    case Z -> out.add(center.offset(a, b, 0));
                }
            }
        }
        return out;
    }

    private boolean hasDrillableBlockAhead(Player rider) {
        Direction face = drillDirection(rider);
        for (BlockPos p : drillMask(this.blockPosition().relative(face), face)) {
            if (canBorerBreak(p, this.level().getBlockState(p), rider)) {
                return true;
            }
        }
        return false;
    }

    private boolean canBorerBreak(BlockPos pos, BlockState state, Player rider) {
        if (state.isAir()) return false;
        if (this.level().getBlockEntity(pos) != null) return false;
        if (state.getDestroySpeed(this.level(), pos) < 0.0F) return false;
        if (state.is(Blocks.BEDROCK) || state.is(Blocks.END_PORTAL_FRAME) || state.is(Blocks.REINFORCED_DEEPSLATE)) {
            return false;
        }
        if (isWorkstationOrStorage(state) || isOreOrValuable(state)) {
            return false;
        }
        if (state.is(Blocks.LAVA)) {
            return hasInventoryItem(rider, ItemRegistry.LAVA_COOLING_MODULE);
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(BlockTags.PLANKS) || state.is(BlockTags.LOGS) || state.is(Blocks.GRAVEL)
                || state.is(Blocks.CLAY) || state.is(Blocks.TUFF) || state.is(Blocks.CALCITE)
                || state.is(Blocks.NETHERRACK) || state.is(ModBlocks.INFESTED_STONE)
                || state.is(ModBlocks.INFESTED_DIRT) || state.is(ModBlocks.INFESTED_SAND)
                || state.is(ModBlocks.INFESTED_GRAVEL) || state.is(ModBlocks.INFESTED_DEEPSLATE)
                || state.is(ModBlocks.INFESTED_CLAY) || state.is(ModBlocks.INFESTED_NETHERRACK)
                || state.is(ModBlocks.INFESTED_PLANKS);
    }

    private boolean isWorkstationOrStorage(BlockState state) {
        return state.is(Blocks.CRAFTING_TABLE) || state.is(Blocks.FURNACE) || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.SMOKER) || state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL) || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.ANVIL)
                || state.is(Blocks.CHIPPED_ANVIL) || state.is(Blocks.DAMAGED_ANVIL)
                || state.is(Blocks.ENCHANTING_TABLE) || state.is(Blocks.BREWING_STAND)
                || state.is(Blocks.SMITHING_TABLE) || state.is(Blocks.GRINDSTONE) || state.is(Blocks.STONECUTTER)
                || state.is(Blocks.CARTOGRAPHY_TABLE) || state.is(Blocks.FLETCHING_TABLE) || state.is(Blocks.LOOM)
                || state.is(Blocks.COMPOSTER);
    }

    private boolean isOreOrValuable(BlockState state) {
        return state.is(BlockTags.COAL_ORES) || state.is(BlockTags.COPPER_ORES) || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.GOLD_ORES) || state.is(BlockTags.REDSTONE_ORES) || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.EMERALD_ORES) || state.is(BlockTags.DIAMOND_ORES)
                || state.is(ModBlocks.COSMIC_ORE) || state.is(ModBlocks.URANIUM_ORE)
                || state.is(ModBlocks.DEEPSLATE_URANIUM_ORE) || state.is(ModBlocks.XENOCRYSTAL_ORE)
                || state.is(ModBlocks.BIO_VEIN_ORE) || state.is(ModBlocks.PLASMA_ORE)
                || state.is(ModBlocks.IRIDIUM_ORE) || state.is(ModBlocks.DARK_MATTER_ORE)
                || state.is(Blocks.COAL_BLOCK) || state.is(Blocks.COPPER_BLOCK) || state.is(Blocks.IRON_BLOCK)
                || state.is(Blocks.GOLD_BLOCK) || state.is(Blocks.REDSTONE_BLOCK) || state.is(Blocks.LAPIS_BLOCK)
                || state.is(Blocks.EMERALD_BLOCK) || state.is(Blocks.DIAMOND_BLOCK)
                || state.is(Blocks.NETHERITE_BLOCK) || state.is(ModBlocks.COSMIC_BLOCK);
    }

    private int fuelCostFor(BlockState state, Player rider) {
        int cost = state.is(BlockTags.BASE_STONE_OVERWORLD) ? 2 : 1;
        if (hasInventoryItem(rider, ItemRegistry.REINFORCED_DRILL_HEAD)) {
            cost = Math.max(1, cost - 1);
        }
        return cost;
    }

    private void tickRiderModules(Player rider) {
        if (this.tickCount % 40 == 0 && hasInventoryItem(rider, ItemRegistry.HEADLAMP_MODULE)) {
            rider.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.NIGHT_VISION, 120, 0, true, false));
        }
        if (hasInventoryItem(rider, ItemRegistry.TOXIC_SEAL_MODULE)) {
            rider.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION));
            rider.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
        }
        // Purifier drill head: scrubs the alien infection off the driver as it bores
        // (complements the toxic seal, which handles radiation + poison).
        if (hasInventoryItem(rider, ItemRegistry.PURIFIER_DRILL_HEAD)) {
            rider.removeEffect(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.INFECTION));
            if (this.tickCount % 20 == 0 && this.level() instanceof net.minecraft.server.level.ServerLevel psl) {
                psl.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        this.getX(), this.getY() + 0.6D, this.getZ(), 6, 0.6D, 0.4D, 0.6D, 0.0D);
            }
        }
        // Radiation drill head: the leaking core irradiates and burns nearby hostiles as
        // you drill - turns the borer into a mobile area-denial weapon.
        if (this.tickCount % 40 == 0 && hasInventoryItem(rider, ItemRegistry.RADIATION_DRILL_HEAD)
                && this.level() instanceof net.minecraft.server.level.ServerLevel rsl) {
            for (net.minecraft.world.entity.LivingEntity e : this.level().getEntitiesOfClass(
                    net.minecraft.world.entity.LivingEntity.class, this.getBoundingBox().inflate(6.0D),
                    ent -> ent instanceof net.minecraft.world.entity.monster.Enemy && ent.isAlive())) {
                e.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        BuiltInRegistries.MOB_EFFECT.wrapAsHolder(ModEffects.RADIATION), 100, 0, false, true));
                e.hurt(this.damageSources().magic(), 2.0F);
            }
            rsl.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                    this.getX(), this.getY() + 0.6D, this.getZ(), 10, 1.5D, 0.6D, 1.5D, 0.0D);
        }
    }

    private boolean hasInventoryItem(Player rider, Item item) {
        for (int i = 0; i < rider.getInventory().getContainerSize(); i++) {
            if (rider.getInventory().getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }

    private boolean refuel(Player player, ItemStack stack) {
        int amount = 0;
        if (stack.is(ItemRegistry.ALIEN_BATTERY)) {
            amount = BATTERY_FUEL;
        } else if (stack.is(ItemRegistry.DRILL_FUEL_CELL)) {
            amount = FUEL_CELL_FUEL;
        }
        if (amount <= 0 || this.fuel >= MAX_FUEL) {
            return false;
        }
        this.fuel = Math.min(MAX_FUEL, this.fuel + amount);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.displayClientMessage(Component.literal("§a[Бур] Заправлено: " + this.fuel + "/" + MAX_FUEL), true);
        this.level().playSound(null, this.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS,
                0.8F, 1.3F);
        return true;
    }

    private boolean isRiderJumping(Player player) {
        return ((com.example.alieninvasion.mixin.LivingEntityAccessor) player).isJumping();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && refuel(player, stack)) {
                return InteractionResult.SUCCESS;
            }
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuel", this.fuel);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.fuel = tag.getInt("Fuel");
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return false;
    }
}
