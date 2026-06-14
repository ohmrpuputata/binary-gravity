#!/usr/bin/env python3
"""Repair Russian localization mojibake and replace placeholder tooltips."""

from __future__ import annotations

import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
LANG_PATH = ROOT / "src/main/resources/assets/alien-invasion/lang/ru_ru.json"
LINE_RE = re.compile(
    r'^(\s*)"((?:\\.|[^"\\])*)"(\s*:\s*)("(?:\\.|[^"\\])*")(,?)(\s*)$'
)

MOJIBAKE_MARKERS = set(
    "ЎЂЃЉЊЋЌЏђѓљњћќџ‚„…†‡€‰‹“”•–—™›№"
)


TOOLTIP_OVERRIDES = {
    "alien_battery": (
        "Компонент питания для инопланетных устройств.",
        "Внутри всё ещё дрожит холодный заряд Роя.",
    ),
    "alien_beacon": (
        "Инопланетный маяк, отмечающий присутствие Роя.",
        "Его сигнал уходит далеко за пределы видимого неба.",
    ),
    "alien_blaster": (
        "ПКМ: мощный выстрел. Shift+ПКМ: очередь из десяти зарядов.",
        "Оружие Роя перегревается, но не требует обычных боеприпасов.",
    ),
    "alien_hive": (
        "Живой блок улья, поддерживающий заражение вокруг себя.",
        "Его стенки едва заметно пульсируют.",
    ),
    "alien_residue": (
        "Мягкий инопланетный осадок, связанный с заражением.",
        "Остатки биомассы Роя цепляются за любую поверхность.",
    ),
    "alien_skin": (
        "Прочный органический материал для защитного снаряжения.",
        "Высохшая кожа пришельца всё ещё сопротивляется химии.",
    ),
    "alien_stash": (
        "Контейнер для добычи и припасов пришельцев.",
        "Рой оставляет такие тайники возле важных объектов.",
    ),
    "bio_filter_mask": (
        "Носимая маска для работы в заражённых и токсичных зонах.",
        "Простой фильтр лучше, чем вдох заражённого воздуха.",
    ),
    "bio_serum": (
        "Снимает заражение, радиацию и психическое давление; даёт регенерацию.",
        "Полноценная аварийная очистка организма.",
    ),
    "blink_core": (
        "Телепортирует примерно на девять блоков вперёд и временно усиливает защиту.",
        "Короткий разрыв пространства для экстренного отступления.",
    ),
    "comms_beacon": (
        "Передаёт всем игрокам ваши координаты и создаёт сигнальный луч.",
        "Когда связь рушится, один яркий сигнал может спасти отряд.",
    ),
    "contaminated_bones": (
        "Хрупкий блок костей, изменённых заражённой средой.",
        "Неясно, кому принадлежал этот скелет.",
    ),
    "contaminated_food": (
        "Сомнительная пища: утоляет голод, но несёт риск заражения.",
        "Запах почти обычный. Почти.",
    ),
    "cosmic_block": (
        "Компактное хранилище космического материала.",
        "Сплав слабо мерцает даже без источника света.",
    ),
    "cosmic_ingot": (
        "Редкий материал для космического снаряжения и устройств.",
        "Слиток кажется легче, чем должен быть.",
    ),
    "cosmic_ore": (
        "Руда, из которой получают космические материалы.",
        "В камне застыл свет чужого неба.",
    ),
    "cosmic_shard": (
        "Осколок для создания космических сплавов.",
        "Край кристалла оставляет холодный светящийся след.",
    ),
    "cosmic_stimulant": (
        "Лечит и временно усиливает защиту, поглощение, скорость и силу.",
        "Аварийная инъекция для боя, когда отступать уже некуда.",
    ),
    "dark_matter_ore": (
        "Редкая светящаяся руда, содержащая тёмную материю.",
        "Пространство рядом с ней выглядит чуть неправильным.",
    ),
    "dark_matter_shard": (
        "Нестабильный материал для высокоуровневых технологий.",
        "Осколок не отражает свет так, как обычное вещество.",
    ),
    "emp_grenade": (
        "Создаёт электромагнитный импульс и нарушает работу техники.",
        "Короткое молчание машин иногда важнее взрыва.",
    ),
    "geiger_counter": (
        "Измеряет местный радиационный фон и накопленную дозу.",
        "Чем чаще щелчки, тем быстрее нужно уходить.",
    ),
    "gravity_boots": (
        "Космические ботинки с системой управления гравитацией.",
        "Каждый шаг ощущается так, будто земля отпускает вас.",
    ),
    "gravity_grenade": (
        "Подбрасывает существ в области взрыва антигравитационной волной.",
        "Полезна, когда Рой окружил со всех сторон.",
    ),
    "gravity_gun": (
        "Накладывает антигравитацию на цель; восемь зарядов восстанавливаются сами.",
        "ЭМП-буря временно отключает оружие.",
    ),
    "herbal_salve": (
        "Даёт регенерацию и сбрасывает раннее накопление заражения.",
        "Полевое средство из мира, который ещё можно спасти.",
    ),
    "hive_core": (
        "Редкое сердце улья и ценный компонент.",
        "Даже отделённое от улья оно продолжает биться.",
    ),
    "hunter_token": (
        "Уникальный трофей, связанный с Охотником.",
        "Знак победы над тем, кто сам привык преследовать добычу.",
    ),
    "infected_water_bucket": (
        "Разливает воду, заражённую биомассой Роя.",
        "Обычная вода уже проиграла борьбу за эту ёмкость.",
    ),
    "infection_pills": (
        "Постепенно очищают заражение в течение минуты.",
        "Лечение действует не сразу и ненадолго замедляет работу.",
    ),
    "invasion_tracker": (
        "Ищет постройки, редкие руды и подсвечивает ближайших пришельцев.",
        "Сканер показывает расстояние, направление и высоту цели.",
    ),
    "nibirium_ingot": (
        "Огнестойкий сплав платины и палладия для продвинутого снаряжения.",
        "В металле соединены прочность и скорость двух редких руд.",
    ),
    "palladium_chunk": (
        "Кусок палладиевой руды для дальнейшей переработки.",
        "Промывка позволяет извлечь из него больше металла.",
    ),
    "palladium_ingot": (
        "Лёгкий металл для быстрых инструментов, брони и нибирия.",
        "Палладий ценят за подвижность, а не за тяжёлую защиту.",
    ),
    "parasite_item": (
        "ПКМ по существу подчиняет его и превращает в союзника.",
        "На голове владельца паразит, напротив, подчиняет самого носителя.",
    ),
    "plasma_bolt": (
        "Стабилизированный плазменный заряд для инопланетного оружия.",
        "Без подходящего ускорителя это лишь горячая искра в оболочке.",
    ),
    "plasma_cell": (
        "Энергетическая ячейка для плазменных устройств.",
        "Внутри удерживается заряд, способный прожечь броню.",
    ),
    "plasma_turret": (
        "Стационарная плазменная установка для обороны территории.",
        "Её оптика следит за движением даже в полной темноте.",
    ),
    "platinum_chunk": (
        "Кусок платиновой руды для дальнейшей переработки.",
        "Промывка позволяет извлечь из него больше металла.",
    ),
    "platinum_ingot": (
        "Тяжёлый металл для прочных инструментов, брони и нибирия.",
        "Платина выдерживает удар там, где лёгкие сплавы гнутся.",
    ),
    "pure_radiation_block": (
        "Яркий и крайне опасный концентрат радиации.",
        "Даже недолгое пребывание рядом требует защиты.",
    ),
    "purifier": (
        "Очищает и удерживает от заражения весь занятый чанк.",
        "Пока устройство работает, Рою приходится отвоёвывать территорию заново.",
    ),
    "purifier_wand": (
        "Очищает заражённые блоки и существ в конусе перед игроком.",
        "Портативная очистка расходует прочность устройства.",
    ),
    "rad_pills": (
        "Постепенно снижают радиационное загрязнение в течение минуты.",
        "Лечение даёт краткую слабость при работе с инструментами.",
    ),
    "radiation_crystal": (
        "Концентрированный радиоактивный ресурс для опасных технологий.",
        "Кристалл светится слишком ровно, чтобы быть безопасным.",
    ),
    "raw_palladium": (
        "Сырой палладий, готовый к переплавке.",
        "После очистки станет лёгким и быстрым металлом.",
    ),
    "raw_platinum": (
        "Сырая платина, готовая к переплавке.",
        "После очистки станет основой тяжёлой защитной экипировки.",
    ),
    "swarm_beacon": (
        "Маяк, способный привлечь к миру главные силы Роя.",
        "Его активация означает, что скрываться больше не получится.",
    ),
    "toxic_water_bucket": (
        "Разливает токсичную воду, опасную для живых существ.",
        "Зелёный оттенок — самая безобидная её особенность.",
    ),
    "weak_antidote": (
        "Полностью снимает заражение после употребления.",
        "Простое лекарство, способное остановить раннюю катастрофу.",
    ),
}


