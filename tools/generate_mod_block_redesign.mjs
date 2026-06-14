import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { execFileSync } from "node:child_process";
import sharp from "sharp";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const userHome = os.homedir();
const minecraftJar = path.join(
  userHome,
  ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-client.jar",
);
const vanillaDir = path.join(root, "build", "vanilla-texture-source");
const assets = path.join(root, "src", "main", "resources", "assets", "alien-invasion");
const data = path.join(root, "src", "main", "resources", "data", "alien-invasion");
const blockTextureDir = path.join(assets, "textures", "block");
const itemTextureDir = path.join(assets, "textures", "item");
const armorTextureDir = path.join(assets, "textures", "models", "armor");
const blockModelDir = path.join(assets, "models", "block");
const itemModelDir = path.join(assets, "models", "item");
const blockstateDir = path.join(assets, "blockstates");
const lootDir = path.join(data, "loot_table", "blocks");
const reportDir = path.join(root, "build", "reports");
const artDir = path.join(root, "art");

const blockEntries = [
  ["alien_residue", "alien_residue"],
  ["pure_radiation_block", "pure_radiation_block"],
  ["cosmic_crystal_ore", "cosmic_ore"],
  ["platinum_ore", "platinum_ore"],
  ["platinum_block", "platinum_block"],
  ["palladium_ore", "palladium_ore"],
  ["palladium_block", "palladium_block"],
  ["alien_flesh", "alien_flesh"],
  ["alien_hive", "alien_hive"],
  ["alien_heart", "alien_heart"],
  ["radio_transmitter", "radio_transmitter"],
  ["alien_tendrils", "alien_tendrils"],
  ["purifier", "purifier"],
  ["alien_stash", "alien_stash"],
  ["alien_beacon", "alien_beacon"],
  ["swarm_beacon", "swarm_beacon"],
  ["dark_matter_ore", "dark_matter_ore"],
  ["plasma_turret", "plasma_turret"],
  ["black_market_terminal", "black_market_terminal"],
  ["purifier_station", "purifier_station"],
  ["ore_washer", "ore_washer"],
  ["blueprint_table", "blueprint_table"],
  ["warning_lamp", "warning_lamp"],
  ["cracked_alien_pipe", "cracked_alien_pipe"],
  ["toxic_barrel", "toxic_barrel"],
  ["broken_lab_crate", "broken_lab_crate"],
  ["contaminated_bones", "contaminated_bones"],
  ["alien_portal", "alien_portal"],
  ["planet_reactor", "planet_reactor_side"],
  ["blood_pool", "blood_pool"],
];

const vanillaAssets = [
  "block/amethyst_block.png",
  "block/anvil.png",
  "block/anvil_top.png",
  "block/barrel_side.png",
  "block/blast_furnace_side.png",
  "block/bone_block_side.png",
  "block/cartography_table_top.png",
  "block/chipped_anvil_top.png",
  "block/copper_block.png",
  "block/copper_ore.png",
  "block/crafter_north.png",
  "block/crimson_roots.png",
  "block/crying_obsidian.png",
  "block/damaged_anvil_top.png",
  "block/deepslate.png",
  "block/deepslate_diamond_ore.png",
  "block/diamond_ore.png",
  "block/glowstone.png",
  "block/gold_ore.png",
  "block/honeycomb_block.png",
  "block/iron_block.png",
  "block/iron_ore.png",
  "block/lodestone_side.png",
  "block/lodestone_top.png",
  "block/nether_portal.png",
  "block/nether_wart_block.png",
  "block/oak_planks.png",
  "block/observer_side.png",
  "block/oxidized_copper.png",
  "block/purple_shulker_box.png",
  "block/redstone_lamp_on.png",
  "block/respawn_anchor_side0.png",
  "block/sculk.png",
  "block/sculk_catalyst_side.png",
  "block/sculk_catalyst_top.png",
  "block/smithing_table_side.png",
  "block/smithing_table_top.png",
  "block/stone.png",
  "item/crossbow_standby.png",
  "item/diamond_boots.png",
  "item/diamond_chestplate.png",
  "item/diamond_helmet.png",
  "item/diamond_leggings.png",
  "item/ender_eye.png",
  "item/diamond_axe.png",
  "item/diamond_hoe.png",
  "item/diamond_pickaxe.png",
  "item/diamond_shovel.png",
  "item/diamond_sword.png",
  "item/iron_axe.png",
  "item/iron_hoe.png",
  "item/iron_pickaxe.png",
  "item/iron_shovel.png",
  "item/iron_sword.png",
  "item/netherite_ingot.png",
  "item/netherite_axe.png",
  "item/netherite_hoe.png",
  "item/netherite_pickaxe.png",
  "item/netherite_shovel.png",
  "item/netherite_sword.png",
  "item/rotten_flesh.png",
  "models/armor/netherite_layer_1.png",
  "models/armor/netherite_layer_2.png",
].map(name => `assets/minecraft/textures/${name}`);

const palettes = {
  platinum: ["#30343a", "#656c75", "#9ba2aa", "#d9dde0", "#f7f7f2"],
  palladium: ["#173d42", "#287178", "#55a7a3", "#9ad2c8", "#d6f1df"],
  emeradium: ["#062f1b", "#087437", "#18b95a", "#55e77d", "#c2ffc2"],
  cosmic: ["#221842", "#43307c", "#7654bf", "#70c7dd", "#d1fbff"],
  darkMatter: ["#090813", "#171329", "#34204b", "#713178", "#dfb83d"],
  purple: ["#24102f", "#4f1c68", "#79339a", "#a54cc7", "#d783ed"],
  blood: ["#2c0307", "#5f0710", "#930d18", "#c31c2a", "#ed4550"],
  greenMachine: ["#10231d", "#1f4838", "#37755a", "#68a976", "#b5e39a"],
  steel: ["#1d2428", "#354249", "#59676d", "#89969a", "#c0c9c8"],
  warmMetal: ["#2b2118", "#55402c", "#856642", "#b58b55", "#dfbd77"],
};

