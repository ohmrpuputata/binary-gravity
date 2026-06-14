const fs = require("node:fs");
const path = require("node:path");
const sharp = require("sharp");

const root = path.resolve(__dirname, "..");
const source = path.join(root, "art", "mod_blocks_redesign_reference.png");
const assets = path.join(root, "src", "main", "resources", "assets", "alien-invasion");
const data = path.join(root, "src", "main", "resources", "data", "alien-invasion");
const textureDir = path.join(assets, "textures", "block");
const blockModelDir = path.join(assets, "models", "block");
const itemModelDir = path.join(assets, "models", "item");
const blockstateDir = path.join(assets, "blockstates");
const lootDir = path.join(data, "loot_table", "blocks");
const reportDir = path.join(root, "build", "reports");

const entries = [
  ["alien_residue", "alien_residue", "Alien Residue", "Инопланетный осадок"],
  ["pure_radiation_block", "pure_radiation_block", "Pure Radiation Block", "Блок чистой радиации"],
  ["cosmic_crystal_ore", "cosmic_ore", "Cosmic Crystal Ore", "Руда космического кристалла"],
  ["platinum_ore", "platinum_ore", "Platinum Ore", "Платиновая руда"],
  ["platinum_block", "platinum_block", "Platinum Block", "Платиновый блок"],
  ["palladium_ore", "palladium_ore", "Palladium Ore", "Палладиевая руда"],
  ["palladium_block", "palladium_block", "Palladium Block", "Палладиевый блок"],
  ["alien_flesh", "alien_flesh", "Alien Flesh", "Инопланетная плоть"],
  ["alien_hive", "alien_hive", "Alien Hive", "Инопланетный улей"],
  ["alien_heart", "alien_heart", "Alien Heart", "Инопланетное сердце"],
  ["radio_transmitter", "radio_transmitter", "Radio Transmitter", "Радиопередатчик"],
  ["alien_tendrils", "alien_tendrils", "Alien Tendrils", "Щупальца Роя", true],
  ["purifier", "purifier", "Purifier", "Очиститель"],
  ["alien_stash", "alien_stash", "Alien Stash", "Тайник пришельцев"],
  ["alien_beacon", "alien_beacon", "Alien Beacon", "Маяк пришельцев"],
  ["swarm_beacon", "swarm_beacon", "Swarm Beacon", "Маяк Роя"],
  ["dark_matter_ore", "dark_matter_ore", "Dark Matter Ore", "Руда тёмной материи"],
  ["plasma_turret", "plasma_turret", "Plasma Turret", "Плазменная турель"],
  ["black_market_terminal", "black_market_terminal", "Black Market Terminal", "Терминал чёрного рынка"],
  ["purifier_station", "purifier_station", "Purifier Station", "Станция очистки"],
  ["ore_washer", "ore_washer", "Ore Washer", "Промыватель руды"],
  ["blueprint_table", "blueprint_table", "Blueprint Table", "Стол чертежей"],
  ["warning_lamp", "warning_lamp", "Warning Lamp", "Сигнальная лампа"],
  ["cracked_alien_pipe", "cracked_alien_pipe", "Cracked Alien Pipe", "Треснувшая инопланетная труба"],
  ["toxic_barrel", "toxic_barrel", "Toxic Barrel", "Токсичная бочка"],
  ["broken_lab_crate", "broken_lab_crate", "Broken Lab Crate", "Сломанный лабораторный ящик"],
  ["contaminated_bones", "contaminated_bones", "Contaminated Bones", "Заражённые кости"],
  ["alien_portal", "alien_portal", "Alien Portal", "Разрыв пространства"],
  ["planet_reactor", "planet_reactor_side", "Planet Reactor", "Реактор Максбетова"],
  ["blood_pool", "blood_pool", "Blood Pool", "Кровавая лужа", true],
];

const xRanges = [[11, 246], [258, 496], [508, 744], [756, 994], [1006, 1242]];
const yRanges = [[10, 225], [237, 450], [462, 674], [686, 898], [910, 1098], [1110, 1242]];
const variants = ["infested", "bloody", "bloody_infested"];

function ensureDirs() {
  for (const dir of [textureDir, blockModelDir, itemModelDir, blockstateDir, lootDir, reportDir]) {
    fs.mkdirSync(dir, { recursive: true });
  }
}

function hash(text) {
  let value = 2166136261;
  for (const char of text) {
    value ^= char.charCodeAt(0);
    value = Math.imul(value, 16777619);
  }
  return value >>> 0;
}

function setPixel(buffer, x, y, color, onlyOpaque = true) {
  if (x < 0 || x >= 16 || y < 0 || y >= 16) return;
  const offset = (y * 16 + x) * 4;
  if (onlyOpaque && buffer[offset + 3] < 32) return;
  buffer[offset] = color[0];
  buffer[offset + 1] = color[1];
  buffer[offset + 2] = color[2];
  buffer[offset + 3] = color[3] ?? 255;
}

