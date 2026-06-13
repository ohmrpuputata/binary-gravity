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
 * НЕ ТРОГАТЬ ЕГО: живучесть у него не в мясе, а в МАСТЕРСТВЕ. HP немного, броня
 * средняя — но он почти не стоит на месте: блинкает за спину, рвёт дистанцию
 * рывком, веером кроет уклонение, парирует ближний бой контрударом, ловит
 * снаряды телепортом и жуёт зачарованные яблоки. Если напасть — он вырежет весь
 * сервер и спросит, стоило ли оно того. Убить можно. Попасть — вот это вряд ли.
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
    // Экипировку зачаровываем при первом серверном тике (в конструкторе registry ещё не готов).
    private boolean gearEnchanted = false;
    private int hazardCooldown = 0;  // антиспам реакции на опасную среду (лава/огонь/падение)
    private int buildCooldown = 0;   // антиспам инженерных приёмов (стена/укрытие/столб)
    private int flurryTicks = 0;     // вихрь клинка: длящийся AoE вокруг
    private int orbitalTimer = 0;    // телеграф орбитального удара (тикает до удара)
    private double orbitalX, orbitalZ; // отмеченная точка орбитального удара
    // Адаптивный ИИ: давление дальнего/ближнего боя (затухает). По перекосу он
    // подстраивает стиль — расстреливают, сокращает дистанцию; лезут в ближний, парирует.
    private int rangedPressure = 0;
    private int meleePressure = 0;
    private int auraTick = 0;        // фаза постоянной боевой ауры
    private int webCooldown = 0;     // антиспам паутины-ловушки
    private int tauntCrouch = 0;     // таймер троллинг-приседа над почти убитой целью
    private int tauntCooldown = 0;   // антиспам троллинга

    // ------------------------------------------------------------- РЕПЛИКИ
    private static final String[] IDLE_QUIPS = {
            "Чё смотрим? Планета сама себя не взорвёт.",
            "Я бы сам сходил, но мне лень. Заслужил отпуск, ёпт.",
            "Шевелитесь, бомжи. Реактор не вечный, и моё терпение тоже.",
            "Красивая у вас Земля. Была. Будет. Если не затупите.",
            "В мои годы рой выносили без брони. В одних трениках.",
            "Если потеряете реактор — со дна океана достанете. Зубами.",
            "Не благодарите. Хотя нет — благодарите.",
            "Город Роя. Реактор. Портал. Чё тут непонятного-то?",
            "Я однажды улей чихом завалил. Простуженный был.",
            "Время — деньги, бомжи. А вы тратите МОЁ время.",
            "Стою тут, как памятник самому себе. Заслуженно, между прочим.",
            "Видите этот город? Скоро это будет кратер. С моим автографом.",
            "Профессионализм, бомжи, — это когда работаешь меньше, а делаешь больше.",
            "Я не считаю убитых. Их считают ОНИ. Точнее, считали.",
            "Запомните этот силуэт. Будете внукам рассказывать.",
            "Реактор в центр города. Бомба. Бах. Это даже вы не испортите. Наверное."
    };
    private static final String[] IDLE_QUIPS_AFTER = {
            "Хорошая работа, бомжи. Не ожидал. Серьёзно.",
            "Тихо как... Даже скучно. Может, где ещё рой остался?",
            "Видали салют? То-то же. Школа Максбетова.",
            "Ну всё, живите. Только Землю больше не теряйте, второй раз спасать не приду.",
            "Птички поют... Аж противно. Но красиво, да.",
            "Целый город Роя — и тот в труху. А вы боялись.",
            "Вот теперь можно и на пенсию. Лет на пять. Ну, на год.",
            "Запишите в учебники: человечество спас один Макс. Вы так, массовка."
    };
    private static final String[] STARE_QUIPS = {
            "Чё уставился? Автограф не раздаю.",
            "Дыши в другую сторону, бомж.",
            "Ближе не подходи — от меня крутость не передаётся.",
            "Чё надо? Реактор я уже отдал.",
            "Любуйся издалека, как все нормальные люди.",
            "Ещё шаг — и будешь любоваться из реанимации."
    };
    private static final String[] ANGRY_LINES = {
            "Ты ЧЁ творишь, дебил?! НА МЕНЯ?! Я тебя сейчас обратно в каменный век отправлю!",
            "О-о-о, смелый нашёлся! Запомни этот момент — это была худшая идея в твоей жизни.",
            "НА МЕНЯ?! Рой смотрел на меня так же. Где теперь рой?",
            "Серьёзно? Я спас твою планету, а ты мне в спину? Ну держись, неблагодарный.",
            "Окей. ОКЕЙ. Ты сам напросился на мастер-класс."
    };
    private static final String[] DODGE_LINES = {
            "Промазал. Дальше что?",
            "Медленно. Очень медленно.",
            "Я такое ещё в детском саду уворачивал.",
            "Ты целишься или приветствуешь?",
            "Мимо. Как и вся твоя жизнь.",
            "Я тут, бомж. Нет, уже тут. И снова не угадал.",
            "Это была твоя лучшая попытка? Запиши, переживать будешь."
    };
    private static final String[] COMBAT_TAUNTS = {
            "Ты ещё жив? Удивительно. Раздражающе, но удивительно.",
            "Побегай-побегай. Кардио тебе не повредит.",
            "Я даже не вспотел, бомжина.",
            "Может, сдашься? Шучу. Не может.",
            "Рой держался дольше. РОЙ, Карл!",
            "Это всё, на что способно человечество? Печально.",
            "Я дерусь вполсилы. В четверть. Ладно, я просто разминаюсь.",
            "Ты бьёшь туда, где я БЫЛ. Учись бить туда, где буду.",
            "Каждый твой удар я вижу за три хода. Это даже неспортивно."
    };
    private static final String[] APPLE_LINES = {
            "*хрум* ...Вкусно. На чём мы остановились? А, точно — на твоих похоронах.",
            "*хрум* Витамины — основа крутости.",
            "*хрум* Секунду. Перекус. Даже не думай, что это твоя заслуга.",
            "*хрум* Перерыв на обед. Не стесняйся, бей. А, ты и так не можешь."
    };
    private static final String[] PLAYER_KILL_LINES = {
            "И стоило оно того, бомжина?",
            "Минус один умник. Кто следующий?",
            "Я же говорил. Я ВСЕГДА говорю.",
            "Передавай привет респауну.",
            "Это был урок. Бесплатный, между прочим.",
            "Лежи. Подумай о своём поведении.",
            "Школа закрыта. Урок усвоен посмертно.",
            "Я бы сказал «ничего личного», но нет. Личное."
    };
    private static final String[] ALIEN_KILL_LINES = {
            "Видали? ВОТ так это делается.",
            "Очередной в коллекцию.",
            "Даже не размялся, ёпт.",
            "Соскучился я по этому делу.",
            "Тыща первый. Или вторая тыща? Сбился.",
            "Шёл бы ты домой, жук. А, точно — дома у тебя скоро не будет.",
            "Один взмах — один жук. Математика уровня бог.",
            "И передайте Матери Роя... а, ну да. Уже некому."
    };
    // Реплики под новые приёмы — он комментирует собственное мастерство.
    private static final String[] DASH_LINES = {
            "Дистанция? Не существует.",
            "Моргнул — а я уже у тебя на лице.",
            "Слишком далеко стоял. Исправил.",
            "Рывок Максбетова. Патент мой."
    };
    private static final String[] PARRY_LINES = {
            "Не-а. Так не пойдёт.",
            "Парирование уровня «легенда».",
            "Ударил? А теперь моя очередь, по правилам.",
            "Спасибо за открытую спину, бомж."
    };
    private static final String[] EXECUTE_LINES = {
            "Финальный аккорд.",
            "Это было предсказуемо. Как и финал.",
            "Конец сета. 6:0.",
            "Запомни: профи не промахиваются дважды."
    };
    // Реакция на опасную среду — он НЕ истукан, который стоит в лаве.
    private static final String[] HAZARD_LINES = {
            "Опа. Пол горит. Я не на пикнике.",
            "Лава? Серьёзно? Я тебе что, турист?",
            "Отошёл. Профи не геройствуют в огне.",
            "Ой, всё. Тут жарковато, отойду-ка.",
            "Чуть не вляпался. КЛЮЧЕВОЕ — чуть."
    };
    // Тактическое отступление — это не бегство, это перегруппировка.
    private static final String[] RETREAT_LINES = {
            "Тактический отход. Запиши: это не бегство.",
            "Перегруппировка, бомж. Учись, пока я добрый.",
            "Дам тебе фору. Секунду. Наслаждайся.",
            "Шаг назад — два шага к твоим похоронам."
    };
    // Инженерные приёмы — он не только машет мечом.
    private static final String[] BUILD_LINES = {
            "Стенку поставлю. Подумай о жизни пока.",
            "Фортификация уровня «бог». Учись.",
            "Я и строитель, и могильщик. Универсал.",
            "Минутку, окопаюсь. Тебе же хуже."
    };
    private static final String[] YANK_LINES = {
            "Куда собрался? Мы не закончили.",
            "А ну иди сюда, бомж.",
            "От меня не убегают. От меня падают.",
            "Поводок короткий. Проверим?"
    };
    private static final String[] ORBITAL_LINES = {
            "Отойди оттуда. Или нет — мне же проще.",
            "Сейчас рванёт. Считай до... поздно.",
            "Небесная кара. По предоплате.",
            "Три... два... а, чё тянуть."
    };
    private static final String[] FLURRY_LINES = {
            "ВИХРЬ! Школа Максбетова, мелкие!",
            "Кручусь-верчусь, всех нашинковать хочу.",
            "Подойдите ближе. ВСЕМ хватит.",
            "Это не танец. Хотя красиво, да."
    };
    private static final String[] DEFLECT_LINES = {
            "Вернул. С процентами.",
            "Спасибо за патрон. Держи обратно.",
            "Твоё? Забери.",
            "Пинг-понг, бомж. Я подаю."
    };
    private static final String[] THREAT_LINES = {
            "О, броня поблёскивает. Ты тут главный? Тобой и займусь.",
            "Самый упакованный — самый интересный. Иди сюда.",
            "Ты за главного? Был.",
            "Лучшая броня на сервере? Снимем вместе с тобой."
    };
    private static final String[] WEB_LINES = {
            "Застрянь-ка, бомж.",
            "Паутинка. Посиди, подумай о жизни.",
            "Тебе ноги там, куда идёшь, не понадобятся.",
            "Прилип? Вот и славно."
    };
    private static final String[] TAUNT_LINES = {
            "Что, уже всё? Дай-ка насладиться моментом.",
            "Присяду на твоей могилке. Традиция такая.",
            "Не вставай, не вставай. Я быстро.",
            "Это даже не спорт. Но до чего приятно.",
            "Смотри внимательно — учись проигрывать с достоинством."
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
        // слабаков. Зачаровывается при первом серверном тике (enchantGear), т.к. в
        // конструкторе реестр чар ещё недоступен. Дроп выключен: наследство — в die().
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
        // 80 HP — он намеренно ХРУПКИЙ по «мясу». Живучесть держится на трёх китах:
        // зачарованная броня (Защита IV сильно режет урон), манёвр (блинки, рывки,
        // парирование, уклонения) и яблоки. Урон высокий (мастерство). Хочешь убить —
        // сперва попади и пробей броню. А он не стоит на месте ни секунды.
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 80.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 6.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.42D)
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
        if (hazardCooldown > 0) hazardCooldown--;
        if (buildCooldown > 0) buildCooldown--;
        if (webCooldown > 0) webCooldown--;
        if (tauntCooldown > 0) tauntCooldown--;

        // Давление боя медленно затухает — адаптация реагирует на СВЕЖую тактику.
        if ((this.tickCount & 31) == 0) {
            if (rangedPressure > 0) rangedPressure--;
            if (meleePressure > 0) meleePressure--;
        }

        // БОЕВАЯ АУРА: пока враждебен — крутящиеся искры и язычки пламени, заряжен.
        if (hostile) {
            double ang = (++auraTick) * 0.5D;
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    this.getX() + Math.cos(ang) * 0.85D, this.getY() + 0.4D + (auraTick % 5) * 0.35D,
                    this.getZ() + Math.sin(ang) * 0.85D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            if (this.tickCount % 6 == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0D, this.getZ(),
                        1, 0.3D, 0.5D, 0.3D, 0.01D);
            }
        }

        // Экипировка зачаровывается один раз — в конструкторе реестр чар недоступен.
        if (!gearEnchanted) {
            gearEnchanted = true;
            enchantGear(sl);
        }

        // Он НЕ истукан в лаве: горит, увяз, стоит на магме, тонет — уходит на сушу.
        if (hazardCooldown <= 0 && inDanger(sl)) {
            hazardCooldown = 30;
            if (escapeHazard(sl) && this.random.nextInt(3) == 0) {
                say(sl, pick(sl.random, HAZARD_LINES));
            }
        }

        // Зачарованные яблоки — его перекус в любом режиме.
        if (this.getHealth() < this.getMaxHealth() * 0.55F && applesLeft > 0 && appleCooldown <= 0) {
            eatEnchantedApple(sl);
        }

        // Весь арсенал работает против ЛЮБОЙ цели — и против роя, и против
        // обнаглевших игроков: стрельба, проломы стен, телепорт за спину.
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive()) {
            abilityTick(sl, target);
        } else if (tauntCrouch > 0) {
            tauntCrouch = 0;              // цель исчезла во время издёвки — встать
            this.setShiftKeyDown(false);
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
                    say(sl, "Тащите его через портал, прямо в их СТОЛИЦУ, и ставьте в центре города — у главного шпиля. Потом валите со всех ног: минута сорок — и их поганый шарик станет салютом. Не обл*жайтесь.");
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
        // Вход в бой — заметная вспышка: взрыв искр и визуальная молния.
        sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 1.0D, this.getZ(), 3, 0.4D, 0.6D, 0.4D, 0.0D);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY() + 1.0D, this.getZ(), 45, 0.6D, 1.0D, 0.6D, 0.6D);
        net.minecraft.world.entity.LightningBolt flash = EntityType.LIGHTNING_BOLT.create(sl);
        if (flash != null) {
            flash.moveTo(this.getX(), this.getY(), this.getZ());
            flash.setVisualOnly(true);
            sl.addFreshEntity(flash);
        }
        if (attacker instanceof Player p) {
            this.setTarget(p);
        }
        // Если реактор ещё не вручён — он его всё равно не зажмёт... но получить
        // его теперь можно только с трупа.
    }

    /**
     * В режиме мести он не успокаивается: цель кончилась — ищет следующую. Но не
     * просто ближайшую, а САМУЮ ОПАСНУЮ — кто в лучшей броне (с поправкой на дистанцию).
     */
    private void retargetPlayers(ServerLevel sl) {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || (target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            Player best = null;
            double bestThreat = -1.0D;
            for (Player pl : sl.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(80.0D),
                    pp -> !pp.isCreative() && !pp.isSpectator() && pp.isAlive())) {
                double threat = pl.getArmorValue() * 2.0D - this.distanceTo(pl) * 0.1D;
                if (threat > bestThreat) {
                    bestThreat = threat;
                    best = pl;
                }
            }
            if (best != null) {
                boolean armored = best.getArmorValue() >= 15;
                this.setTarget(best);
                if (armored && sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
                    say(sl, pick(sl.random, THREAT_LINES));
                }
            }
        }
    }

    private void abilityTick(ServerLevel sl, LivingEntity target) {
        // ТРОЛЛИНГ: над почти убитой целью пару секунд приседает (teabag), потом добивает.
        if (tauntCrouch > 0) {
            tauntCrouch--;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 30.0F, 30.0F);
            this.setShiftKeyDown(((tauntCrouch / 3) & 1) == 0); // быстрый присед/встал
            if (tauntCrouch % 6 == 0) {
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 2.1D, this.getZ(),
                        3, 0.3D, 0.2D, 0.3D, 0.0D);
            }
            if (tauntCrouch == 0) {
                this.setShiftKeyDown(false);
                say(sl, pick(sl.random, EXECUTE_LINES));
                if (this.distanceTo(target) < 4.0D) {
                    this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    this.doHurtTarget(target);
                } else {
                    dashStrike(sl, target);
                }
            }
            return; // во время издёвки больше ничего не делает
        }
        // Цель почти мертва — шанс поиздеваться перед добиванием (только над игроками).
        if (hostile && target instanceof Player && target.getHealth() <= 6.0F
                && this.distanceTo(target) < 7.0D && tauntCooldown <= 0 && sl.random.nextInt(100) < 35) {
            tauntCrouch = 40;
            tauntCooldown = 300;
            say(sl, pick(sl.random, TAUNT_LINES));
            sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.HOSTILE, 0.8F, 0.6F);
            return;
        }

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

        // ВИХРЬ КЛИНКА: пока активен — стоит на месте и рубит всех в 3 блоках по дуге.
        if (flurryTicks > 0) {
            flurryTicks--;
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 40.0F, 40.0F);
            if (flurryTicks % 3 == 0) {
                float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.45F;
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                        this.getBoundingBox().inflate(3.2D), en -> isSplashTarget(en, null))) {
                    e.hurt(this.damageSources().mobAttack(this), dmg);
                    e.knockback(0.25D, this.getX() - e.getX(), this.getZ() - e.getZ());
                }
                for (int k = 0; k < 8; k++) {
                    double a = (flurryTicks + k) * 0.8D;
                    sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                            this.getX() + Math.cos(a) * 2.0D, this.getY() + 1.0D, this.getZ() + Math.sin(a) * 2.0D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 0.8F, 1.3F);
            }
        }

        // ОРБИТАЛЬНЫЙ УДАР: телеграф столбом частиц, затем взрыв по отмеченной точке.
        if (orbitalTimer > 0) {
            orbitalTimer--;
            for (int k = 1; k <= 6; k++) {
                sl.sendParticles(ParticleTypes.SCULK_SOUL, orbitalX, this.getY() + k, orbitalZ,
                        2, 0.25D, 0.25D, 0.25D, 0.0D);
            }
            if (orbitalTimer == 0) {
                orbitalStrikeHit(sl);
            }
        }

        // ПвП-подскоки в ближнем бою — двигается как игрок, а не как зомби.
        if (this.onGround() && this.distanceTo(target) < 5.5D && sl.random.nextInt(18) == 0) {
            this.getJumpControl().jump();
        }

        // ТАКТИКА: постоянная смена манёвра, никакого «просто иду и бью».
        tacticTimer--;
        if (tacticTimer <= 0) {
            tacticTimer = 32 + sl.random.nextInt(22); // чаще меняет манёвр — почти не стоит на месте
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

        // Критический HP — ТАКТИЧЕСКОЕ ОТСТУПЛЕНИЕ + ОКОП: рывок назад, стена между
        // собой и врагом, а при совсем критическом — укрытие, пока регенерация тикает.
        if (this.getHealth() < this.getMaxHealth() * 0.18F) {
            tacticalRetreat(sl, target);
            if (buildCooldown <= 0) {
                buildCooldown = 220;
                buildBunker(sl);
            }
            return;
        }
        // Мало HP — тактический отход с прикрытием стеной (перекус съест общий тик).
        if (this.getHealth() < this.getMaxHealth() * 0.35F) {
            tacticalRetreat(sl, target);
            return;
        }
        // АДАПТАЦИЯ: его расстреливают сильнее, чем бьют в ближнем — рвётся в упор.
        if (rangedPressure > meleePressure + 12 && dist > 5.0D && sl.random.nextInt(100) < 55) {
            if (sl.random.nextBoolean()) {
                dashStrike(sl, target);
            } else {
                yankTarget(sl, target);
            }
            return;
        }
        // Прессуют в ближнем сильнее, чем стреляют — разрывает дистанцию и бьёт издалека.
        if (meleePressure > rangedPressure + 12 && dist < 6.0D && sl.random.nextInt(100) < 55) {
            blinkAway(sl, target, 9.0D);
            if (sl.random.nextBoolean()) {
                homingVolley(sl, target);
            } else {
                shockNova(sl);
            }
            return;
        }
        // Наседают — оплести цель паутиной и отскочить, расстреливать застрявшего.
        if (dist >= 2.0D && dist <= 9.0D && webCooldown <= 0 && sl.random.nextInt(100) < 25) {
            webCooldown = 160;
            webTrap(sl, target);
            blinkAway(sl, target, 7.0D);
            burstFire(sl, target, 3);
            return;
        }
        // Зажали вплотную — иногда столбится вверх и поливает сверху (клатч в меру).
        if (dist < 4.0D && buildCooldown <= 0 && sl.random.nextInt(100) < 30) {
            buildCooldown = 170;
            pillarUp(sl, 4 + sl.random.nextInt(3));
            burstFire(sl, target, 3);
            return;
        }
        // Цель сбежала далеко или окопалась наверху — он уже за спиной.
        if (dist > 16.0D || target.getY() > this.getY() + 4.0D) {
            blinkBehind(sl, target);
            return;
        }

        // Толпа вокруг — площадная атака: слэм, замедляющая нова или вихрь клинка.
        if (countSplashTargets(sl, 5.0D) >= 2 && sl.random.nextInt(100) < 55) {
            switch (sl.random.nextInt(3)) {
                case 0 -> groundSlam(sl);
                case 1 -> shockNova(sl);
                default -> bladeFlurry(sl);
            }
            return;
        }

        int roll = sl.random.nextInt(100);
        if (roll < 15) {
            blinkBehind(sl, target);
        } else if (roll < 28 && dist > 6.0D) {
            dashStrike(sl, target);                 // рывок вплотную сквозь дистанцию
        } else if (roll < 42 && dist > 7.0D) {
            yankTarget(sl, target);                 // телекинез: притянуть к себе
        } else if (roll < 54 && dist < 9.0D) {
            blinkAway(sl, target, 8.0D);
            burstFire(sl, target, 3);
        } else if (roll < 66) {
            homingVolley(sl, target);               // веер из пяти болтов
        } else if (roll < 78 && dist > 7.0D && orbitalTimer <= 0) {
            startOrbital(sl, target);               // телеграф-столб по позиции цели
        } else if (roll < 90) {
            // Зайти сбоку: 1.5-2.5 сек стрейфа по случайной дуге.
            strafeTicks = 30 + sl.random.nextInt(20);
            strafeDir = sl.random.nextBoolean() ? 0.9F : -0.9F;
        } else if (dist > 3.5D && dist < 13.0D) {
            leapAt(sl, target);
        } else {
            dashStrike(sl, target);
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

    /** Рывок-сближение: телепорт вплотную к цели + немедленный удар. Профи не догоняют — они ПОЯВЛЯЮТСЯ. */
    private void dashStrike(ServerLevel sl, LivingEntity target) {
        Vec3 toMe = this.position().subtract(target.position());
        Vec3 flat = new Vec3(toMe.x, 0.0D, toMe.z);
        flat = flat.lengthSqr() < 0.01D ? new Vec3(1.0D, 0.0D, 0.0D) : flat.normalize();
        Vec3 dest = target.position().add(flat.scale(1.7D));
        if (safeBlink(sl, dest.x, target.getY(), dest.z)) {
            this.getLookControl().setLookAt(target, 60.0F, 60.0F);
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            this.doHurtTarget(target);
            sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.HOSTILE, 1.3F, 0.9F);
            if (hostile && sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
                say(sl, pick(sl.random, DASH_LINES));
            }
        }
    }

    /** Веер из пяти болтов широким конусом — наказание за попытку «просто отойти вбок». */
    private void homingVolley(ServerLevel sl, LivingEntity target) {
        Vec3 aim = new Vec3(target.getX() - this.getX(),
                target.getEyeY() - this.getEyeY(),
                target.getZ() - this.getZ());
        for (int i = -2; i <= 2; i++) {
            Projectile bolt = (i % 2 == 0) ? new PlasmaBoltEntity(sl, this) : new RadiationBoltEntity(sl, this, false);
            double ang = Math.toRadians(i * 10.0);
            double cos = Math.cos(ang), sin = Math.sin(ang);
            double nx = aim.x * cos - aim.z * sin;
            double nz = aim.x * sin + aim.z * cos;
            bolt.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
            bolt.shoot(nx, aim.y, nz, 2.2F, 1.0F);
            sl.addFreshEntity(bolt);
        }
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        sl.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.3F, 1.2F);
    }

    /** Шоковая нова: импульс на 6 блоков — урон средний, но всех вокруг замедляет и расшвыривает. */
    private void shockNova(ServerLevel sl) {
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5F;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(6.0D), en -> isSplashTarget(en, null))) {
            e.hurt(this.damageSources().mobAttack(this), dmg);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            e.knockback(0.5D, this.getX() - e.getX(), this.getZ() - e.getZ());
        }
        for (int i = 0; i < 32; i++) {
            double a = i * Math.PI / 16.0D;
            sl.sendParticles(ParticleTypes.SONIC_BOOM,
                    this.getX() + Math.cos(a) * 1.5D, this.getY() + 0.8D, this.getZ() + Math.sin(a) * 1.5D,
                    1, Math.cos(a) * 0.3D, 0.0D, Math.sin(a) * 0.3D, 0.0D);
        }
        sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.2F, 1.4F);
    }

    /** Телекинез: рывком ПРИТЯГИВАЕТ убегающую цель к себе — от Макса не уходят. */
    private void yankTarget(ServerLevel sl, LivingEntity target) {
        Vec3 pull = this.position().subtract(target.position());
        if (pull.lengthSqr() < 0.01D) {
            return;
        }
        pull = pull.normalize().scale(1.8D);
        target.setDeltaMovement(pull.x, 0.3D, pull.z);
        target.hurtMarked = true;
        sl.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1.0D, target.getZ(),
                25, 0.3D, 0.5D, 0.3D, 0.4D);
        sl.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.8F, 1.5F);
        if (sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
            say(sl, pick(sl.random, YANK_LINES));
        }
    }

    /** Вихрь клинка: 1.5 секунды стоит и рубит всё вокруг (тикает в abilityTick). */
    private void bladeFlurry(ServerLevel sl) {
        flurryTicks = 30;
        this.getNavigation().stop();
        sl.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5F, 0.7F);
        if (sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
            say(sl, pick(sl.random, FLURRY_LINES));
        }
    }

    /** Помечает точку под целью; через ~1.2 с туда бьёт радиационный столб (telegraph). */
    private void startOrbital(ServerLevel sl, LivingEntity target) {
        orbitalX = target.getX();
        orbitalZ = target.getZ();
        orbitalTimer = 25;
        sl.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5F, 0.6F);
        if (sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
            say(sl, pick(sl.random, ORBITAL_LINES));
        }
    }

    private void orbitalStrikeHit(ServerLevel sl) {
        int gy = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                (int) Math.floor(orbitalX), (int) Math.floor(orbitalZ));
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, orbitalX, gy + 1.0D, orbitalZ, 3, 0.5D, 0.5D, 0.5D, 0.0D);
        sl.playSound(null, BlockPos.containing(orbitalX, gy, orbitalZ),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.7F);
        float dmg = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F;
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                orbitalX - 3.0D, gy - 2.0D, orbitalZ - 3.0D, orbitalX + 3.0D, gy + 6.0D, orbitalZ + 3.0D);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box, en -> isSplashTarget(en, null))) {
            e.hurt(this.damageSources().indirectMagic(this, this), dmg);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
        }
    }

    /** Отражает выстрел: гасит входящий и бьёт встречным болтом в самого стрелка. */
    private void deflectAt(ServerLevel sl, LivingEntity shooter) {
        sl.sendParticles(ParticleTypes.CRIT, this.getX(), this.getEyeY(), this.getZ(), 14, 0.3D, 0.3D, 0.3D, 0.4D);
        sl.playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.3F, 0.7F);
        RadiationBoltEntity bolt = new RadiationBoltEntity(sl, this, true);
        Vec3 aim = new Vec3(shooter.getX() - this.getX(),
                shooter.getEyeY() - this.getEyeY(),
                shooter.getZ() - this.getZ());
        bolt.setPos(this.getX(), this.getEyeY() - 0.1D, this.getZ());
        bolt.shoot(aim.x, aim.y, aim.z, 2.6F, 1.0F);
        sl.addFreshEntity(bolt);
        if (shooter instanceof Player && sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
            say(sl, pick(sl.random, DEFLECT_LINES));
        }
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

    // ----------------------------------------------------- ЭКИПИРОВКА / ЧАРЫ

    /** Зачаровывает экипировку «по-топовому». Вызывается один раз на сервере. */
    private void enchantGear(ServerLevel sl) {
        var reg = sl.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        // Меч: Острота V, Отбрасывание II, Огненный аспект II, Разящий клинок III, Прочность III.
        ItemStack sword = new ItemStack(ItemRegistry.NIBIRIUM_SWORD);
        sword.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS), 5);
        sword.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK), 2);
        sword.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FIRE_ASPECT), 2);
        sword.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SWEEPING_EDGE), 3);
        sword.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 3);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, sword);
        // Броня: Защита IV + Прочность III + Шипы III на каждую часть.
        ItemStack chest = new ItemStack(ItemRegistry.COSMIC_CHESTPLATE);
        ItemStack legs = new ItemStack(ItemRegistry.COSMIC_LEGGINGS);
        ItemStack boots = new ItemStack(ItemRegistry.COSMIC_BOOTS);
        for (ItemStack piece : new ItemStack[]{chest, legs, boots}) {
            piece.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION), 4);
            piece.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 3);
            piece.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.THORNS), 3);
        }
        // Сапоги дополнительно: Невесомость IV (мягкое приземление) и Хождение по дну III.
        boots.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FEATHER_FALLING), 4);
        boots.enchant(reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.DEPTH_STRIDER), 3);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, chest);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, legs);
        this.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, boots);
        for (net.minecraft.world.entity.EquipmentSlot s : net.minecraft.world.entity.EquipmentSlot.values()) {
            this.setDropChance(s, 0.0F);
        }
    }

    // ------------------------------------------------- РЕАКЦИЯ НА ОПАСНУЮ СРЕДУ

    /** Стоит ли Макс в опасности прямо сейчас (в лаве, тонет, на вредном блоке). */
    private boolean inDanger(ServerLevel sl) {
        if (this.isInLava()) {
            return true;
        }
        if (this.isInWater() && this.getAirSupply() < 40) {
            return true; // начал захлёбываться
        }
        BlockPos feet = this.blockPosition();
        return isHazardBlock(sl.getBlockState(feet)) || isHazardBlock(sl.getBlockState(feet.below()));
    }

    private boolean isHazardBlock(net.minecraft.world.level.block.state.BlockState s) {
        return s.is(net.minecraft.world.level.block.Blocks.LAVA)
            || s.is(net.minecraft.world.level.block.Blocks.FIRE)
            || s.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE)
            || s.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)
            || s.is(net.minecraft.world.level.block.Blocks.CACTUS)
            || s.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)
            || s.is(net.minecraft.world.level.block.Blocks.WITHER_ROSE)
            || s.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)
            || s.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)
            || s.is(com.example.alieninvasion.registry.ModBlocks.TOXIC_WATER)
            || s.is(com.example.alieninvasion.registry.ModBlocks.PURE_RADIATION_BLOCK);
    }

    /** Уходит на ближайшую безопасную сушу — не стоит столбом в огне/лаве/яде. */
    private boolean escapeHazard(ServerLevel sl) {
        for (int attempt = 0; attempt < 14; attempt++) {
            double a = sl.random.nextDouble() * Math.PI * 2.0D;
            double r = 3.0D + sl.random.nextDouble() * 5.0D;
            int x = (int) Math.floor(this.getX() + Math.cos(a) * r);
            int z = (int) Math.floor(this.getZ() + Math.sin(a) * r);
            int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos head = feet.above();
            var gs = sl.getBlockState(ground);
            if (!gs.isSolidRender(sl, ground) || isHazardBlock(gs)) {
                continue;
            }
            if (isHazardBlock(sl.getBlockState(feet)) || isHazardBlock(sl.getBlockState(head))) {
                continue;
            }
            if (!sl.getBlockState(feet).isAir() || !sl.getBlockState(head).isAir()) {
                continue;
            }
            this.clearFire();
            this.setDeltaMovement(0.0D, 0.0D, 0.0D);
            return safeBlink(sl, x + 0.5D, y, z + 0.5D);
        }
        // Сушу рядом не нашёл — хотя бы выпрыгнуть вверх из опасности.
        this.setDeltaMovement(this.getDeltaMovement().x, 0.55D, this.getDeltaMovement().z);
        this.hurtMarked = true;
        this.clearFire();
        return false;
    }

    // ------------------------------------------------------- ИНЖЕНЕРНЫЕ ПРИЁМЫ

    private static net.minecraft.world.level.block.state.BlockState buildBlock() {
        return com.example.alieninvasion.registry.ModBlocks.INFESTED_STONE_BRICKS.defaultBlockState();
    }

    private static net.minecraft.sounds.SoundEvent placeSound() {
        return net.minecraft.world.level.block.SoundType.STONE.getPlaceSound();
    }

    /** Быстрая стена между Максом и целью: 3 в ширину, 3 в высоту. */
    private void buildWall(ServerLevel sl, LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position());
        Vec3 flat = new Vec3(dir.x, 0.0D, dir.z);
        if (flat.lengthSqr() < 0.01D) {
            return;
        }
        flat = flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        BlockPos front = BlockPos.containing(this.getX() + flat.x * 1.5D, this.getY(), this.getZ() + flat.z * 1.5D);
        boolean placed = false;
        for (int dy = 0; dy <= 2; dy++) {
            for (int ds = -1; ds <= 1; ds++) {
                BlockPos p = front.offset((int) Math.round(side.x * ds), dy, (int) Math.round(side.z * ds));
                var st = sl.getBlockState(p);
                if (st.isAir() || st.canBeReplaced()) {
                    sl.setBlockAndUpdate(p, buildBlock());
                    placed = true;
                }
            }
        }
        if (placed) {
            sl.playSound(null, front, placeSound(), SoundSource.HOSTILE, 1.0F, 0.8F);
        }
    }

    /** Столбится вверх: колонна под ногами + подъём наверх (классический клатч). */
    private void pillarUp(ServerLevel sl, int height) {
        int bx = this.blockPosition().getX();
        int bz = this.blockPosition().getZ();
        int baseY = this.blockPosition().getY();
        int top = baseY + height;
        for (int y = baseY; y < top; y++) {
            BlockPos p = new BlockPos(bx, y, bz);
            var st = sl.getBlockState(p);
            if (st.isAir() || st.canBeReplaced()) {
                sl.setBlockAndUpdate(p, buildBlock());
            }
        }
        this.teleportTo(bx + 0.5D, top, bz + 0.5D);
        this.setDeltaMovement(0.0D, 0.0D, 0.0D);
        this.hurtMarked = true;
        sl.sendParticles(ParticleTypes.PORTAL, bx + 0.5D, top, bz + 0.5D, 15, 0.3D, 0.8D, 0.3D, 0.2D);
        sl.playSound(null, this.blockPosition(), placeSound(), SoundSource.HOSTILE, 1.0F, 1.0F);
        if (sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
            say(sl, pick(sl.random, BUILD_LINES));
        }
    }

    /** Окоп: стены вокруг + крыша — пара секунд под прикрытием, пока регенерация тикает. */
    private void buildBunker(ServerLevel sl) {
        BlockPos c = this.blockPosition();
        int[][] ring = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean placed = false;
        for (int dy = 0; dy <= 1; dy++) {
            for (int[] o : ring) {
                BlockPos p = c.offset(o[0], dy, o[1]);
                var st = sl.getBlockState(p);
                if (st.isAir() || st.canBeReplaced()) {
                    sl.setBlockAndUpdate(p, buildBlock());
                    placed = true;
                }
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos p = c.offset(dx, 2, dz);
                var st = sl.getBlockState(p);
                if (st.isAir() || st.canBeReplaced()) {
                    sl.setBlockAndUpdate(p, buildBlock());
                }
            }
        }
        if (placed) {
            sl.playSound(null, c, placeSound(), SoundSource.HOSTILE, 1.2F, 0.7F);
            if (sl.getGameTime() >= this.voiceUntil) {
                say(sl, pick(sl.random, BUILD_LINES));
            }
        }
    }

    /** Тактический отход: рывок назад + стена прикрытия. Перекус — общий тик. */
    private void tacticalRetreat(ServerLevel sl, LivingEntity target) {
        // Оплести преследователя паутиной, чтобы не догнал, и отскочить за стену.
        if (webCooldown <= 0) {
            webCooldown = 120;
            webTrap(sl, target);
        }
        blinkAway(sl, target, 12.0D);
        if (buildCooldown <= 0) {
            buildCooldown = 120;
            buildWall(sl, target);
        }
        if (sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
            say(sl, pick(sl.random, RETREAT_LINES));
        }
    }

    /** Паутина-ловушка: оплетает цель — застревают и мобы, и игроки. */
    private void webTrap(ServerLevel sl, LivingEntity target) {
        BlockPos c = target.blockPosition();
        int[][] spots = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean placed = false;
        for (int dy = 0; dy <= 1; dy++) {
            for (int[] o : spots) {
                BlockPos p = c.offset(o[0], dy, o[1]);
                if (sl.getBlockState(p).isAir()) {
                    sl.setBlockAndUpdate(p, net.minecraft.world.level.block.Blocks.COBWEB.defaultBlockState());
                    placed = true;
                }
            }
        }
        if (placed) {
            sl.playSound(null, c, SoundEvents.SPIDER_AMBIENT, SoundSource.HOSTILE, 1.0F, 0.7F);
            sl.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 1.0D, target.getZ(),
                    10, 0.4D, 0.6D, 0.4D, 0.0D);
            if (sl.getGameTime() >= this.voiceUntil && sl.random.nextInt(2) == 0) {
                say(sl, pick(sl.random, WEB_LINES));
            }
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
        // Средовой урон (лава, огонь, магма, кактус, падение, удушье) — он НЕ
        // игнорирует: тут же уходит на безопасную сушу. /kill и пустоту не убежать.
        if (source.getEntity() == null && source.getDirectEntity() == null
                && !source.is(net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL)
                && !source.is(net.minecraft.world.damagesource.DamageTypes.FELL_OUT_OF_WORLD)
                && hazardCooldown <= 0) {
            hazardCooldown = 20;
            if (escapeHazard(sl) && this.random.nextInt(3) == 0) {
                say(sl, pick(sl.random, HAZARD_LINES));
            }
        }
        // АДАПТАЦИЯ к дальнему бою: чем больше его расстреливают, тем чаще уклоняется
        // (до 65%), а иногда ОТРАЖАЕТ снаряд обратно в стрелка.
        if (source.getDirectEntity() instanceof Projectile) {
            rangedPressure = Math.min(60, rangedPressure + 4);
            float dodgeChance = 0.45F + Math.min(0.20F, rangedPressure * 0.005F);
            if (this.random.nextFloat() < dodgeChance) {
                if (source.getEntity() instanceof LivingEntity shooter && this.random.nextFloat() < 0.4F) {
                    deflectAt(sl, shooter);
                } else {
                    double a = this.random.nextDouble() * Math.PI * 2.0D;
                    safeBlink(sl, this.getX() + Math.cos(a) * 4.0D, this.getY(), this.getZ() + Math.sin(a) * 4.0D);
                    if (source.getEntity() instanceof Player && this.random.nextInt(3) != 0) {
                        say(sl, pick(sl.random, DODGE_LINES));
                    }
                }
                return false;
            }
        }
        boolean result = super.hurt(source, amount);
        // ПАРИРОВАНИЕ: пропустил удар в ближнем — мгновенно уходит за спину обидчику
        // и отвечает контратакой. Добить его непрерывной серией почти нереально.
        if (result && !(source.getDirectEntity() instanceof Projectile)
                && source.getEntity() instanceof LivingEntity parryTarget) {
            meleePressure = Math.min(60, meleePressure + 4);
            float parryChance = 0.35F + Math.min(0.20F, meleePressure * 0.005F); // до 0.55 под ближним прессингом
            if (this.random.nextFloat() < parryChance) {
                blinkBehind(sl, parryTarget);
                if (hostile && parryTarget == this.getTarget() && sl.getGameTime() >= this.voiceUntil
                        && sl.random.nextInt(2) == 0) {
                    say(sl, pick(sl.random, PARRY_LINES));
                }
            }
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
            say(sl, pick(sl.random, sl.random.nextBoolean() ? PLAYER_KILL_LINES : EXECUTE_LINES));
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
        tag.putBoolean("GearEnchanted", gearEnchanted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.dialogueStage = tag.getInt("DialogueStage");
        this.reactorGiven = tag.getBoolean("ReactorGiven");
        this.hostile = tag.getBoolean("Hostile");
        this.applesLeft = tag.contains("ApplesLeft") ? tag.getInt("ApplesLeft") : 4;
        this.gearEnchanted = tag.getBoolean("GearEnchanted");
    }
}