function ensureDirs() {
  for (const dir of [
    vanillaDir, blockTextureDir, itemTextureDir, armorTextureDir,
    blockModelDir, itemModelDir, blockstateDir, lootDir, reportDir, artDir,
  ]) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function extractVanillaAssets() {
  if (!fs.existsSync(minecraftJar)) {
    throw new Error(`Minecraft client jar not found: ${minecraftJar}`);
  }
  const missing = vanillaAssets.filter(rel => !fs.existsSync(path.join(vanillaDir, rel)));
  if (missing.length === 0) return;
  execFileSync("jar", ["xf", minecraftJar, ...missing], {
    cwd: vanillaDir,
    stdio: "inherit",
  });
}

function rgba(hex, alpha = 255) {
  const value = Number.parseInt(hex.slice(1), 16);
  return [(value >> 16) & 255, (value >> 8) & 255, value & 255, alpha];
}

function hash(text) {
  let value = 2166136261;
  for (const char of text) {
    value ^= char.charCodeAt(0);
    value = Math.imul(value, 16777619);
  }
  return value >>> 0;
}

async function loadVanilla(name, firstFrame = false) {
  const input = path.join(vanillaDir, "assets", "minecraft", "textures", name);
  let image = sharp(input).ensureAlpha();
  const metadata = await image.metadata();
  if (firstFrame && metadata.height > metadata.width) {
    image = image.extract({ left: 0, top: 0, width: metadata.width, height: metadata.width });
  }
  const { data, info } = await image.raw().toBuffer({ resolveWithObject: true });
  return { data: Buffer.from(data), width: info.width, height: info.height };
}

async function loadImage(file) {
  const { data, info } = await sharp(file).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
  return { data: Buffer.from(data), width: info.width, height: info.height };
}

function cloneImage(image) {
  return { data: Buffer.from(image.data), width: image.width, height: image.height };
}

function pixelOffset(image, x, y) {
  return (y * image.width + x) * 4;
}

function setPixel(image, x, y, color, opaqueOnly = false) {
  if (x < 0 || x >= image.width || y < 0 || y >= image.height) return;
  const offset = pixelOffset(image, x, y);
  if (opaqueOnly && image.data[offset + 3] < 32) return;
  image.data[offset] = color[0];
  image.data[offset + 1] = color[1];
  image.data[offset + 2] = color[2];
  image.data[offset + 3] = color[3] ?? 255;
}

function getPixel(image, x, y) {
  const offset = pixelOffset(image, x, y);
  return [
    image.data[offset],
    image.data[offset + 1],
    image.data[offset + 2],
    image.data[offset + 3],
  ];
}

function luminance(color) {
  return color[0] * 0.2126 + color[1] * 0.7152 + color[2] * 0.0722;
}

function paletteColor(color, palette) {
  const colors = palette.map(value => rgba(value));
  const index = Math.min(colors.length - 1, Math.floor(luminance(color) / 256 * colors.length));
  return colors[index];
}

function recolor(image, palette, strength = 1) {
  const output = cloneImage(image);
  for (let y = 0; y < output.height; y++) {
    for (let x = 0; x < output.width; x++) {
      const offset = pixelOffset(output, x, y);
      if (output.data[offset + 3] < 32) continue;
      const original = getPixel(image, x, y);
      const target = paletteColor(original, palette);
      for (let channel = 0; channel < 3; channel++) {
        output.data[offset + channel] = Math.round(
          original[channel] * (1 - strength) + target[channel] * strength,
        );
      }
    }
  }
  return output;
}

function tint(image, color, amount) {
  const output = cloneImage(image);
  for (let offset = 0; offset < output.data.length; offset += 4) {
    if (output.data[offset + 3] < 32) continue;
    for (let channel = 0; channel < 3; channel++) {
      output.data[offset + channel] = Math.round(
        output.data[offset + channel] * (1 - amount) + color[channel] * amount,
      );
    }
  }
  return output;
}

function overlayOre(base, oreTexture, palette) {
  const output = cloneImage(base);
  for (let y = 0; y < output.height; y++) {
    for (let x = 0; x < output.width; x++) {
      const basePixel = getPixel(base, x, y);
      const orePixel = getPixel(oreTexture, x, y);
      const difference =
        Math.abs(basePixel[0] - orePixel[0]) +
        Math.abs(basePixel[1] - orePixel[1]) +
        Math.abs(basePixel[2] - orePixel[2]);
      if (difference > 34) setPixel(output, x, y, paletteColor(orePixel, palette));
    }
  }
  return output;
}

function addRect(image, x0, y0, x1, y1, color, opaqueOnly = true) {
  for (let y = y0; y <= y1; y++) {
    for (let x = x0; x <= x1; x++) setPixel(image, x, y, color, opaqueOnly);
  }
}

function addMachineLights(image, color = rgba("#7dff79")) {
  const dark = rgba("#102019");
  addRect(image, 5, 5, 10, 10, dark);
  addRect(image, 6, 6, 9, 9, rgba("#214d34"));
  setPixel(image, 7, 7, color);
  setPixel(image, 8, 7, rgba("#d6ffd0"));
  setPixel(image, 7, 8, rgba("#5bd869"));
  setPixel(image, 8, 8, color);
}

function addCrack(image, color = rgba("#1a1720")) {
  const points = [[3, 1], [4, 2], [5, 3], [5, 4], [6, 5], [7, 6], [7, 7], [8, 8], [9, 9], [9, 10], [10, 11], [11, 12], [12, 13]];
  for (const [x, y] of points) setPixel(image, x, y, color, true);
  setPixel(image, 4, 4, color, true);
  setPixel(image, 3, 5, color, true);
  setPixel(image, 10, 8, color, true);
  setPixel(image, 11, 7, color, true);
}

function addCrackPath(image, points, color) {
  for (const [x, y] of points) setPixel(image, x, y, color, true);
}

function makeBloodPool() {
  const image = {
    data: Buffer.alloc(16 * 16 * 4),
    width: 16,
    height: 16,
  };
  const outline = rgba("#4d050c");
  const dark = rgba("#750913");
  const mid = rgba("#a5121e");
  const light = rgba("#d22b35");
  const rows = [
    [5, 10], [3, 12], [2, 13], [1, 14], [1, 14],
    [2, 13], [3, 12], [4, 11],
  ];
  rows.forEach(([left, right], index) => {
    const y = index + 4;
    for (let x = left; x <= right; x++) {
      const edge = x === left || x === right || index === 0 || index === rows.length - 1;
      setPixel(image, x, y, edge ? outline : ((x + y) % 4 === 0 ? light : mid), false);
    }
  });
  setPixel(image, 5, 7, dark, false);
  setPixel(image, 10, 9, dark, false);
  setPixel(image, 8, 6, light, false);
  return image;
}

function infect(image, id) {
  const output = tint(image, [102, 45, 128], 0.2);
  const seed = hash(`${id}:infection`);
  const dark = rgba("#4e1b68");
  const mid = rgba("#7f35a2");
  const light = rgba("#ae5ac9");
  for (let index = 0; index < 7; index++) {
    const x = (seed >>> ((index * 3) % 24)) & 15;
    const y = (seed >>> ((index * 5 + 2) % 24)) & 15;
    setPixel(output, x, y, index < 2 ? light : mid, true);
    if (index < 3) setPixel(output, x + 1, y, dark, true);
    if (index === 0) setPixel(output, x, y + 1, mid, true);
  }
  return output;
}

function bloody(image, id) {
  const output = tint(image, [94, 15, 22], 0.035);
  const seed = hash(`${id}:blood`);
  const dark = rgba("#5c0710");
  const mid = rgba("#9e101c");
  const light = rgba("#d22631");
  for (let index = 0; index < 8; index++) {
    const x = (seed >>> ((index * 4) % 24)) & 15;
    const y = (seed >>> ((index * 3 + 1) % 24)) & 15;
    setPixel(output, x, y, index < 2 ? light : mid, true);
    if (index < 4) setPixel(output, x + 1, y, dark, true);
    if (index === 0) setPixel(output, x, y + 1, dark, true);
  }
  return output;
}

async function createBlockTexture(id) {
  const stone = await loadVanilla("block/stone.png");
  switch (id) {
    case "alien_residue":
      return recolor(await loadVanilla("block/sculk.png"), palettes.purple, 0.72);
    case "pure_radiation_block":
      return recolor(await loadVanilla("block/glowstone.png"), palettes.emeradium, 0.9);
    case "cosmic_crystal_ore":
      return overlayOre(stone, await loadVanilla("block/gold_ore.png"), palettes.cosmic);
    case "platinum_ore":
      return overlayOre(stone, await loadVanilla("block/iron_ore.png"), palettes.platinum);
    case "platinum_block":
      return recolor(await loadVanilla("block/iron_block.png"), palettes.platinum, 0.82);
    case "palladium_ore":
      return overlayOre(stone, await loadVanilla("block/copper_ore.png"), palettes.palladium);
    case "palladium_block":
      return recolor(await loadVanilla("block/copper_block.png"), palettes.palladium, 0.86);
    case "alien_flesh":
      return recolor(await loadVanilla("block/nether_wart_block.png"), palettes.purple, 0.72);
    case "alien_hive":
      return recolor(await loadVanilla("block/honeycomb_block.png"), palettes.purple, 0.82);
    case "alien_heart": {
      const image = recolor(await loadVanilla("block/redstone_lamp_on.png"), palettes.blood, 0.72);
      setPixel(image, 7, 6, rgba("#ef6c77"));
      setPixel(image, 8, 6, rgba("#ff9a9a"));
      return image;
    }
    case "radio_transmitter": {
      const image = recolor(await loadVanilla("block/observer_side.png"), palettes.steel, 0.78);
      addMachineLights(image, rgba("#66ff8a"));
      return image;
    }
    case "alien_tendrils":
      return recolor(await loadVanilla("block/crimson_roots.png"), palettes.purple, 0.92);
    case "purifier": {
      const image = recolor(await loadVanilla("block/smithing_table_side.png"), palettes.steel, 0.66);
      addMachineLights(image, rgba("#8fffdc"));
      return image;
    }
    case "alien_stash": {
      const image = recolor(await loadVanilla("block/barrel_side.png"), palettes.warmMetal, 0.45);
      addRect(image, 7, 6, 8, 9, rgba("#765194"));
      setPixel(image, 7, 7, rgba("#c28ce6"));
      return image;
    }
    case "alien_beacon": {
      const image = recolor(await loadVanilla("block/respawn_anchor_side0.png"), palettes.cosmic, 0.78);
      addMachineLights(image, rgba("#93f7ff"));
      return image;
    }
    case "swarm_beacon": {
      const image = recolor(await loadVanilla("block/sculk_catalyst_side.png"), palettes.purple, 0.75);
      addMachineLights(image, rgba("#c375e6"));
      return image;
    }
    case "dark_matter_ore":
      return overlayOre(
        await loadVanilla("block/deepslate.png"),
        await loadVanilla("block/deepslate_diamond_ore.png"),
        palettes.darkMatter,
      );
    case "plasma_turret": {
      const image = recolor(await loadVanilla("block/copper_block.png"), palettes.steel, 0.78);
      addMachineLights(image, rgba("#68f2df"));
      return image;
    }
    case "black_market_terminal": {
      const image = recolor(await loadVanilla("block/crafter_north.png"), palettes.warmMetal, 0.55);
      addRect(image, 4, 4, 11, 9, rgba("#161b1e"));
      setPixel(image, 6, 6, rgba("#7fe55f"));
      setPixel(image, 7, 6, rgba("#9bff76"));
      setPixel(image, 9, 7, rgba("#7fe55f"));
      return image;
    }
    case "purifier_station": {
      const image = recolor(await loadVanilla("block/smithing_table_top.png"), palettes.steel, 0.76);
      addMachineLights(image, rgba("#b2fff0"));
      return image;
    }
    case "ore_washer": {
      const image = recolor(await loadVanilla("block/blast_furnace_side.png"), palettes.steel, 0.7);
      addRect(image, 5, 6, 10, 10, rgba("#244c55"));
      setPixel(image, 6, 7, rgba("#75dce8"));
      setPixel(image, 8, 8, rgba("#a6f4ff"));
      setPixel(image, 9, 9, rgba("#55b8c8"));
      return image;
    }
    case "blueprint_table": {
      const image = recolor(await loadVanilla("block/cartography_table_top.png"), palettes.warmMetal, 0.34);
      addRect(image, 4, 3, 12, 11, rgba("#d9e7d2"));
      setPixel(image, 6, 5, rgba("#4e8eb3"));
      setPixel(image, 7, 5, rgba("#4e8eb3"));
      setPixel(image, 7, 6, rgba("#4e8eb3"));
      setPixel(image, 9, 8, rgba("#4e8eb3"));
      return image;
    }
    case "warning_lamp": {
      const image = recolor(await loadVanilla("block/redstone_lamp_on.png"), palettes.warmMetal, 0.25);
      addRect(image, 5, 5, 10, 10, rgba("#8e1019"));
      addRect(image, 6, 6, 9, 9, rgba("#ee3b2e"));
      setPixel(image, 7, 6, rgba("#ffd164"));
      return image;
    }
    case "cracked_alien_pipe": {
      const image = recolor(await loadVanilla("block/oxidized_copper.png"), palettes.greenMachine, 0.52);
      addCrack(image);
      return image;
    }
    case "toxic_barrel": {
      const image = recolor(await loadVanilla("block/barrel_side.png"), palettes.warmMetal, 0.42);
      addRect(image, 5, 5, 10, 10, rgba("#31431e"));
      setPixel(image, 7, 6, rgba("#a3e837"));
      setPixel(image, 8, 7, rgba("#d3ff59"));
      setPixel(image, 6, 8, rgba("#80ca31"));
      return image;
    }
    case "broken_lab_crate": {
      const image = recolor(await loadVanilla("block/oak_planks.png"), palettes.warmMetal, 0.3);
      addCrack(image, rgba("#3b2419"));
      return image;
    }
    case "contaminated_bones": {
      const image = recolor(await loadVanilla("block/bone_block_side.png"), palettes.platinum, 0.22);
      setPixel(image, 3, 4, rgba("#75bd42"));
      setPixel(image, 10, 7, rgba("#9ce35a"));
      setPixel(image, 12, 12, rgba("#557f37"));
      return image;
    }
    case "alien_portal":
      return recolor(await loadVanilla("block/nether_portal.png", true), palettes.cosmic, 0.86);
    case "planet_reactor": {
      const image = recolor(await loadVanilla("block/lodestone_side.png"), palettes.warmMetal, 0.42);
      addMachineLights(image, rgba("#ffbc45"));
      return image;
    }
    case "blood_pool":
      return makeBloodPool();
    default:
      throw new Error(`No block texture design for ${id}`);
  }
}

function replaceTextures(value, replacements) {
  if (typeof value === "string") {
    let result = value;
    for (const [from, to] of replacements) result = result.replaceAll(from, to);
    return result;
  }
  if (Array.isArray(value)) return value.map(item => replaceTextures(item, replacements));
  if (value && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([key, item]) => [key, replaceTextures(item, replacements)]),
    );
  }
  return value;
}