VANILLA_BLOCK_NAMES = {
    "barrel": "Бочка",
    "cartography_table": "Стол картографа",
    "clay": "Глина",
    "crafting_table": "Верстак",
    "deepslate": "Глубинный сланец",
    "diamond_ore": "Алмазная руда",
    "dirt": "Земля",
    "door": "Дверь",
    "fletching_table": "Стол лучника",
    "glass": "Стекло",
    "grass": "Дёрн",
    "gravel": "Гравий",
    "grindstone": "Точило",
    "ice": "Лёд",
    "leaves": "Листва",
    "log": "Бревно",
    "loom": "Ткацкий станок",
    "netherrack": "Незерак",
    "plank_fence": "Деревянный забор",
    "plank_slab": "Деревянная плита",
    "plank_stairs": "Деревянные ступени",
    "planks": "Доски",
    "redstone_ore": "Редстоуновая руда",
    "sand": "Песок",
    "sandstone": "Песчаник",
    "smithing_table": "Стол кузнеца",
    "snow": "Снег",
    "stone": "Камень",
    "stone_bricks": "Каменные кирпичи",
    "stone_slab": "Каменная плита",
    "stone_stairs": "Каменные ступени",
    "stonecutter": "Камнерез",
    "terracotta": "Терракота",
    "trapdoor": "Люк",
    "wool": "Шерсть",
}


