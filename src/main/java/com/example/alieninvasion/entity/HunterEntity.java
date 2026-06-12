package com.example.alieninvasion.entity;

import com.example.alieninvasion.registry.EntityRegistry;
import com.example.alieninvasion.registry.ItemRegistry;
import com.example.alieninvasion.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * МАКС МАКСБЕТОВ — легендарный охотник на пришельцев. Приходит после
 * отступления роя, на пафосе раздаёт комплименты выжившим «бомжам» и вручает
 * свой реактор для уничтожения планеты Роя.
 *
 * НЕ ТРОГАТЬ ЕГО: 400 HP, топовая броня, мгновенно ломает стены 3x3, стреляет
 * всем инопланетным арсеналом, уворачивается от снарядов телепортом и жуёт
 * зачарованные яблоки. Если напасть — он вырежет весь сервер и спросит,
 * стоило ли оно того.
 */
public class HunterEntity extends PathfinderMob {

    // --- Сценарий мирного диалога ---
    private int dialogueStage = 0;     // 0=идёт к игроку, 1..N=реплики, 99=раздал и отдыхает
    private int talkTimer = 0;
    private boolean reactorGiven = false;
    private int idleQuipTimer = 0;
    // До какого gameTime занят «рот»: новая озвучка не стартует, пока не дозвучала
    // предыдущая (длительности файлов см. generate_hunter_voice.py).
    private long voiceUntil = 0L;

    // --- Боевой режим ---
    private boolean hostile = false;
    private int shootTimer = 0;
    private int breakTimer = 0;
    private int appleCooldown = 0;
    private int applesLeft = 4;
    // Тактический мозг: каждые 2.5-4 сек выбирает новый манёвр (блинк за спину,
    // разрыв дистанции с бурстом, стрейф по дуге, прыжок-удар, отход на лечение).
    private int tacticTimer = 0;
    private int strafeTicks = 0;
    private float strafeDir = 1.0F;
    private int tauntTimer = 0;
    private boolean saidLowHp = false;

    // ------------------------------------------------------------- РЕПЛИКИ
    private static final String[] IDLE_QUIPS = {
            "Чё смотрим? Планета сама себя не взорвёт.",
            "Я бы сам сходил, но мне лень. Заслужил отпуск, ёпт.",
            "Шевелитесь, бомжи. Реактор не вечный, и моё терпение тоже.",
            "Красивая у вас Земля. Была. Будет. Если не затупите.",
            "В мои годы рой выносили без брони. В одних трениках.",
            "Если потеряете реактор — со дна океана достанете. Зубами.",
            "Не благодарите. Хотя нет — благодарите.",
            "Главный улей. Реактор. Портал. Чё тут непонятного-то?",
            "Я однажды улей чихом завалил. Простуженный был.",
            "Время — деньги, бомжи. А вы тратите МОЁ время."
    };
    private static final String[] IDLE_QUIPS_AFTER = {
            "Хорошая работа, бомжи. Не ожидал. Серьёзно.",
            "Тихо как... Даже скучно. Может, где ещё рой остался?",
            "Видали салют? То-то же. Школа Максбетова.",
            "Ну всё, живите. Только Землю больше не теряйте, второй раз спасать не приду.",
            "Птички поют... Аж противно. Но красиво, да."
    };
    private static final String[] STARE_QUIPS = {
            "Чё уставился? Автограф не раздаю.",
            "Дыши в другую сторону, бомж.",
            "Ближе не подходи — от меня крутость не передаётся.",
            "Чё надо? Реактор я уже отдал."
    };
    private static final String[] ANGRY_LINES = {
            "Ты ЧЁ творишь, дебил?! НА МЕНЯ?! Я тебя сейчас обратно в каменный век отправлю!",
            "О-о-о, смелый нашёлся! Запомни этот момент — это была худшая идея в твоей жизни.",
            "НА МЕНЯ?! Рой смотрел на меня так же. Где теперь рой?"
    };
    private static final String[] DODGE_LINES = {
            "Промазал. Дальше что?",
            "Медленно. Очень медленно.",
            "Я такое ещё в детском саду уворачивал.",
            "Ты целишься или приветствуешь?"
    };
    private static final String[] COMBAT_TAUNTS = {
            "Ты ещё жив? Удивительно. Раздражающе, но удивительно.",
            "Побегай-побегай. Кардио тебе не повредит.",
            "Я даже не вспотел, бомжина.",
            "Может, сдашься? Шучу. Не может.",
            "Рой держался дольше. РОЙ, Карл!",
            "Это всё, на что способно человечество? Печально."
    };
    private static final String[] APPLE_LINES = {
            "*хрум* ...Вкусно. На чём мы остановились? А, точно — на твоих похоронах.",
            "*хрум* Витамины — основа крутости.",
            "*хрум* Секунду. Перекус. Даже не думай, что это твоя заслуга."
    };
    private static final String[] PLAYER_KILL_LINES = {
            "И стоило оно того, бомжина?",
            "Минус один умник. Кто следующий?",
            "Я же говорил. Я ВСЕГДА говорю.",
            "Передавай привет респауну.",
            "Это был урок. Бесплатный, между прочим.",
            "Лежи. Подумай о своём поведении."
    };
    private static final String[] ALIEN_KILL_LINES = {
            "Видали? ВОТ так это делается.",
            "Очередной в коллекцию.",
            "Даже не размялся, ёпт.",
            "Соскучился я по этому делу.",
            "Тыща первый. Или вторая тыща? Сбился.",
            "Шёл бы ты домой, жук. А, точно — дома у тебя скоро не будет."
    };