function writeJson(file, value) {
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);
}

function writeVariantResources(entry, variant) {
  const [id, textureName] = entry;
  const variantId = `${variant}_${id}`;
  const baseModel = JSON.parse(fs.readFileSync(path.join(blockModelDir, `${id}.json`), "utf8"));
  const replacements = [
    [`alien-invasion:block/${textureName}`, `alien-invasion:block/${variantId}`],
    ["alien-invasion:block/planet_reactor_top", `alien-invasion:block/${variant}_planet_reactor_top`],
    ["alien-invasion:block/planet_reactor_side", `alien-invasion:block/${variant}_planet_reactor_side`],
  ];
  writeJson(path.join(blockModelDir, `${variantId}.json`), replaceTextures(baseModel, replacements));
  writeJson(path.join(blockstateDir, `${variantId}.json`), {
    variants: { "": { model: `alien-invasion:block/${variantId}` } },
  });
  writeJson(path.join(itemModelDir, `${variantId}.json`), {
    parent: `alien-invasion:block/${variantId}`,
  });
  writeJson(path.join(lootDir, `${variantId}.json`), {
    type: "minecraft:block",
    pools: [{
      bonus_rolls: 0,
      conditions: [{ condition: "minecraft:survives_explosion" }],
      entries: [{ type: "minecraft:item", name: `alien-invasion:${variantId}` }],
      rolls: 1,
    }],
    random_sequence: `alien-invasion:blocks/${variantId}`,
  });
}

