package com.example.alieninvasion.command;

import com.example.alieninvasion.world.InvasionManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import com.example.alieninvasion.registry.ItemRegistry;
import net.minecraft.core.particles.ParticleTypes;

public class InvasionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invasion")
                .requires(source -> source.hasPermission(2)) // OP permission
                .then(Commands.literal("status")
                        .executes(InvasionCommand::status))
                .then(Commands.literal("set")
                        .then(Commands.argument("day", IntegerArgumentType.integer(0))
                                .executes(InvasionCommand::setDay)))
                .then(Commands.literal("add")
                        .then(Commands.argument("days", IntegerArgumentType.integer(1))
                                .executes(InvasionCommand::addDays)))
                .then(Commands.literal("maxstage")
                        .executes(InvasionCommand::maxStage)));

        dispatcher.register(Commands.literal("alien-boots-jump")
                .executes(context -> {
                    net.minecraft.server.level.ServerPlayer player = context.getSource().getPlayerOrException();
                    if (player.getTags().contains("EmpActive")) {
                        return 0;
                    }
                    if (player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.FEET).is(ItemRegistry.GRAVITY_BOOTS)) {
                        if (!player.getAbilities().instabuild) {
                            boolean consumed = false;
                            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                                net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                                if (stack.is(ItemRegistry.ALIEN_BATTERY)) {
                                    stack.shrink(1);
                                    consumed = true;
                                    break;
                                }
                            }
                            if (consumed && player.level() instanceof ServerLevel sl) {
                                sl.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 10, 0.3, 0.1, 0.3, 0.05);
                            }
                        }
                    }
                    return 1;
                }));
    }

    // Jump straight to the final stage: Total War with the toughest, fully-evolved
    // swarm and the Hive Tyrant boss in the rotation.
    private static final int FINAL_STAGE_DAY = 20;

    private static int maxStage(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        InvasionManager manager = InvasionManager.get(level);
        manager.setInvasionDays(FINAL_STAGE_DAY);
        context.getSource().sendSuccess(
                () -> Component.literal("§c[Вторжение] Включена ПОСЛЕДНЯЯ стадия — Тотальная война! День: " + FINAL_STAGE_DAY),
                true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        InvasionManager manager = InvasionManager.get(level);
        int days = manager.getInvasionDays();

        String difficultyStage = "Фаза разведки";
        if (days >= 5) {
            difficultyStage = "Тотальная война";
        } else if (days >= 3) {
            difficultyStage = "Фаза штурма";
        }
        final String finalDifficulty = difficultyStage;

        context.getSource().sendSuccess(() -> Component.literal("§a[Вторжение] Статус:"), false);
        context.getSource().sendSuccess(() -> Component.literal("§eДень: " + days), false);
        context.getSource().sendSuccess(() -> Component.literal("§cУровень угрозы: " + finalDifficulty), false);

        return 1;
    }

    private static int setDay(CommandContext<CommandSourceStack> context) {
        int day = IntegerArgumentType.getInteger(context, "day");
        ServerLevel level = context.getSource().getLevel();
        InvasionManager manager = InvasionManager.get(level);

        manager.setInvasionDays(day);
        context.getSource().sendSuccess(() -> Component.literal("§a[Вторжение] День вторжения установлен: " + day),
                true);
        return 1;
    }

    private static int addDays(CommandContext<CommandSourceStack> context) {
        int daysToAdd = IntegerArgumentType.getInteger(context, "days");
        ServerLevel level = context.getSource().getLevel();
        InvasionManager manager = InvasionManager.get(level);

        int newDay = manager.getInvasionDays() + daysToAdd;
        manager.setInvasionDays(newDay);

        context.getSource().sendSuccess(
                () -> Component.literal("§a[Вторжение] Пропущено дней: " + daysToAdd + ". Текущий день: " + newDay),
                true);
        return 1;
    }
}
