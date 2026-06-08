# Леджер контента — вердикты и бэклог срезов

> Реестр всех узлов графа (см. [DESIGN.md](DESIGN.md)) с вердиктом по каждому.
> Вердикты: **KEEP** (есть назначение и связи) · **MERGE** (слить дубли) · **CUT** (мёртвое/нерабочее) · **REPURPOSE** (оживить, дав связь) · **INVESTIGATE** (нужна проверка кода перед действием).
> Это **бэклог** — исполняется срезами по одному. В Срезе 1 трогаем только `bio_blade` (рецепт) и `alien_city` (лут); остальное — будущие раунды.

## Материалы

| Имя | Как добыть | Связи / назначение | Вердикт |
|-----|-----------|--------------------|---------|
| `alien_alloy` | лут (все сундуки), блок-улей; ремонт био/хитин-гира | 28 рецептов — backbone | **KEEP** |
| `cosmic_shard` → `cosmic_ingot` → `cosmic_block` | `cosmic_ore` (глубоко, радиоактивно) | броня/инструменты/apex-крафт | **KEEP** |
| `platinum` + `palladium` → `nibirium_ingot` | руды | nibirium-инструменты | **KEEP** (цепочка v1.15) |
| `uranium_dust` → `uranium_rod` | `uranium_ore` (blast) | инструменты, стимуляторы, модули | **KEEP** (downstream сожмётся при MERGE тиров) |
| `plasma_core` | `plasma_ore` | турель, дрон, оружие | **KEEP** |
| `iridium_plate` | `iridium_ore` | гир, ремонты | **KEEP** |
| `hive_core` | **только лут** глубоких структур | `swarm_beacon`, `purifier`, `rally_banner`; **+ apex `bio_blade` (Срез 1)** | **KEEP** (чужой «командный» ресурс) |
| `xenocrystal` | `xenocrystal_ore` | 4 рецепта (есть downstream) | **KEEP** (функционален, не тупик) |
| `bio_fiber` | `bio_vein_ore` | серум, маски, фильтры | **KEEP** (функционален, не тупик) |
| `alien_skin` | `alien_flesh` | только `light_hazmat` | KEEP / merge-кандидат |
| `alien_scrap` | дроп мобов + лут | валюта чёрного рынка + крафт (recycler, drill_fuel_cell) | **KEEP** ✅ |
| **`dark_matter_shard`** | `dark_matter_ore` (blast); дроп Day-8 босса | ✅ ядро `bio_blade` + `cosmic_warhammer` | **REPURPOSE — сделано** |
| **`radiation_crystal`** | `pure_radiation_block` | → `alien_battery` (×2) | **REPURPOSE — сделано** |

## Оружие

| Имя | Механика | Вердикт |
|-----|----------|---------|
| `bio_blade` | инфекция + вампиризм vs алиены + AoE-нова (apex) | **KEEP** — *перегейтить в Срезе 1* |
| `cosmic_warhammer` | слэм-CC + Resistance | **KEEP** |
| `star_cleaver` | клив + бонус по рою | **KEEP** |
| `nibirium_sword` | резонансный клив | **KEEP** (металл-альтернатива) |
| `alien_blaster` | дальний бой, магазин + заряд-выстрел | **KEEP** |
| `emp_grenade` | вырубает технику/borer | **KEEP** |
| `uranium_sword` / `plasma_sword` / `iridium_sword` | uranium=рад+яд DoT · plasma=поджиг · iridium=нокбэк+замедл. | **DIFFERENTIATED — сделано** |
| `gravity_grenade` | рабочий (item + GravityGrenadeEntity + AntiGravityEffect) | **KEEP** ✅ |

## Броня

| Имя | Сет-бонус | Вердикт |
|-----|-----------|---------|
| `light_hazmat` | защита от лёгкой радиации (раннее) | **KEEP** / merge-кандидат с `hazmat` |
| `hazmat` | иммун к радиации | **KEEP** |
| `chitin` | снимает инфекцию/яд | **KEEP** |
| `cosmic` | двойной иммун + ходьба по чужим блокам (endgame) | **KEEP** |
| `gravity_boots` | рабочий класс (GravityBootsItem) | **KEEP** ✅ |

## Инструменты

| Имя | Механика | Вердикт |
|-----|----------|---------|
| bio-набор (`pickaxe`/`axe`/`shovel`/`hoe`) | быстрая добыча заражённого, дебаф, очистка | **KEEP** |
| `cosmic_pickaxe` | near-instant добыча | **KEEP** |
| nibirium-набор | `pickaxe`/`shovel` копают 3×3 | **KEEP** |
| uranium / plasma / iridium наборы (pickaxe/axe/shovel/hoe) | стандартные тиры инструментов (как ванильные) | **KEEP** (осознанно: оружие различено, тиры инструментов — норма прогрессии) |

## Бур (`borer`) и модули