async function writeTexture(file, image) {
  await sharp(image.data, {
    raw: { width: image.width, height: image.height, channels: 4 },
  }).png({ palette: true, colours: 64, dither: 0 }).toFile(file);
}

async function createReactorTop() {
  const image = recolor(await loadVanilla("block/lodestone_top.png"), palettes.warmMetal, 0.38);
  addRect(image, 5, 5, 10, 10, rgba("#6c4215"));
  addRect(image, 6, 6, 9, 9, rgba("#dc8c22"));
  setPixel(image, 7, 7, rgba("#ffe88b"));
  setPixel(image, 8, 7, rgba("#fff4b3"));
  setPixel(image, 7, 8, rgba("#ffbd3e"));
  setPixel(image, 8, 8, rgba("#ffd567"));
  return image;
}

async function createAnvils() {
  const definitions = [
    ["platinum", palettes.platinum],
    ["palladium", palettes.palladium],
  ];
  for (const [name, palette] of definitions) {
    const side = recolor(await loadVanilla("block/anvil.png"), palette, 0.88);
    const top = recolor(await loadVanilla("block/anvil_top.png"), palette, 0.88);
    const chipped = recolor(await loadVanilla("block/chipped_anvil_top.png"), palette, 0.88);
    const damaged = recolor(await loadVanilla("block/damaged_anvil_top.png"), palette, 0.88);
    const crack = name === "platinum" ? rgba("#454b52") : rgba("#16494c");
    addCrackPath(chipped, [[5, 1], [6, 2], [7, 2], [8, 3], [9, 3]], crack);
    addCrackPath(damaged, [
      [3, 1], [4, 2], [5, 3], [6, 3], [7, 4],
      [10, 1], [9, 2], [8, 3], [8, 4], [7, 5],
      [11, 6], [10, 7], [9, 8],
    ], crack);
    await writeTexture(path.join(blockTextureDir, `${name}_anvil_side.png`), side);
    await writeTexture(path.join(blockTextureDir, `${name}_anvil_top.png`), top);
    await writeTexture(path.join(blockTextureDir, `chipped_${name}_anvil_top.png`), chipped);
    await writeTexture(path.join(blockTextureDir, `damaged_${name}_anvil_top.png`), damaged);
  }
}

