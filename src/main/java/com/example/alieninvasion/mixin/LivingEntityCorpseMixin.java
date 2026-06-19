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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    // Труп — БЕЗ ХИТБОКСА: его нельзя задеть/прокликать (удары/курсор проходят сквозь к
    // врагам позади), пока он лежит/тонет. Игроков не трогаем — у них своя смерть.
    @Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
    private void alien$corpseNoHitbox(CallbackInfoReturnable<Boolean> cir) {
        if (this.deathTime > 0 && !((Object) this instanceof Player)) {
            cir.setReturnValue(false);
        }
    }

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
        // ПОЛНАЯ заморозка: нулевая скорость и ФИКСИРОВАННЫЙ поворот тела и головы (+ их
        // «прошлые» значения, чтобы не было интерполяции) — труп не дёргается, не крутит
        // головой и не поворачивается. setNoAi выше уже снял ввод движения/навигацию.
        self.setSpeed(0.0F);
        float corpseYaw = self.getYRot();
        self.setYBodyRot(corpseYaw);
        self.setYHeadRot(corpseYaw);
        self.yBodyRotO = corpseYaw;
        self.yHeadRotO = corpseYaw;
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
        // Заражённый червём носитель: червь ВЫЛЕЗАЕТ ИЗ ТРУПА по ходу лежания (≈2.5 c после
        // смерти), а не мгновенно при гибели.
        if (this.deathTime == 50
                && self.getTags().contains(com.example.alieninvasion.logic.WormInfestation.HOST_TAG)) {
            com.example.alieninvasion.logic.WormInfestation.emergeFromCorpse(sl, self);
        }
        if (this.deathTime >= SINK_START) {
            // Отключаем коллизию, иначе тело упирается в землю и не тонет, а просто
            // исчезает. С noPhysics оно проваливается СКВОЗЬ блоки под землю.
            self.noPhysics = true;
            self.setDeltaMovement(0.0D, 0.0D, 0.0D);
            self.setPos(self.getX(), self.getY() - 0.08D, self.getZ());
        }
        if (this.deathTime >= GONE && !self.isRemoved()) {
            sl.broadcastEntityEvent(self, (byte) 60); // ванильный «пуф» частиц при исчезновении
            self.remove(Entity.RemovalReason.KILLED);
        }
        ci.cancel();
    }
}
