# -*- coding: utf-8 -*-
"""Тролль-звуки, заражение 5->1, нибириевая броня, регистрация турельного BER/слоя,
блок-модель турели = основание, 3D-модель трубы."""
import json
import os


def patch(path, pairs):
    src = open(path, encoding="utf-8").read()
    for old, new in pairs:
        if old not in src:
            print("  !! NOT FOUND in %s: %r" % (path.split("/")[-1], old[:55]))
            continue
        src = src.replace(old, new, 1)
    open(path, "w", encoding="utf-8").write(src)
    print("  patched " + path.split("/")[-1])


E = "src/main/java/com/example/alieninvasion"

# 1) Заражение за удар 5 -> 1.
patch(E + "/events/ModEvents.java", [
    ("                InfectionManager.addMeter(player, 5.0F);",
     "                InfectionManager.addMeter(player, 1.0F);"),
])

# 2) Тролль: писклявые тихие звуки вместо рычания равагера.
patch(E + "/entity/AlienTrollEntity.java", [
    ("        return net.minecraft.sounds.SoundEvents.RAVAGER_AMBIENT;",
     "        return net.minecraft.sounds.SoundEvents.SILVERFISH_AMBIENT;"),
    ("        return net.minecraft.sounds.SoundEvents.RAVAGER_HURT;",
     "        return net.minecraft.sounds.SoundEvents.SILVERFISH_HURT;"),
    ("        return net.minecraft.sounds.SoundEvents.RAVAGER_DEATH;",
     "        return net.minecraft.sounds.SoundEvents.SILVERFISH_DEATH;"),
])
# питч/громкость тролля -> высокий и тихий (хрупкий воришка)
troll = open(E + "/entity/AlienTrollEntity.java", encoding="utf-8").read()
import re
troll = re.sub(r"public float getVoicePitch\(\) \{\s*return [0-9.F]+",
               "public float getVoicePitch() {\n        return 1.8F", troll, count=1)
troll = re.sub(r"protected float getSoundVolume\(\) \{\s*return [0-9.F]+",
               "protected float getSoundVolume() {\n        return 0.4F", troll, count=1)
open(E + "/entity/AlienTrollEntity.java", "w", encoding="utf-8").write(troll)
print("  troll pitch/volume tweaked")