function recolorTool(image, palette) {
  const output = cloneImage(image);
  for (let y = 0; y < output.height; y++) {
    for (let x = 0; x < output.width; x++) {
      const original = getPixel(image, x, y);
      if (original[3] < 32) continue;
      const isWoodHandle =
        original[0] > original[1] * 1.08 &&
        original[1] > original[2] * 1.18 &&
        original[0] > 45;
      if (isWoodHandle) continue;
      setPixel(output, x, y, paletteColor(original, palette), false);
    }
  }
  return output;
}

async function createMaterialTools() {
  const tools = ["sword", "pickaxe", "axe", "shovel", "hoe"];
  const materials = [
    ["platinum", "iron", palettes.platinum],
    ["palladium", "diamond", palettes.palladium],
    ["nibirium", "netherite", palettes.darkMatter],
  ];
  for (const [material, vanillaMaterial, palette] of materials) {
    for (const tool of tools) {
      const source = await loadVanilla(`item/${vanillaMaterial}_${tool}.png`);
      const texture = recolorTool(source, palette);
      await writeTexture(path.join(itemTextureDir, `${material}_${tool}.png`), texture);
    }
  }
}

async function createDistinctHumanoids() {
  const entityDir = path.join(assets, "textures", "entity");
  const skinSource = path.join(artDir, "alien_humanoid_texture_base.png");
  const eyesSource = path.join(artDir, "alien_humanoid_eyes_base.png");
  if (!fs.existsSync(skinSource)) {
    fs.copyFileSync(path.join(entityDir, "acid_spitter.png"), skinSource);
  }
  if (!fs.existsSync(eyesSource)) {
    fs.copyFileSync(path.join(entityDir, "acid_spitter_eyes.png"), eyesSource);
  }

  const baseSkin = await loadImage(skinSource);
  const baseEyes = await loadImage(eyesSource);
  const acidPalette = ["#142016", "#334c25", "#5f8034", "#94bd3d", "#d6f05c"];
  const plasmaPalette = ["#11162c", "#283965", "#4b5fa0", "#667fd2", "#9ff3ff"];
  const acidSkin = recolor(baseSkin, acidPalette, 0.86);
  const plasmaSkin = recolor(baseSkin, plasmaPalette, 0.86);
  const acidEyes = recolor(baseEyes, ["#25400f", "#5a8e1d", "#9bdc31", "#dfff62"], 1);
  const plasmaEyes = recolor(baseEyes, ["#182653", "#315f99", "#52b8d8", "#d0ffff"], 1);

  await writeTexture(path.join(entityDir, "acid_spitter.png"), acidSkin);
  await writeTexture(path.join(entityDir, "plasma_caster.png"), plasmaSkin);
  await writeTexture(path.join(entityDir, "acid_spitter_eyes.png"), acidEyes);
  await writeTexture(path.join(entityDir, "plasma_caster_eyes.png"), plasmaEyes);
}

function removeUnusedDuplicateTextures() {
  const files = [
    path.join(blockTextureDir, "bloody_infested.png"),
    path.join(blockTextureDir, "infested_planet_reactor_side.png"),
    path.join(blockTextureDir, "bloody_planet_reactor_side.png"),
    path.join(blockTextureDir, "bloody_infested_planet_reactor_side.png"),
    path.join(itemTextureDir, "purifier.png"),
    path.join(armorTextureDir, "hazmat_layer_1.png"),
    path.join(armorTextureDir, "hazmat_layer_2.png"),
  ];
  for (const file of files) {
    if (fs.existsSync(file)) fs.rmSync(file);
  }
}

