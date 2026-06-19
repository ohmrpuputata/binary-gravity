package com.example.alieninvasion.mixin;

import com.example.alieninvasion.block.BloodyBlocks;
import com.example.alieninvasion.logic.BleedManager;
import com.example.alieninvasion.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Трупы не исчезают мгновенно. Убитое существо (НЕ игрок — у него экран смерти)
 * лежит ~5 секунд, всё это время МЕДЛЕННО истекает кровью (капли + лужи нужного
 * цвета), затем ПРОВАЛИВАЕТСЯ под землю и пропадает с обычным «пуфом».
 *
 * Полностью заменяет ванильный tickDeath (там тело удаляется уже на 20-м тике).
 * deathTime считаем сами на обеих сторонах — клиент по нему крутит анимацию смерти;
 * частицы/лужи/погружение/удаление делает только сервер.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCorpseMixin {
    @Shadow public int deathTime;

    private static final int LIE_TICKS = 100;   // ~5 c лежит
    private static final int SINK_START = 100;   // потом тонет
    private static final int GONE = 130;         // и исчезает

    @Inject(method = "tickDeath", at = @At("HEAD"), cancellable = true)
    private void alien$corpse(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player) {
            return; // игроки умирают по-ванильному (экран смерти/респаун)
        }
        this.deathTime++;
        // Труп НЕ живёт: глушим ИИ/навигацию/прыжки и гасим горизонтальную скорость —
        // иначе «мертвец» все эти секунды бегает, прыгает и таранит игрока.
        if (self instanceof net.minecraft.world.entity.Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setTarget(null);
        }
        self.setDeltaMovement(0.0D, self.getDeltaMovement().y, 0.0D);
        self.setJumping(false);
        if (!(self.level() instanceof ServerLevel sl)) {
            ci.cancel(); // клиент: только крутим deathTime для анимации, не удаляем
            return;
        }

        boolean purple = BleedManager.isInfectedBlood(self);
        if (this.deathTime % 4 == 0) {
            sl.sendParticles(purple ? ModParticles.BLOOD_PURPLE : ModParticles.BLOOD,
                    self.getX(), self.getY() + 0.15D, self.getZ(), 4, 0.18D, 0.04D, 0.18D, 0.02D);
        }
        if (this.deathTime % 14 == 0 && this.deathTime < SINK_START) {
            BloodyBlocks.splatter(sl, self.blockPosition().below(), purple);
        }
        if (this.deathTime >= SINK_START) {
            self.setPos(self.getX(), self.getY() - 0.045D, self.getZ()); // медленно тонет под землю
        }
        if (this.deathTime >= GONE && !self.isRemoved()) {
            sl.broadcastEntityEvent(self, (byte) 60); // ванильный «пуф» частиц при исчезновении
            self.remove(Entity.RemovalReason.KILLED);
        }
        ci.cancel();
    }
}