| Имя | Эффект | Вердикт |
|-----|--------|---------|
| `borer` (транспорт) | туннелит 3×3, на топливе | **KEEP** |
| `reinforced_drill_head` | −1 топливо/блок | **KEEP** |
| `lava_cooling_module` | бурение сквозь лаву | **KEEP** |
| `toxic_seal_module` | снимает радиацию/яд с водителя | **KEEP** |
| `headlamp_module` | ночное зрение | **KEEP** |
| ~~`ore_filter_module`~~ | удалён (реестр+креатив+рецепт+модель+текстура) | **CUT — сделано** |
| ~~`storage_bay_module`~~ | удалён | **CUT — сделано** |
| `radiation_drill_head` | ✅ радиац-аура бура: урон+эффект по врагам в радиусе | **IMPLEMENTED** |
| `purifier_drill_head` | ✅ снимает инфекцию с водителя + частицы (доп. к toxic_seal) | **IMPLEMENTED** |
| `alien_battery` / `drill_fuel_cell` | топливо | **KEEP** |

## Утилити / расходники

| Имя | Назначение | Вердикт |
|-----|-----------|---------|
| `gravity_gun`, `blink_core`, `bio_grappling_hook` | CC / эскейп / мобильность | **KEEP** |
| `rally_banner`, `comms_beacon` | ко-оп бафф / мультиплеер | **KEEP** |
| `geiger_counter`, `portable_purifier`, `bio_filter_mask`, `rad_pills`, `purified_water_flask` | контра радиации | **KEEP** |
| `bio_serum`, `cosmic_stimulant`, `weak_antidote`, `herbal_salve` | хил/антидоты | **KEEP** |
| `purifier_wand` (PurifierItem), `invasion_tracker` | рабочие классы (очистка / трекер дня-сложности) | **KEEP** ✅ |

## Мобы

> **Поправка аудита:** петля дропа УЖЕ работает (`ModEvents.dropAlienLoot` на AFTER_DEATH: alloy/scrap/cosmic_credit/skin + рабочий BlackMarketTerminal). Срез 2 добавил `AlienBreacher`/`CaveLurker` в список и **apex-дроп боссу** (`bio_blade`+dark_matter+hive_core) — раньше Day-8 босс не ронял ничего.

| Имя | Роль / проблема | Вердикт |
|-----|-----------------|---------|
| `alien_grunt` | ядро, dual-mode AI | **KEEP** (дроп уже есть) |
| `alien_brute` | элит-страж | **KEEP** (дроп уже есть) |
| `alien_troll` | крадёт предметы (интересно) | **KEEP** |
| `plasma_caster` | дальний бой | **KEEP** |
| `alien_breacher`, `cave_lurker` | подземные варианты | **KEEP** + ✅ добавлены в дроп-список |
| `ufo` / `sky_drone` | воздух/транспорт | **KEEP** |
| `hive_tyrant` | мини-босс | **KEEP** (джекпот-дроп уже есть) |
| `swarm_mother` | **финальный босс** | **KEEP** + ✅ apex-дроп + ✅ фазовая аура/переходы (рёв+рывок+RED-бар)/зрелищная смерть |
| `parasite` | проклятие при смерти; entity+renderer+item полностью подключены | **KEEP** ✅ |
| `alien_chicken` | раш-юнит; дроп есть (в isAlien-списке) | **KEEP** |
| `alien_stalker`, `telekinetic_alien` | взвешенный подземный пул (gates diff≥5/≥6) | **KEEP** (видимость ✅ улучшена) |
| `hive_shaman` | всё ещё diff≥7–8 | KEEP (намеренно редкий элит) |
| `acid_spitter` | ✅ подземный пул (diff≥3) + наземные штурмы | **FIXED — спавнится** |
| `plasma_caster` (подземный) | ✅ был недостижим (баг порядка), теперь в пуле | **FIXED** |

## Структуры и лут

| Структура | Tier | Вердикт |
|-----------|------|---------|
| `alien_city` | 1 (поверхность) | ✅ **FIXED** — `bio_blade`+`hive_core` убраны, добавлены расходники |
| `crashed_ufo` | 1 (та же таблица `alien_city`) | чинится тем же edit; позже — своя таблица |
| `cave_dungeon`, `infested_mine`, `abandoned_lab` | 2 | **KEEP** (сбалансированы) |
| `cosmic_vault`, `hive_nest` | 3 | **KEEP** (законный гейт apex); в `hive_nest` лежит dead-end `dark_matter_shard` — см. материалы |

## Машины-блоки

| Имя | Статус | Вердикт |
|-----|--------|---------|
| `blueprint_table` | ✅ полевой мануал: apex-рецепты + статус вторжения (ПКМ пустой рукой) | **IMPLEMENTED** |
| `alien_recycler` | ✅ 6 scrap → 1 alloy (ПКМ + частицы/звук) | **IMPLEMENTED** |
| `ore_washer` | ✅ raw metal → 2× ingot (удвоение) | **IMPLEMENTED** |
| `radiation_forge` | ✅ плавка без топлива (cosmic_shard/raw → ingot) | **IMPLEMENTED** |
| `black_market_terminal` | рабочий (валюта scrap/credit) | **KEEP** |
| `purifier_station` | ✅ станция дезактивации: чистит радиац-дозу+эффекты у игроков в радиусе (safe-room) | **IMPLEMENTED** |