function makeShield() {
  const image = { data: Buffer.alloc(16 * 16 * 4), width: 16, height: 16 };
  const outline = rgba("#052617");
  const dark = rgba("#08703a");
  const mid = rgba("#17b858");
  const light = rgba("#6aeb87");
  for (let y = 1; y <= 12; y++) {
    const inset = y < 3 ? 2 : (y > 9 ? y - 8 : 1);
    for (let x = 3 + inset; x <= 12 - inset; x++) {
      const edge = x === 3 + inset || x === 12 - inset || y === 1 || y === 12;
      setPixel(image, x, y, edge ? outline : ((x + y) % 3 === 0 ? light : mid), false);
    }
  }
  addRect(image, 7, 3, 8, 10, dark, false);
  addRect(image, 5, 6, 10, 7, dark, false);
  setPixel(image, 7, 6, rgba("#caffbc"), false);
  setPixel(image, 8, 6, rgba("#efffe7"), false);
  return image;
}

function makeGreenRay(charge) {
  const image = { data: Buffer.alloc(16 * 16 * 4), width: 16, height: 16 };
  const outline = rgba("#11161a");
  const steelDark = rgba("#273238");
  const steel = rgba("#53656a");
  const steelLight = rgba("#8ba0a1");
  const greenDark = rgba("#07582c");
  const green = rgba(charge >= 2 ? "#49e978" : "#18ad55");
  const greenLight = rgba(charge >= 3 ? "#efffc9" : "#9bff95");

  addRect(image, 1, 5, 11, 9, outline, false);
  addRect(image, 2, 5, 10, 8, steelDark, false);
  addRect(image, 3, 5, 8, 5, steelLight, false);
  addRect(image, 3, 8, 9, 8, steel, false);
  addRect(image, 10, 4, 14, 7, outline, false);
  addRect(image, 10, 5, 14, 6, steel, false);
  addRect(image, 4, 9, 8, 13, outline, false);
  addRect(image, 5, 9, 7, 12, steelDark, false);
  setPixel(image, 5, 13, outline, false);
  setPixel(image, 6, 13, outline, false);

  addRect(image, 7, 5, 10, 8, greenDark, false);
  addRect(image, 8, 6, 9, 7, green, false);
  setPixel(image, 9, 6, greenLight, false);
  setPixel(image, 10, 7, green, false);
  setPixel(image, 14, 5, charge >= 2 ? greenLight : green, false);
  setPixel(image, 14, 6, greenDark, false);
  if (charge >= 1) {
    setPixel(image, 11, 4, green, false);
    setPixel(image, 10, 8, green, false);
  }
  if (charge >= 2) {
    setPixel(image, 13, 4, greenLight, false);
    setPixel(image, 12, 7, greenLight, false);
    setPixel(image, 7, 4, green, false);
  }
  if (charge >= 3) {
    setPixel(image, 15, 5, greenLight, false);
    setPixel(image, 15, 6, green, false);
    setPixel(image, 14, 3, green, false);
    setPixel(image, 14, 8, green, false);
    setPixel(image, 11, 3, greenLight, false);
  }
  return image;
}

function makeAstralPrismIngot() {
  const image = { data: Buffer.alloc(16 * 16 * 4), width: 16, height: 16 };
  const outline = rgba("#17112f");
  const violetDark = rgba("#382277");
  const violet = rgba("#7548d8");
  const cyan = rgba("#44dbe8");
  const cyanLight = rgba("#b9ffff");
  const gold = rgba("#e6b84b");

  // A faceted, asymmetric prism rather than another recoloured vanilla ingot.
  addRect(image, 4, 4, 11, 11, outline, false);
  addRect(image, 3, 6, 12, 9, outline, false);
  addRect(image, 5, 3, 10, 12, outline, false);
  addRect(image, 5, 4, 10, 11, violetDark, false);
  addRect(image, 4, 6, 11, 9, violet, false);
  addRect(image, 6, 4, 9, 10, cyan, false);
  addRect(image, 7, 4, 8, 8, cyanLight, false);
  setPixel(image, 5, 5, gold, false);
  setPixel(image, 10, 10, gold, false);
  setPixel(image, 4, 8, violetDark, false);
  setPixel(image, 11, 7, cyan, false);
  return image;
}

function makeAstralPrismGun() {
  const image = { data: Buffer.alloc(16 * 16 * 4), width: 16, height: 16 };
  const outline = rgba("#11151f");
  const metalDark = rgba("#28303b");
  const metal = rgba("#596474");
  const violetDark = rgba("#392371");
  const violet = rgba("#7948dc");
  const cyan = rgba("#40dce8");
  const cyanLight = rgba("#c4ffff");
  const gold = rgba("#d6a844");

  addRect(image, 1, 5, 12, 8, outline, false);
  addRect(image, 2, 5, 11, 7, metalDark, false);
  addRect(image, 3, 5, 8, 5, metal, false);
  addRect(image, 8, 4, 13, 7, outline, false);
  addRect(image, 9, 4, 13, 6, violetDark, false);
  addRect(image, 10, 4, 12, 5, violet, false);
  addRect(image, 13, 4, 15, 6, outline, false);
  addRect(image, 13, 5, 15, 5, cyan, false);
  setPixel(image, 15, 5, cyanLight, false);

  addRect(image, 4, 8, 8, 12, outline, false);
  addRect(image, 5, 8, 7, 11, metalDark, false);
  setPixel(image, 5, 12, outline, false);
  setPixel(image, 6, 12, outline, false);

  addRect(image, 6, 5, 9, 8, violetDark, false);
  addRect(image, 7, 5, 8, 7, cyan, false);
  setPixel(image, 8, 5, cyanLight, false);
  setPixel(image, 3, 6, gold, false);
  setPixel(image, 9, 8, violet, false);
  return image;
}