function tint(buffer, color, amount) {
  const output = Buffer.from(buffer);
  for (let offset = 0; offset < output.length; offset += 4) {
    if (output[offset + 3] < 32) continue;
    for (let channel = 0; channel < 3; channel++) {
      output[offset + channel] = Math.round(output[offset + channel] * (1 - amount) + color[channel] * amount);
    }
  }
  return output;
}

function infect(buffer, id) {
  const output = tint(buffer, [104, 28, 142], 0.25);
  const seed = hash(id);
  const rising = (seed & 1) === 0;
  for (let x = -1; x < 17; x++) {
    const wave = Math.round(Math.sin((x + (seed % 7)) * 0.72) * 1.35);
    const y = rising ? 14 - Math.round(x * 0.68) + wave : 2 + Math.round(x * 0.68) + wave;
    for (let oy = -1; oy <= 1; oy++) setPixel(output, x, y + oy, [43, 7, 61, 255]);
    setPixel(output, x, y, [151, 38, 201, 255]);
    if (x % 3 === 0) setPixel(output, x, y - 1, [204, 72, 239, 255]);
  }
  const branchStart = 5 + (seed % 5);
  for (let step = 0; step < 7; step++) {
    const x = branchStart + step;
    const y = rising ? 10 - Math.floor(step / 2) : 6 + Math.floor(step / 2);
    setPixel(output, x, y, [116, 25, 166, 255]);
  }
  for (let i = 0; i < 5; i++) {
    const x = (seed >>> (i * 3)) & 15;
    const y = (seed >>> (i * 4 + 2)) & 15;
    setPixel(output, x, y, i === 0 ? [184, 255, 72, 255] : [102, 218, 53, 255]);
  }
  return output;
}

function blood(buffer, id) {
  const output = tint(buffer, [105, 8, 18], 0.1);
  const seed = hash(`${id}:blood`);
  const baseY = 3 + (seed % 9);
  for (let x = 0; x < 16; x++) {
    const y = Math.max(1, Math.min(14, baseY + Math.round(Math.sin((x + seed % 5) * 0.85) * 2)));
    setPixel(output, x, y, [76, 3, 10, 255]);
    if ((x + seed) % 3 !== 0) setPixel(output, x, y + 1, [132, 7, 18, 255]);
    if ((x + seed) % 5 === 0) setPixel(output, x, y, [190, 14, 29, 255]);
  }
  for (let i = 0; i < 7; i++) {
    const x = (seed >>> (i * 3)) & 15;
    const y = (seed >>> (i * 2 + 1)) & 15;
    setPixel(output, x, y, i < 2 ? [202, 19, 36, 255] : [109, 4, 15, 255]);
    if (i < 3) setPixel(output, x + 1, y, [79, 2, 10, 255]);
  }
  return output;
}

async function writeTexture(file, raw) {
  await sharp(raw, { raw: { width: 16, height: 16, channels: 4 } })
    .png({ palette: true, colours: 48, dither: 0 })
    .toFile(file);
}

async function extractBase(entry, index) {
  const [id, textureName, , , transparent] = entry;
  const col = index % 5;
  const row = Math.floor(index / 5);
  const [left, right] = xRanges[col];
  const [top, bottom] = yRanges[row];
  const result = await sharp(source)
    .extract({ left, top, width: right - left + 1, height: bottom - top + 1 })
    .resize(16, 16, { fit: "fill", kernel: "nearest" })
    .ensureAlpha()
    .raw()
    .toBuffer();

  if (transparent) {
    for (let offset = 0; offset < result.length; offset += 4) {
      const r = result[offset];
      const g = result[offset + 1];
      const b = result[offset + 2];
      const max = Math.max(r, g, b);
      const chroma = max - Math.min(r, g, b);
      const remove = id === "blood_pool"
        ? (max < 50 && chroma < 28)
        : (max < 55 && chroma < 34);
      result[offset + 3] = remove ? 0 : 255;
    }
  }

  await writeTexture(path.join(textureDir, `${textureName}.png`), result);
  if (id === "planet_reactor") {
    const topRaw = Buffer.from(result);
    for (let y = 0; y < 16; y++) {
      for (let x = 0; x < 16; x++) {
        const distance = Math.max(Math.abs(x - 7.5), Math.abs(y - 7.5));
        if (distance < 4.2) setPixel(topRaw, x, y, [255, 153 + ((x + y) % 2) * 35, 25, 255], false);
      }
    }
    await writeTexture(path.join(textureDir, "planet_reactor_top.png"), topRaw);
  }
  if (id === "plasma_turret") {
    const head = tint(result, [18, 218, 235], 0.18);
    await writeTexture(path.join(textureDir, "plasma_turret_head.png"), head);
  }
  return result;
}

function replaceTextures(value, replacements) {
  if (typeof value === "string") {
    let result = value;
    for (const [from, to] of replacements) result = result.replaceAll(from, to);
    return result;
  }
  if (Array.isArray(value)) return value.map(item => replaceTextures(item, replacements));
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, replaceTextures(item, replacements)]));
  }
  return value;
}