# 3) Турель: блок-модель = только основание (башню рисует BER).
json.dump({
    "credit": "turret base",
    "textures": {"metal": "alien-invasion:block/plasma_turret", "particle": "alien-invasion:block/plasma_turret"},
    "elements": [
        {"from": [1, 0, 1], "to": [15, 3, 15],
         "faces": {f: {"texture": "#metal"} for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [5, 3, 5], "to": [11, 5, 11],
         "faces": {f: {"texture": "#metal"} for f in ("north", "south", "east", "west", "up", "down")}}
    ]
}, open("src/main/resources/assets/alien-invasion/models/block/plasma_turret.json", "w"), indent=2)
print("  block model plasma_turret -> base only")

# 4) Турельный слой + BER в клиенте.
patch(E + "/client/ModModelLayers.java", [
    ("    public static final ModelLayerLocation ALIEN_CHICKEN = new ModelLayerLocation(",
     '''    public static final ModelLayerLocation PLASMA_TURRET = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "plasma_turret"), "main");
    public static final ModelLayerLocation ALIEN_CHICKEN = new ModelLayerLocation('''),
])
patch("src/main/java/com/example/alieninvasion/AlienInvasionClient.java", [
    ("                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ALIEN_CHICKEN,",
     """                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.PLASMA_TURRET,
                                com.example.alieninvasion.client.model.PlasmaTurretModel::createBodyLayer);
                net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
                                com.example.alieninvasion.registry.ModBlocks.PLASMA_TURRET_BLOCK_ENTITY,
                                com.example.alieninvasion.client.PlasmaTurretRenderer::new);
                EntityModelLayerRegistry.registerModelLayer(ModModelLayers.ALIEN_CHICKEN,"""),
])

# 5) 3D-модель трубы: тело-цилиндр + два фланца + утечка-вентиль.
json.dump({
    "credit": "cracked alien pipe",
    "textures": {"pipe": "alien-invasion:block/cracked_alien_pipe",
                 "particle": "alien-invasion:block/cracked_alien_pipe"},
    "elements": [
        {"from": [4, 0, 4], "to": [12, 16, 12],
         "faces": {f: {"texture": "#pipe"} for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [3, 2, 3], "to": [13, 5, 13],
         "faces": {f: {"texture": "#pipe"} for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [3, 11, 3], "to": [13, 14, 13],
         "faces": {f: {"texture": "#pipe"} for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [11, 6, 7], "to": [15, 10, 9],
         "faces": {f: {"texture": "#pipe"} for f in ("north", "south", "east", "west", "up", "down")}}
    ]
}, open("src/main/resources/assets/alien-invasion/models/block/cracked_alien_pipe.json", "w"), indent=2)
json.dump({"parent": "alien-invasion:block/cracked_alien_pipe"},
          open("src/main/resources/assets/alien-invasion/models/item/cracked_alien_pipe.json", "w"), indent=2)
print("  3D pipe model")

# 6) НИБИРИЕВАЯ БРОНЯ: материал + 4 предмета.
material = '''package com.example.alieninvasion.item;

import com.example.alieninvasion.AlienInvasionMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.List;

/** Nibirium armor: the heaviest endgame plate - tanky, radiation-resistant. */
public class NibiriumArmorMaterial {
    public static final Holder<ArmorMaterial> INSTANCE = register();

    private static Holder<ArmorMaterial> register() {
        EnumMap<ArmorItem.Type, Integer> def = new EnumMap<>(ArmorItem.Type.class);
        def.put(ArmorItem.Type.BOOTS, 4);
        def.put(ArmorItem.Type.LEGGINGS, 7);
        def.put(ArmorItem.Type.CHESTPLATE, 9);
        def.put(ArmorItem.Type.HELMET, 4);
        def.put(ArmorItem.Type.BODY, 13);
        ArmorMaterial mat = new ArmorMaterial(def, 12,
                SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(ItemRegistry.NIBIRIUM_INGOT),
                List.of(new ArmorMaterial.Layer(
                        ResourceLocation.fromNamespaceAndPath(AlienInvasionMod.MODID, "nibirium"))),
                3.0F, 0.1F);
        return Holder.direct(mat);
    }
}
'''
open(E + "/item/NibiriumArmorMaterial.java", "w", encoding="utf-8").write(material)
print("  NibiriumArmorMaterial.java")

# Регистрация предметов брони.
ir = open(E + "/registry/ItemRegistry.java", encoding="utf-8").read()
if "NIBIRIUM_HELMET" not in ir:
    anchor = '    public static final Item NIBIRIUM_INGOT = registerItem("nibirium_ingot",'
    idx = ir.index(anchor)
    block = '''    public static final Item NIBIRIUM_HELMET = registerItem("nibirium_helmet",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.NibiriumArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.RARE)));
    public static final Item NIBIRIUM_CHESTPLATE = registerItem("nibirium_chestplate",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.NibiriumArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.RARE)));
    public static final Item NIBIRIUM_LEGGINGS = registerItem("nibirium_leggings",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.NibiriumArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, new Item.Properties().rarity(Rarity.RARE)));
    public static final Item NIBIRIUM_BOOTS = registerItem("nibirium_boots",
            new net.minecraft.world.item.ArmorItem(com.example.alieninvasion.item.NibiriumArmorMaterial.INSTANCE,
                    net.minecraft.world.item.ArmorItem.Type.BOOTS, new Item.Properties().rarity(Rarity.RARE)));
'''
    ir = ir[:idx] + block + ir[idx:]
    open(E + "/registry/ItemRegistry.java", "w", encoding="utf-8").write(ir)
    print("  nibirium armor items registered")

# Рецепты нибириевой брони + lang.
RD = "src/main/resources/data/alien-invasion/recipe"
pats = {"helmet": ["III", "I I"], "chestplate": ["I I", "III", "III"],
        "leggings": ["III", "I I", "I I"], "boots": ["I I", "I I"]}
for piece, pattern in pats.items():
    json.dump({"type": "minecraft:crafting_shaped", "pattern": pattern,
               "key": {"I": {"item": "alien-invasion:nibirium_ingot"}},
               "result": {"id": "alien-invasion:nibirium_" + piece, "count": 1}},
              open(RD + "/nibirium_" + piece + ".json", "w"), indent=2)
for piece in pats:
    json.dump({"parent": "minecraft:item/generated",
               "textures": {"layer0": "alien-invasion:item/nibirium_" + piece}},
              open("src/main/resources/assets/alien-invasion/models/item/nibirium_" + piece + ".json", "w"), indent=2)
ru = {"item.alien-invasion.nibirium_helmet": "Нибириевый шлем",
      "item.alien-invasion.nibirium_chestplate": "Нибириевый нагрудник",
      "item.alien-invasion.nibirium_leggings": "Нибириевые поножи",
      "item.alien-invasion.nibirium_boots": "Нибириевые ботинки"}
en = {"item.alien-invasion.nibirium_helmet": "Nibirium Helmet",
      "item.alien-invasion.nibirium_chestplate": "Nibirium Chestplate",
      "item.alien-invasion.nibirium_leggings": "Nibirium Leggings",
      "item.alien-invasion.nibirium_boots": "Nibirium Boots"}
for path, add in (("src/main/resources/assets/alien-invasion/lang/ru_ru.json", ru),
                  ("src/main/resources/assets/alien-invasion/lang/en_us.json", en)):
    d = json.load(open(path, encoding="utf-8"))
    d.update(add)
    json.dump(d, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print("  nibirium recipes + models + lang")
print("DONE JAVA")