function makeAstralResonanceGrenade() {
  const image = { data: Buffer.alloc(16 * 16 * 4), width: 16, height: 16 };
  const outline = rgba("#17112d");
  const violetDark = rgba("#352067");
  const violet = rgba("#7544cf");
  const cyan = rgba("#3fd7e3");
  const cyanLight = rgba("#caffff");
  const gold = rgba("#dfb34d");

  // Compact crystal cage: clearly a grenade, but not the old round EMP sprite.
  addRect(image, 6, 2, 9, 13, outline, false);
  addRect(image, 3, 6, 12, 9, outline, false);
  addRect(image, 5, 4, 10, 11, outline, false);
  addRect(image, 6, 3, 9, 12, violetDark, false);
  addRect(image, 4, 6, 11, 9, violet, false);
  addRect(image, 6, 5, 9, 10, cyan, false);
  addRect(image, 7, 5, 8, 8, cyanLight, false);
  setPixel(image, 5, 5, gold, false);
  setPixel(image, 10, 5, gold, false);
  setPixel(image, 5, 10, gold, false);
  setPixel(image, 10, 10, gold, false);
  setPixel(image, 7, 2, outline, false);
  setPixel(image, 8, 2, gold, false);
  return image;
}

async function createAstralPrismItems() {
  await writeTexture(
    path.join(itemTextureDir, "astral_prism_ingot.png"),
    makeAstralPrismIngot(),
  );
  await writeTexture(
    path.join(itemTextureDir, "astral_prism_gun.png"),
    makeAstralPrismGun(),
  );
  await writeTexture(
    path.join(itemTextureDir, "astral_resonance_grenade.png"),
    makeAstralResonanceGrenade(),
  );
}

async function createEmeradiumItems() {
  const itemSources = [
    ["emeradium_helmet", "item/diamond_helmet.png"],
    ["emeradium_chestplate", "item/diamond_chestplate.png"],
    ["emeradium_leggings", "item/diamond_leggings.png"],
    ["emeradium_boots", "item/diamond_boots.png"],
  ];
  for (const [name, source] of itemSources) {
    const texture = recolor(await loadVanilla(source), palettes.emeradium, 0.9);
    await writeTexture(path.join(itemTextureDir, `${name}.png`), texture);
  }

  const ingot = recolor(await loadVanilla("item/netherite_ingot.png"), palettes.emeradium, 0.92);
  await writeTexture(path.join(itemTextureDir, "emeradium_ingot.png"), ingot);

  const resonator = recolor(await loadVanilla("item/ender_eye.png"), palettes.emeradium, 0.82);
  setPixel(resonator, 7, 7, rgba("#eaffd8"), true);
  setPixel(resonator, 8, 7, rgba("#b7ff9f"), true);
  setPixel(resonator, 7, 8, rgba("#88ff7c"), true);
  await writeTexture(path.join(itemTextureDir, "emeradium_resonator.png"), resonator);

  await writeTexture(path.join(itemTextureDir, "emeradium_shield.png"), makeShield());
  for (let frame = 0; frame <= 3; frame++) {
    const suffix = frame === 0 ? "" : `_charge_${frame}`;
    await writeTexture(
      path.join(itemTextureDir, `green_ray_blaster${suffix}.png`),
      makeGreenRay(frame),
    );
  }

  for (const layer of [1, 2]) {
    // Emeradium uses the PLATINUM custom model. Recolouring that exact 64x64
    // atlas preserves every UV used by its visor, shoulders, backpack, knees,
    // and boots; vanilla armor atlases are only 64x32 and leave those parts blank.
    const platinumAtlas = await loadImage(
      path.join(armorTextureDir, `platinum_layer_${layer}.png`),
    );
    const armor = recolor(platinumAtlas, palettes.emeradium, 0.9);
    await writeTexture(path.join(armorTextureDir, `emeradium_layer_${layer}.png`), armor);
  }
}

async function createFleshItems() {
  const source = await loadVanilla("item/rotten_flesh.png");
  const bloodyFlesh = recolor(source, palettes.blood, 0.88);
  const infestedFlesh = recolor(source, palettes.purple, 0.78);
  const combined = bloody(infestedFlesh, "bloody_infested_flesh");
  await writeTexture(path.join(itemTextureDir, "bloody_flesh.png"), bloodyFlesh);
  await writeTexture(path.join(itemTextureDir, "bloody_infested_flesh.png"), combined);
}