function writeJson(file, value) {
  fs.writeFileSync(file, `${JSON.stringify(value, null, 2)}\n`);
}

function writeResources(entry, variant) {
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

function updateLanguage(file, locale) {
  const language = JSON.parse(fs.readFileSync(file, "utf8"));
  for (const [id, , en, ru] of entries.slice(0, -1)) {
    const name = locale === "ru" ? ru : en;
    language[`block.alien-invasion.infested_${id}`] =
      locale === "ru" ? `${name} (заражённый)` : `Infested ${name}`;
    language[`block.alien-invasion.bloody_${id}`] =
      locale === "ru" ? `${name} (в крови)` : `Bloody ${name}`;
    language[`block.alien-invasion.bloody_infested_${id}`] =
      locale === "ru" ? `${name} (заражённый, в крови)` : `Bloody Infested ${name}`;
  }
  writeJson(file, language);
}

async function makePreview(baseTextures) {
  const cols = 5;
  const cardW = 244;
  const cardH = 142;
  const width = cols * cardW;
  const height = 64 + Math.ceil((entries.length - 1) / cols) * cardH;
  const escape = text => text.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
  const svg = `<svg width="${width}" height="${height}" xmlns="http://www.w3.org/2000/svg">
  <rect width="100%" height="100%" fill="#11151d"/>
  <text x="20" y="31" fill="#f3f5f7" font-family="Segoe UI,Arial" font-size="22" font-weight="700">Mod block redesign variants</text>
  <text x="20" y="51" fill="#8f9bad" font-family="Segoe UI,Arial" font-size="12">base / infested / bloody / combined</text>
  ${entries.slice(0, -1).map((entry, index) => {
    const x = (index % cols) * cardW + 7;
    const y = 64 + Math.floor(index / cols) * cardH + 7;
    return `<rect x="${x}" y="${y}" width="${cardW - 14}" height="${cardH - 14}" rx="7" fill="#1b2230" stroke="#303a4b"/>
      <text x="${x + (cardW - 14) / 2}" y="${y + 116}" text-anchor="middle" fill="#d9e0e9" font-family="Consolas,monospace" font-size="10">${escape(entry[0])}</text>`;
  }).join("")}
  </svg>`;
  const layers = [{ input: Buffer.from(svg), left: 0, top: 0 }];
  for (let index = 0; index < entries.length - 1; index++) {
    const [id] = entries[index];
    const raws = [
      baseTextures.get(id),
      infect(baseTextures.get(id), id),
      blood(baseTextures.get(id), id),
      blood(infect(baseTextures.get(id), id), id),
    ];
    const cardX = (index % cols) * cardW + 18;
    const cardY = 64 + Math.floor(index / cols) * cardH + 21;
    for (let variant = 0; variant < 4; variant++) {
      const image = await sharp(raws[variant], { raw: { width: 16, height: 16, channels: 4 } })
        .resize(48, 48, { kernel: "nearest" })
        .png()
        .toBuffer();
      layers.push({ input: image, left: cardX + variant * 54, top: cardY });
    }
  }
  await sharp({ create: { width, height, channels: 4, background: "#11151d" } })
    .composite(layers)
    .png()
    .toFile(path.join(reportDir, "mod_block_redesign_variants.png"));
}

async function main() {
  ensureDirs();
  const baseTextures = new Map();
  for (let index = 0; index < entries.length; index++) {
    const entry = entries[index];
    baseTextures.set(entry[0], await extractBase(entry, index));
  }
  for (const entry of entries.slice(0, -1)) {
    const [id] = entry;
    const base = baseTextures.get(id);
    const generated = {
      infested: infect(base, id),
      bloody: blood(base, id),
      bloody_infested: blood(infect(base, id), id),
    };
    for (const variant of variants) {
      await writeTexture(path.join(textureDir, `${variant}_${id}.png`), generated[variant]);
      if (id === "planet_reactor") {
        await writeTexture(path.join(textureDir, `${variant}_planet_reactor_side.png`), generated[variant]);
        const top = Buffer.from(generated[variant]);
        for (let y = 4; y < 12; y++) for (let x = 4; x < 12; x++) {
          if (x === 4 || x === 11 || y === 4 || y === 11) {
            setPixel(top, x, y, variant === "infested" ? [181, 53, 225, 255] : [165, 14, 29, 255], false);
          }
        }
        await writeTexture(path.join(textureDir, `${variant}_planet_reactor_top.png`), top);
      }
      writeResources(entry, variant);
    }
  }
  updateLanguage(path.join(assets, "lang", "en_us.json"), "en");
  updateLanguage(path.join(assets, "lang", "ru_ru.json"), "ru");
  await makePreview(baseTextures);
  console.log(`Generated ${entries.length} redesigned bases and ${(entries.length - 1) * variants.length} variants.`);
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