EXTRA_OVERRIDES = {
    "tooltip.alien-invasion.alien_worker_spawn_egg.functional": "Призывает: рабочий Роя.",
    "tooltip.alien-invasion.drill_spawn_egg.functional": "Призывает: бурильную машину Роя.",
    "tooltip.alien-invasion.meteor_spawn_egg.functional": "Призывает: метеорит.",
    "tooltip.alien-invasion.planet_reactor.functional": (
        "Массивный генератор, перерабатывающий высокоэнергетические космические ядра."
    ),
}


def mojibake_score(text: str) -> int:
    score = text.count("Р") + text.count("С")
    for char in text:
        if char in MOJIBAKE_MARKERS or 0x80 <= ord(char) <= 0x9F:
            score += 1
    return score


def decode_cp1251_mojibake(text: str) -> str:
    try:
        encoded = bytearray()
        for char in text:
            if 0x80 <= ord(char) <= 0x9F:
                encoded.append(ord(char))
            else:
                encoded.extend(char.encode("cp1251"))
        return bytes(encoded).decode("utf-8")
    except (UnicodeEncodeError, UnicodeDecodeError):
        return text


def repair_mojibake(text: str) -> str:
    whole = decode_cp1251_mojibake(text)
    if mojibake_score(whole) < mojibake_score(text):
        return whole

    parts = re.split(r"([ \t\r\n]+)", text)
    for index, part in enumerate(parts):
        candidate = decode_cp1251_mojibake(part)
        if mojibake_score(candidate) < mojibake_score(part):
            parts[index] = candidate
    return "".join(parts)


def build_overrides(data: dict[str, str]) -> dict[str, str]:
    overrides = dict(EXTRA_OVERRIDES)
    for item_id, (functional, lore) in TOOLTIP_OVERRIDES.items():
        prefix = f"tooltip.alien-invasion.{item_id}"
        overrides[f"{prefix}.functional"] = functional
        overrides[f"{prefix}.lore"] = lore

    clean_names = {
        key.removeprefix("block.alien-invasion."): repair_mojibake(value)
        for key, value in data.items()
        if key.startswith("block.alien-invasion.") and isinstance(value, str)
    }

    def base_name(block_id: str) -> str | None:
        return VANILLA_BLOCK_NAMES.get(block_id) or clean_names.get(block_id)

    for key in data:
        prefix = "block.alien-invasion."
        if not key.startswith(prefix):
            continue
        block_id = key.removeprefix(prefix)
        if block_id == "bloody_infested":
            overrides[key] = "Заражённый камень (в крови)"
        elif block_id.startswith("bloody_infested_"):
            name = base_name(block_id.removeprefix("bloody_infested_"))
            if name:
                overrides[key] = f"{name} (заражение и кровь)"
        elif block_id.startswith("bloody_"):
            name = base_name(block_id.removeprefix("bloody_"))
            if name:
                overrides[key] = f"{name} (в крови)"
        elif block_id.startswith("infested_"):
            name = base_name(block_id.removeprefix("infested_"))
            if name:
                overrides[key] = f"{name} (заражение)"
    return overrides


def main() -> None:
    raw = LANG_PATH.read_text(encoding="utf-8-sig")
    data = json.loads(raw)
    overrides = build_overrides(data)

    repaired_count = 0
    override_count = 0
    output_lines: list[str] = []

    for line in raw.splitlines():
        match = LINE_RE.match(line)
        if not match:
            output_lines.append(line)
            continue

        indent, encoded_key, separator, encoded_value, comma, suffix = match.groups()
        key = json.loads(f'"{encoded_key}"')
        value = json.loads(encoded_value)
        repaired = repair_mojibake(value)
        if repaired != value:
            repaired_count += 1

        final_value = overrides.get(key, repaired)
        if final_value != repaired:
            override_count += 1

        output_lines.append(
            f'{indent}"{encoded_key}"{separator}'
            f"{json.dumps(final_value, ensure_ascii=False)}{comma}{suffix}"
        )

    output = "\n".join(output_lines) + "\n"
    parsed = json.loads(output)
    if len(parsed) != len(data):
        raise RuntimeError("Localization key count changed unexpectedly")

    LANG_PATH.write_text(output, encoding="utf-8", newline="\n")
    print(
        f"Updated {LANG_PATH}: repaired {repaired_count} mojibake values, "
        f"applied {override_count} text improvements."
    )


if __name__ == "__main__":
    main()