async function makePreview(blocks) {
  const scale = 5;
  const cell = 112;
  const cols = 6;
  const rows = Math.ceil((blockEntries.length - 1) / cols);
  const width = cols * cell;
  const height = 52 + rows * cell;
  const composites = [];
  const labels = [];

  for (let index = 0; index < blockEntries.length - 1; index++) {
    const [id] = blockEntries[index];
    const base = blocks.get(id);
    const variants = [base, infect(base, id), bloody(base, id), bloody(infect(base, id), id)];
    const left = (index % cols) * cell + 8;
    const top = Math.floor(index / cols) * cell + 58;
    for (let variant = 0; variant < variants.length; variant++) {
      const png = await sharp(variants[variant].data, {
        raw: { width: variants[variant].width, height: variants[variant].height, channels: 4 },
      }).resize(16 * scale, 16 * scale, { kernel: "nearest" }).png().toBuffer();
      composites.push({ input: png, left: left + variant * 24, top });
    }
    labels.push(`<text x="${left}" y="${top + 94}" fill="#d5dce5" font-family="Consolas" font-size="9">${id}</text>`);
  }

  const svg = Buffer.from(`<svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
    <rect width="100%" height="100%" fill="#121720"/>
    <text x="14" y="24" fill="#f4f7fa" font-family="Segoe UI" font-size="18" font-weight="700">Vanilla-style mod textures</text>
    <text x="14" y="42" fill="#8f9bad" font-family="Segoe UI" font-size="11">base / infested / bloody / combined</text>
    ${labels.join("")}
  </svg>`);
  composites.unshift({ input: svg, left: 0, top: 0 });

  await sharp({ create: { width, height, channels: 4, background: "#121720" } })
    .composite(composites)
    .png()
    .toFile(path.join(reportDir, "vanilla_style_block_variants.png"));

  const itemNames = [
    "emeradium_ingot", "emeradium_resonator", "emeradium_shield",
    "emeradium_helmet", "emeradium_chestplate", "emeradium_leggings", "emeradium_boots",
    "green_ray_blaster", "green_ray_blaster_charge_1",
    "green_ray_blaster_charge_2", "green_ray_blaster_charge_3",
  ];
  const itemComposites = [];
  const itemLabels = [];
  for (let index = 0; index < itemNames.length; index++) {
    const name = itemNames[index];
    const left = 18 + index * 92;
    const image = await sharp(path.join(itemTextureDir, `${name}.png`))
      .resize(64, 64, { kernel: "nearest" })
      .png()
      .toBuffer();
    itemComposites.push({ input: image, left, top: 44 });
    itemLabels.push(`<text x="${left + 32}" y="124" text-anchor="middle" fill="#d5dce5" font-family="Consolas" font-size="8">${name}</text>`);
  }
  const itemWidth = 28 + itemNames.length * 92;
  const itemSvg = Buffer.from(`<svg width="${itemWidth}" height="144" xmlns="http://www.w3.org/2000/svg">
    <rect width="100%" height="100%" fill="#121720"/>
    <text x="14" y="24" fill="#f4f7fa" font-family="Segoe UI" font-size="18" font-weight="700">Emeradium items and Green Ray charge</text>
    ${itemLabels.join("")}
  </svg>`);
  itemComposites.unshift({ input: itemSvg, left: 0, top: 0 });
  await sharp({ create: { width: itemWidth, height: 144, channels: 4, background: "#121720" } })
    .composite(itemComposites)
    .png()
    .toFile(path.join(reportDir, "emeradium_items_and_charge.png"));
}

async function makeDistinctFamiliesPreview() {
  const entries = [
    ["Platinum", path.join(itemTextureDir, "platinum_pickaxe.png")],
    ["Palladium", path.join(itemTextureDir, "palladium_pickaxe.png")],
    ["Nibirium", path.join(itemTextureDir, "nibirium_pickaxe.png")],
    ["Acid spitter", path.join(assets, "textures", "entity", "acid_spitter.png")],
    ["Plasma caster", path.join(assets, "textures", "entity", "plasma_caster.png")],
    ["Anvil", path.join(blockTextureDir, "platinum_anvil_top.png")],
    ["Chipped", path.join(blockTextureDir, "chipped_platinum_anvil_top.png")],
    ["Damaged", path.join(blockTextureDir, "damaged_platinum_anvil_top.png")],
  ];
  const width = 8 * 126;
  const height = 180;
  const composites = [];
  const labels = [];
  for (let index = 0; index < entries.length; index++) {
    const [label, file] = entries[index];
    const left = index * 126 + 15;
    const image = await sharp(file)
      .resize(96, 96, { kernel: "nearest", fit: "contain" })
      .png()
      .toBuffer();
    composites.push({ input: image, left, top: 48 });
    labels.push(`<text x="${left + 48}" y="163" text-anchor="middle" fill="#d5dce5" font-family="Segoe UI" font-size="11">${label}</text>`);
  }
  const svg = Buffer.from(`<svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
    <rect width="100%" height="100%" fill="#121720"/>
    <text x="16" y="26" fill="#f4f7fa" font-family="Segoe UI" font-size="18" font-weight="700">Distinct texture families</text>
    ${labels.join("")}
  </svg>`);
  composites.unshift({ input: svg, left: 0, top: 0 });
  await sharp({ create: { width, height, channels: 4, background: "#121720" } })
    .composite(composites)
    .png()
    .toFile(path.join(reportDir, "distinct_texture_families.png"));
}

async function main() {
  ensureDirs();
  extractVanillaAssets();

  const blocks = new Map();
  for (const entry of blockEntries) {
    const [id, textureName] = entry;
    const base = await createBlockTexture(id);
    blocks.set(id, base);
    await writeTexture(path.join(blockTextureDir, `${textureName}.png`), base);
  }

  const reactorTop = await createReactorTop();
  await writeTexture(path.join(blockTextureDir, "planet_reactor_top.png"), reactorTop);

  for (const entry of blockEntries.slice(0, -1)) {
    const [id] = entry;
    const base = blocks.get(id);
    const variants = {
      infested: infect(base, id),
      bloody: bloody(base, id),
      bloody_infested: bloody(infect(base, id), id),
    };
    for (const [variant, image] of Object.entries(variants)) {
      await writeTexture(path.join(blockTextureDir, `${variant}_${id}.png`), image);
      if (id === "planet_reactor") {
        const top = variant === "infested"
          ? infect(reactorTop, "planet_reactor_top")
          : variant === "bloody"
            ? bloody(reactorTop, "planet_reactor_top")
            : bloody(infect(reactorTop, "planet_reactor_top"), "planet_reactor_top");
        await writeTexture(
          path.join(blockTextureDir, `${variant}_planet_reactor_top.png`),
          top,
        );
      }
      writeVariantResources(entry, variant);
    }
  }

  const turretHead = recolor(blocks.get("plasma_turret"), palettes.palladium, 0.42);
  addMachineLights(turretHead, rgba("#87fff1"));
  await writeTexture(path.join(blockTextureDir, "plasma_turret_head.png"), turretHead);

  await createAnvils();
  await createEmeradiumItems();
  await createAstralPrismItems();
  await createFleshItems();
  await createMaterialTools();
  await createDistinctHumanoids();
  removeUnusedDuplicateTextures();
  await makePreview(blocks);
  await makeDistinctFamiliesPreview();
  console.log(`Generated ${blockEntries.length} vanilla-style block bases, variants, anvils, and mod items.`);
}

await main();