    private static String pick(net.minecraft.util.RandomSource random, String[] pool) {
        return pool[random.nextInt(pool.length)];
    }

    public HunterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§6Макс Максбетов"));
        this.setCustomNameVisible(true);
        this.xpReward = 500;
        // Топовая броня надета честно (видна на модели); лицо открыто — шлемы для
        // слабаков. Дроп экипировки выключен: наследство выдаёт die() по списку.
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST,
                new ItemStack(ItemRegistry.COSMIC_CHESTPLATE));
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS,
                new ItemStack(ItemRegistry.COSMIC_LEGGINGS));
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
                new ItemStack(ItemRegistry.COSMIC_BOOTS));
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                new ItemStack(ItemRegistry.NIBIRIUM_SWORD));
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        // 200 HP: живучесть теперь не в толщине, а в манёвре — блинки, уклонения,
        // стрейф и яблоки. Убить можно, но сначала попробуй в него попасть.
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0D)
                .add(Attributes.ARMOR, 20.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 12.0D)
                .add(Attributes.ATTACK_DAMAGE, 14.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 80.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25D, true));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        // Игроков — только в режиме мести; пришельцев — ВСЕГДА: он охотник, он этим живёт.
        this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> this.hostile && !((Player) p).isCreative() && !p.isSpectator()));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class,
                10, true, false, e -> AlienUtils.isAlliedTo(null, e)));
    }

    // ------------------------------------------------------------------ AI

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (appleCooldown > 0) appleCooldown--;

        // Зачарованные яблоки — его перекус в любом режиме.
        if (this.getHealth() < this.getMaxHealth() * 0.55F && applesLeft > 0 && appleCooldown <= 0) {
            eatEnchantedApple(sl);
        }

        // Весь арсенал работает против ЛЮБОЙ цели — и против роя, и против
        // обнаглевших игроков: стрельба, проломы стен, телепорт за спину.
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            abilityTick(sl, target);
        }

        if (hostile) {
            retargetPlayers(sl);
        } else {
            cinematicTick(sl);
        }
    }

    // ---------------------------------------------------------- МИРНЫЙ АКТ

    private void cinematicTick(ServerLevel sl) {
        // Пока идёт бой с пришельцами — не до разговоров.
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            return;
        }
        Player nearest = sl.getNearestPlayer(this, 96.0D);

        if (dialogueStage == 0) {
            if (nearest == null) {
                return;
            }
            this.getLookControl().setLookAt(nearest, 30.0F, 30.0F);
            if (this.distanceTo(nearest) > 5.0D) {
                this.getNavigation().moveTo(nearest, 1.0D);
            } else {
                this.getNavigation().stop();
                dialogueStage = 1;
                talkTimer = 0;
            }
            return;
        }

        if (dialogueStage >= 1 && dialogueStage <= 5) {
            if (nearest != null) {
                this.getLookControl().setLookAt(nearest, 30.0F, 30.0F);
            }
            talkTimer++;
            // Темп подогнан под длительность озвучки: реплики в чате появляются
            // примерно там, где голос их произносит, и НИКОГДА не перебивают друг друга.
            int needed = switch (dialogueStage) {
                case 1 -> 20;   // подошёл — поздоровался
                case 2 -> 150;  // середина hello.ogg («Я такие рои в одиночку гасил...»)
                case 3 -> 140;  // hello.ogg дозвучал
                case 4 -> 80;   // пауза перед вручением
                case 5 -> 170;  // середина gift.ogg («Тащите через портал...»)
                default -> 70;
            };
            boolean voiceBusy = sl.getGameTime() < this.voiceUntil;
            if (talkTimer < needed || ((dialogueStage == 1 || dialogueStage == 4) && voiceBusy)) {
                return;
            }
            talkTimer = 0;
            int players = sl.getServer().getPlayerList().getPlayerCount();
            switch (dialogueStage) {
                case 1 -> {
                    if (players <= 1) {
                        say(sl, "Ну хоть кто-то выжил. Один. ОДИН, ёпрст! Вся планета — и один бомж в драных портках.");
                    } else if (players <= 3) {
                        say(sl, "Ну хоть кто-то выжил. Целых " + players + " калеки. Лучшее, что осталось у человечества, бл*ха...");
                    } else {
                        say(sl, "О, целая армия бомжей — " + players + " рыл! Рой небось со смеху передох, а не от ваших зубочисток.");
                    }
                    voice(sl, ModSounds.HUNTER_HELLO);
                }
                case 2 -> say(sl, "Я такие рои в одиночку гасил, пока вы по землянкам сидели и в штаны откладывали.");
                case 3 -> say(sl, "Мать Роя завалили? Хм. Неплохо для бомжей. Я свою первую голыми руками порвал. Ну... почти голыми.");
                case 4 -> {
                    say(sl, "Ладно, хорош сопли жевать. Сделайте хоть что-то полезное в жизни — вот вам МОЙ реактор. НАСТОЯЩАЯ пушка, не ваши палки.");
                    voice(sl, ModSounds.HUNTER_GIFT);
                    giveReactor(sl, nearest);
                }
                case 5 -> {
                    say(sl, "Тащите его через портал, ставьте у ГЛАВНОГО улья и валите со всех ног. Минута сорок — и их поганый шарик станет салютом. Не обл*жайтесь.");
                    dialogueStage = 99;
                    return;
                }
            }
            dialogueStage++;
            return;
        }

        // Раздал — отдыхает рядом и иногда подгоняет (не перебивая сам себя).
        if (dialogueStage == 99) {
            idleQuipTimer++;
            // Залип рядом вплотную — отдельные подколки за личное пространство.
            if (nearest != null && this.distanceTo(nearest) < 2.5D
                    && idleQuipTimer >= 300 && sl.random.nextInt(80) == 0) {
                idleQuipTimer = 0;
                say(sl, pick(sl.random, STARE_QUIPS));
                return;
            }
            if (idleQuipTimer >= 1200 && sl.getGameTime() >= this.voiceUntil) {
                idleQuipTimer = 0;
                // После взрыва планеты — другой настрой: подгонять больше некого.
                boolean planetGone = sl.dimension() == net.minecraft.world.level.Level.OVERWORLD
                        && com.example.alieninvasion.world.InvasionManager.get(sl).isPlanetDestroyed();
                say(sl, pick(sl.random, planetGone ? IDLE_QUIPS_AFTER : IDLE_QUIPS));
                voice(sl, ModSounds.HUNTER_IDLE);
            }
        }
    }

    private void giveReactor(ServerLevel sl, Player to) {
        if (reactorGiven) {
            return;
        }
        reactorGiven = true;
        Vec3 at = to != null ? to.position() : this.position();
        Vec3 dir = at.subtract(this.position());
        Vec3 vel = dir.lengthSqr() > 0.01D ? dir.normalize().scale(0.4D) : Vec3.ZERO;
        ItemEntity drop = new ItemEntity(sl, this.getX(), this.getY() + 1.2D, this.getZ(),
                new ItemStack(com.example.alieninvasion.registry.ModBlocks.PLANET_REACTOR.asItem()));
        drop.setDeltaMovement(vel.x, 0.3D, vel.z);
        drop.setUnlimitedLifetime();
        sl.addFreshEntity(drop);
        sl.playSound(null, this.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 1.0F, 0.6F);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, this.getX(), this.getY() + 1.5D, this.getZ(),
                20, 0.5D, 0.5D, 0.5D, 0.2D);
    }

    // ---------------------------------------------------------- БОЕВОЙ АКТ

    private void becomeHostile(ServerLevel sl, LivingEntity attacker) {
        if (hostile) {
            return;
        }
        hostile = true;
        say(sl, pick(sl.random, ANGRY_LINES));
        voice(sl, ModSounds.HUNTER_ANGRY);
        sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 1.2F);
        if (attacker instanceof Player p) {
            this.setTarget(p);
        }
        // Если реактор ещё не вручён — он его всё равно не зажмёт... но получить
        // его теперь можно только с трупа.
    }

    /** В режиме мести он не успокаивается: цель кончилась — ищет следующего игрока. */
    private void retargetPlayers(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || (target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            Player next = sl.getNearestPlayer(this, 80.0D);
            if (next != null && !next.isCreative() && !next.isSpectator()) {
                this.setTarget(next);
            }
        }
    }

    private void abilityTick(ServerLevel sl, LivingEntity target) {
        this.setSprinting(this.distanceTo(target) > 6.0D);

        // ВЕСЬ инопланетный арсенал: плазма, кислота, тяжёлая радиация.
        shootTimer++;
        if (shootTimer >= 25 && this.distanceTo(target) > 3.0D && this.hasLineOfSight(target)) {
            shootTimer = 0;
            Projectile bolt;
            float roll = sl.random.nextFloat();
            if (roll < 0.4F) {
                bolt = new PlasmaBoltEntity(sl, this);
            } else if (roll < 0.7F) {
                bolt = new AcidBoltEntity(sl, this);
            } else {
                bolt = new RadiationBoltEntity(sl, this, true);
            }
            Vec3 aim = new Vec3(target.getX() - this.getX(),
                    target.getEyeY() - this.getEyeY(),
                    target.getZ() - this.getZ());
            bolt.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
            bolt.shoot(aim.x, aim.y, aim.z, 2.2F, 2.0F);
            sl.addFreshEntity(bolt);
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND); // как игрок
            sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2F, 1.4F);
        }

        // Стены его не останавливают: мгновенно прогрызает проход 3x3.
        breakTimer++;
        if (breakTimer >= 12 && !this.hasLineOfSight(target)) {
            breakTimer = 0;
            breakWallTowards(sl, target);
        }

        // Стрейф по дуге: пока активен — кружит вокруг цели, не сводя с неё глаз.
        if (strafeTicks > 0) {
            strafeTicks--;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.getMoveControl().strafe(0.4F, strafeDir);
        }

        // ПвП-подскоки в ближнем бою — двигается как игрок, а не как зомби.
        if (this.onGround() && this.distanceTo(target) < 5.5D && sl.random.nextInt(18) == 0) {
            this.getJumpControl().jump();
        }

        // ТАКТИКА: постоянная смена манёвра, никакого «просто иду и бью».
        tacticTimer--;
        if (tacticTimer <= 0) {
            tacticTimer = 50 + sl.random.nextInt(30);
            chooseTactic(sl, target);
        }

        // Подколки по ходу затянувшейся драки с игроками.
        if (hostile && target instanceof Player) {
            tauntTimer++;
            if (tauntTimer >= 300 + sl.random.nextInt(160) && sl.getGameTime() >= this.voiceUntil) {
                tauntTimer = 0;
                say(sl, pick(sl.random, COMBAT_TAUNTS));
            }
        }
    }

    /** Выбор следующего манёвра — он непредсказуем и не стоит на месте. */
    private void chooseTactic(ServerLevel sl, LivingEntity target) {
        double dist = this.distanceTo(target);

        // Мало HP — разорвать дистанцию и перекусить (яблоко съест общий тик).
        if (this.getHealth() < this.getMaxHealth() * 0.3F && applesLeft > 0) {
            blinkAway(sl, target, 11.0D);
            return;
        }
        // Цель сбежала далеко или окопалась наверху — он уже за спиной.
        if (dist > 16.0D || target.getY() > this.getY() + 4.0D) {
            blinkBehind(sl, target);
            return;
        }

        // Толпа вокруг — время для удара по площади.
        if (countSplashTargets(sl, 4.5D) >= 2 && sl.random.nextInt(100) < 35) {
            groundSlam(sl);
            return;
        }

        int roll = sl.random.nextInt(100);
        if (roll < 35) {
            blinkBehind(sl, target);
        } else if (roll < 55 && dist < 8.0D) {
            blinkAway(sl, target, 8.0D);
            burstFire(sl, target, 3);
        } else if (roll < 80) {
            // Зайти сбоку: 1.5-2.5 сек стрейфа по случайной дуге.
            strafeTicks = 30 + sl.random.nextInt(20);
            strafeDir = sl.random.nextBoolean() ? 0.9F : -0.9F;
        } else if (dist > 3.5D && dist < 13.0D) {
            leapAt(sl, target);
        } else {
            strafeTicks = 20;
            strafeDir = sl.random.nextBoolean() ? 0.9F : -0.9F;
        }
    }

    /** Блинк за спину цели + мгновенный удар, если дотягивается. */
    private void blinkBehind(ServerLevel sl, LivingEntity target) {
        Vec3 behind = target.position().subtract(target.getLookAngle().scale(2.2D));
        if (safeBlink(sl, behind.x, target.getY(), behind.z)) {
            this.getLookControl().setLookAt(target, 60.0F, 60.0F);
            if (this.distanceTo(target) < 3.2D) {
                this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                this.doHurtTarget(target);
            }
        }
    }

    /** Разрыв дистанции: отскакивает на radius блоков в случайную сторону от цели. */
    private void blinkAway(ServerLevel sl, LivingEntity target, double radius) {
        double a = sl.random.nextDouble() * Math.PI * 2.0D;
        safeBlink(sl, target.getX() + Math.cos(a) * radius, target.getY(), target.getZ() + Math.sin(a) * radius);
        this.getLookControl().setLookAt(target, 60.0F, 60.0F);
    }

    /** Быстрый бурст из малых болтов — наказание за попытку сблизиться. */
    private void burstFire(ServerLevel sl, LivingEntity target, int count) {
        for (int i = 0; i < count; i++) {
            RadiationBoltEntity bolt = new RadiationBoltEntity(sl, this, false);
            Vec3 aim = new Vec3(target.getX() - this.getX(),
                    target.getEyeY() - this.getEyeY(),
                    target.getZ() - this.getZ());
            bolt.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
            bolt.shoot(aim.x, aim.y, aim.z, 2.4F, 4.0F);
            sl.addFreshEntity(bolt);
        }
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.0F, 1.8F);
    }

    /** Прыжок-удар: бросается на цель по дуге, как раптор. */
    private void leapAt(ServerLevel sl, LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position());
        Vec3 flat = new Vec3(dir.x, 0.0D, dir.z);
        if (flat.lengthSqr() < 0.01D) {
            return;
        }
        flat = flat.normalize().scale(Math.min(1.1D, dir.horizontalDistance() * 0.18D));
        this.setDeltaMovement(flat.x, 0.55D, flat.z);
        this.hurtMarked = true;
        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.8F);
    }

    /**
     * Сплеш-цели: пришельцы — всегда; игроки — только в режиме мести (и никогда
     * креатив/спектатор). Мирный Макс, рубящий рой возле игрока, не заденет его.
     */
    private boolean isSplashTarget(LivingEntity e, net.minecraft.world.entity.Entity primary) {
        if (e == this || e == primary || !e.isAlive()) {
            return false;
        }
        if (e instanceof Player p) {
            return hostile && !p.isCreative() && !p.isSpectator();
        }
        return AlienUtils.isAlliedTo(null, e);
    }

    private int countSplashTargets(ServerLevel sl, double radius) {
        return sl.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
                e -> isSplashTarget(e, null)).size();
    }

    /** Удар по площади: обрушивает меч в землю — волна расшвыривает всех вокруг. */
    private void groundSlam(ServerLevel sl) {
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.85F;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(4.5D), en -> isSplashTarget(en, null))) {
            e.hurt(this.damageSources().mobAttack(this), damage);
            e.knockback(0.8D, this.getX() - e.getX(), this.getZ() - e.getZ());
        }
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12.0D;
            sl.sendParticles(ParticleTypes.CLOUD,
                    this.getX() + Math.cos(a) * 2.5D, this.getY() + 0.3D, this.getZ() + Math.sin(a) * 2.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.15D);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5D, this.getZ(), 2, 0.4D, 0.2D, 0.4D, 0.0D);
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 1.5F);
        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.7F);
        if (sl.random.nextInt(4) == 0) {
            say(sl, "РАЗОЙДИСЬ, МЕЛОЧЬ!");
        }
    }

    /** Сплеш на обычном ударе: нибириевый меч рубит по дуге, достаётся всем рядом. */
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && this.level() instanceof ServerLevel sl) {
            float splash = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6F;
            int struck = 0;
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                    target.getBoundingBox().inflate(2.5D), en -> isSplashTarget(en, target))) {
                e.hurt(this.damageSources().mobAttack(this), splash);
                e.knockback(0.4D, this.getX() - e.getX(), this.getZ() - e.getZ());
                struck++;
            }
            if (struck > 0) {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        (this.getX() + target.getX()) / 2.0D, target.getY() + 1.0D,
                        (this.getZ() + target.getZ()) / 2.0D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.HOSTILE, 1.0F, 1.0F);
            }
        }
        return hit;
    }

    /**
     * Безопасный блинк через randomTeleport (сам ищет место без блоков);
     * с частицами и звуком с обеих сторон.
     */
    private boolean safeBlink(ServerLevel sl, double x, double y, double z) {
        sl.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0D, this.getZ(), 25, 0.3D, 0.8D, 0.3D, 0.25D);
        boolean ok = this.randomTeleport(x, y, z, false);
        if (ok) {
            sl.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.3F, 1.1F);
            sl.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY() + 1.0D, this.getZ(), 25, 0.3D, 0.8D, 0.3D, 0.25D);
        }
        return ok;
    }

    private void breakWallTowards(ServerLevel sl, LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position());
        Vec3 flat = new Vec3(dir.x, 0.0D, dir.z);
        if (flat.lengthSqr() < 0.01D) {
            flat = new Vec3(this.getLookAngle().x, 0.0D, this.getLookAngle().z);
        }
        flat = flat.normalize();
        BlockPos front = BlockPos.containing(this.getX() + flat.x * 1.2D, this.getY(), this.getZ() + flat.z * 1.2D);
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        boolean broke = false;
        for (int dy = 0; dy <= 2; dy++) {
            for (int ds = -1; ds <= 1; ds++) {
                BlockPos p = front.offset((int) Math.round(side.x * ds), dy, (int) Math.round(side.z * ds));
                var state = sl.getBlockState(p);
                if (state.isAir() || state.getDestroySpeed(sl, p) < 0.0F) {
                    continue; // бедрок и порталы ему не по зубам
                }
                if (state.is(com.example.alieninvasion.registry.ModBlocks.PLANET_REACTOR)) {
                    continue; // свой реактор не трогает
                }
                sl.destroyBlock(p, false, this);
                broke = true;
            }
        }
        if (broke) {
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND); // как игрок
            sl.playSound(null, front, SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.5F, 0.7F);
            sl.sendParticles(ParticleTypes.CRIT, front.getX() + 0.5D, front.getY() + 1.0D, front.getZ() + 0.5D,
                    20, 1.0D, 1.0D, 1.0D, 0.3D);
        }
    }

    private void eatEnchantedApple(ServerLevel sl) {
        applesLeft--;
        appleCooldown = 300;
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 1, false, true));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 0, false, true));
        sl.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 1.2F, 0.9F);
        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.NEUTRAL, 1.0F, 0.8F);
        sl.sendParticles(new net.minecraft.core.particles.ItemParticleOption(ParticleTypes.ITEM,
                        new ItemStack(Items.ENCHANTED_GOLDEN_APPLE)),
                this.getX(), this.getEyeY(), this.getZ(), 12, 0.3D, 0.3D, 0.3D, 0.1D);
        if (hostile) {
            say(sl, pick(sl.random, APPLE_LINES));
        }
    }

    // ------------------------------------------------------------- РЕАКЦИИ

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel sl)) {
            return super.hurt(source, amount);
        }
        // Хитрый: от трети снарядов просто уходит телепортом.
        if (source.getDirectEntity() instanceof Projectile && this.random.nextFloat() < 0.3F) {
            double a = this.random.nextDouble() * Math.PI * 2.0D;
            safeBlink(sl, this.getX() + Math.cos(a) * 4.0D, this.getY(), this.getZ() + Math.sin(a) * 4.0D);
            // Подкалывает только игроков — на снаряды роя не отвлекается.
            if (source.getEntity() instanceof Player && this.random.nextInt(3) != 0) {
                say(sl, pick(sl.random, DODGE_LINES));
            }
            return false;
        }
        boolean result = super.hurt(source, amount);
        // Получил по лицу в ближке — иногда тут же отскакивает вбок (урон прошёл,
        // но добивающую серию по нему не прокрутишь).
        if (result && !(source.getDirectEntity() instanceof Projectile)
                && source.getEntity() instanceof LivingEntity && this.random.nextFloat() < 0.2F) {
            double a = this.random.nextDouble() * Math.PI * 2.0D;
            safeBlink(sl, this.getX() + Math.cos(a) * 5.0D, this.getY(), this.getZ() + Math.sin(a) * 5.0D);
        }
        // Первый раз продавили ниже половины — он впервые воспринимает бой всерьёз.
        if (result && hostile && !saidLowHp && this.getHealth() < this.getMaxHealth() * 0.5F) {
            saidLowHp = true;
            say(sl, "Хм. А вы не безнадёжны... Ладно. ИГРАЕМ ПО-ВЗРОСЛОМУ.");
            sl.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 1.6F);
        }
        if (result && !hostile) {
            LivingEntity attacker = source.getEntity() instanceof LivingEntity le ? le : null;
            if (attacker instanceof Player p && !p.isCreative() && !p.isSpectator()) {
                becomeHostile(sl, attacker);
            }
        }
        return result;
    }

    @Override
    public boolean killedEntity(ServerLevel sl, LivingEntity victim) {
        boolean r = super.killedEntity(sl, victim);
        if (victim instanceof Player) {
            say(sl, pick(sl.random, PLAYER_KILL_LINES));
            voice(sl, ModSounds.HUNTER_KILL);
        } else if (AlienUtils.isAlliedTo(null, victim) && sl.random.nextInt(4) == 0) {
            say(sl, pick(sl.random, ALIEN_KILL_LINES));
        }
        return r;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        // Невозможное случилось. Уважение — и предсмертная передача дел.
        say(sl, "Хех... красиво вышло... *кхе*... Реактор забери, бродяга. Закончи... начатое... Теперь ТЫ тут самый крутой...");
        voice(sl, ModSounds.HUNTER_DEATH);
        AlienUtils.broadcastTitle(sl,
                Component.literal("§6МАКС МАКСБЕТОВ ПАЛ"),
                Component.literal("§eЛегенды тоже смертны. Закончите начатое."));
        for (int i = 0; i < 3; i++) {
            net.minecraft.world.entity.LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(this.getX() + (i - 1) * 4.0D, this.getY(), this.getZ());
                bolt.setVisualOnly(true);
                sl.addFreshEntity(bolt);
            }
        }
        // Наследство: реактор (если зажал), жетон, его перекус и тёмная материя.
        if (!reactorGiven) {
            this.spawnAtLocation(new ItemStack(com.example.alieninvasion.registry.ModBlocks.PLANET_REACTOR.asItem()));
        }
        this.spawnAtLocation(new ItemStack(ItemRegistry.HUNTER_TOKEN));
        this.spawnAtLocation(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, Math.max(1, applesLeft)));
        this.spawnAtLocation(new ItemStack(ItemRegistry.DARK_MATTER_SHARD, 5));
    }

    // ------------------------------------------------------------- ПРОЧЕЕ

    private void say(ServerLevel sl, String text) {
        sl.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("§6[Макс Максбетов] §f" + text), false);
    }

    private void voice(ServerLevel sl, SoundEvent sound) {
        sl.playSound(null, this.blockPosition(), sound, SoundSource.NEUTRAL, 2.5F, 1.0F);
        this.voiceUntil = sl.getGameTime() + voiceTicks(sound);
    }

    /** Длительности озвучки в тиках (замерено по ogg + небольшой зазор). */
    private static int voiceTicks(SoundEvent sound) {
        if (sound == ModSounds.HUNTER_HELLO) return 280;
        if (sound == ModSounds.HUNTER_GIFT)  return 390;
        if (sound == ModSounds.HUNTER_ANGRY) return 200;
        if (sound == ModSounds.HUNTER_KILL)  return 85;
        if (sound == ModSounds.HUNTER_DEATH) return 250;
        if (sound == ModSounds.HUNTER_IDLE)  return 135;
        return 100;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("DialogueStage", dialogueStage);
        tag.putBoolean("ReactorGiven", reactorGiven);
        tag.putBoolean("Hostile", hostile);
        tag.putInt("ApplesLeft", applesLeft);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dialogueStage = tag.getInt("DialogueStage");
        this.reactorGiven = tag.getBoolean("ReactorGiven");
        this.hostile = tag.getBoolean("Hostile");
        this.applesLeft = tag.contains("ApplesLeft") ? tag.getInt("ApplesLeft") : 4;
    }
}
