import fs from "node:fs/promises";
import path from "node:path";
import sharp from "sharp";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const vanillaBlockDir = path.join(
  root,
  "build", "vanilla-texture-source", "assets", "minecraft", "textures", "block",
);
const blockDir = path.join(
  root,
  "src", "main", "resources", "assets", "alien-invasion", "textures", "block",
);

const clamp = (value) => Math.max(0, Math.min(255, Math.round(value)));
const mix = (a, b, t) => a.map((value, index) => clamp(value + (b[index] - value) * t));
const luminance = (r, g, b) => (r * 0.2126 + g * 0.7152 + b * 0.0722) / 255;

const grass = {
  dark: [45, 82, 32, 255],
  mid: [91, 145, 54, 255],
  light: [132, 182, 71, 255],
};
const infection = {
  purpleDark: [92, 37, 124, 255],
  purple: [152, 55, 202, 255],
  green: [116, 226, 62, 255],
};
const blood = {
  dark: [74, 5, 12, 255],
  mid: [132, 12, 22, 255],
  light: [198, 29, 42, 255],
};

async function loadPng(file) {
  const { data, info } = await sharp(file)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });
  return { data: new Uint8ClampedArray(data), info };
}

async function writePng(file, image) {
  await sharp(Buffer.from(image.data), {
    raw: {
      width: image.info.width,
      height: image.info.height,
      channels: 4,
    },
  }).png().toFile(file);
}

function pixelOffset(width, x, y) {
  return (y * width + x) * 4;
}

function setPixel(image, x, y, color) {
  if (x < 0 || y < 0 || x >= image.info.width || y >= image.info.height) {
    return;
  }
  const offset = pixelOffset(image.info.width, x, y);
  if (image.data[offset + 3] === 0) {
    return;
  }
  image.data[offset] = color[0];
  image.data[offset + 1] = color[1];
  image.data[offset + 2] = color[2];
  image.data[offset + 3] = color[3];
}

function grassColor(value) {
  if (value < 0.5) {
    return mix(grass.dark, grass.mid, value * 2);
  }
  return mix(grass.mid, grass.light, (value - 0.5) * 2);
}

function colorizeGrassTop(image) {
  for (let i = 0; i < image.data.length; i += 4) {
    if (image.data[i + 3] === 0) {
      continue;
    }
    const value = Math.pow(luminance(image.data[i], image.data[i + 1], image.data[i + 2]), 0.72);
    const color = grassColor(value);
    image.data[i] = color[0];
    image.data[i + 1] = color[1];
    image.data[i + 2] = color[2];
  }
  return image;
}

function colorizeGrassStrip(image) {
  for (let y = 0; y < Math.min(5, image.info.height); y++) {
    for (let x = 0; x < image.info.width; x++) {
      const offset = pixelOffset(image.info.width, x, y);
      if (image.data[offset + 3] === 0) {
        continue;
      }
      const value = Math.pow(luminance(image.data[offset], image.data[offset + 1], image.data[offset + 2]), 0.72);
      const color = grassColor(value);
      image.data[offset] = color[0];
      image.data[offset + 1] = color[1];
      image.data[offset + 2] = color[2];
    }
  }
  return image;
}

function addBloodDecal(image, mask) {
  for (const [x, y, color] of mask) {
    setPixel(image, x, y, color);
  }
  return image;
}

async function bloodMaskFromPlanks() {
  const source = await loadPng(path.join(blockDir, "bloody_planks.png"));
  const mask = [];
  for (let y = 0; y < source.info.height; y++) {
    for (let x = 0; x < source.info.width; x++) {
      const offset = pixelOffset(source.info.width, x, y);
      const alpha = source.data[offset + 3];
      if (alpha === 0) {
        continue;
      }
      const r = source.data[offset];
      const g = source.data[offset + 1];
      const b = source.data[offset + 2];
      if (isBloodish(r, g, b)) {
        mask.push([x, y, normalizeBloodPixel(r)]);
      }
    }
  }
  return mask;
}

function addInfection(image, variant = "top") {
  const mask = variant === "side"
    ? [
      [1, 4, infection.purpleDark], [2, 5, infection.purple], [3, 6, infection.purple],
      [4, 7, infection.purpleDark], [12, 2, infection.green], [13, 3, infection.green],
      [7, 10, infection.green],
    ]
    : [
      [1, 4, infection.purpleDark], [2, 5, infection.purple], [3, 6, infection.purple],
      [4, 7, infection.purpleDark], [12, 3, infection.green], [14, 6, infection.green],
      [7, 12, infection.green],
    ];
  for (const [x, y, color] of mask) {
    setPixel(image, x, y, color);
  }
  return image;
}

function isBloodish(r, g, b) {
  return r >= 72 && g <= 80 && b <= 86 && r > g * 1.35 && r > b * 1.2;
}

function normalizeBloodPixel(r) {
  if (r > 175) {
    return blood.light;
  }
  if (r > 115) {
    return blood.mid;
  }
  return blood.dark;
}

async function normalizeBloodPalette(file) {
  const image = await loadPng(file);
  let changed = false;
  for (let i = 0; i < image.data.length; i += 4) {
    const alpha = image.data[i + 3];
    if (alpha === 0) {
      continue;
    }
    const r = image.data[i];
    const g = image.data[i + 1];
    const b = image.data[i + 2];
    if (!isBloodish(r, g, b)) {
      continue;
    }
    const color = normalizeBloodPixel(r);
    image.data[i] = color[0];
    image.data[i + 1] = color[1];
    image.data[i + 2] = color[2];
    image.data[i + 3] = alpha;
    changed = true;
  }
  if (changed) {
    await writePng(file, image);
  }
}

async function main() {
  const grassTop = colorizeGrassTop(await loadPng(path.join(vanillaBlockDir, "grass_block_top.png")));
  const grassSide = colorizeGrassStrip(await loadPng(path.join(vanillaBlockDir, "grass_block_side.png")));
  const plankBloodMask = await bloodMaskFromPlanks();

  await writePng(path.join(blockDir, "infested_grass_top.png"), addInfection(structuredClone(grassTop), "top"));
  await writePng(path.join(blockDir, "infested_grass_side.png"), addInfection(structuredClone(grassSide), "side"));

  await writePng(path.join(blockDir, "bloody_grass_top.png"), addBloodDecal(structuredClone(grassTop), plankBloodMask));
  await writePng(path.join(blockDir, "bloody_grass_side.png"), addBloodDecal(structuredClone(grassSide), plankBloodMask));
  await writePng(path.join(blockDir, "bloody_grass.png"), addBloodDecal(structuredClone(grassTop), plankBloodMask));

  await writePng(
    path.join(blockDir, "bloody_infested_grass_top.png"),
    addBloodDecal(addInfection(structuredClone(grassTop), "top"), plankBloodMask),
  );
  await writePng(
    path.join(blockDir, "bloody_infested_grass_side.png"),
    addBloodDecal(addInfection(structuredClone(grassSide), "side"), plankBloodMask),
  );
  await writePng(
    path.join(blockDir, "bloody_infested_grass.png"),
    addBloodDecal(addInfection(structuredClone(grassTop), "top"), plankBloodMask),
  );

  const blockFiles = await fs.readdir(blockDir);
  for (const name of blockFiles) {
    if (name.startsWith("bloody_") && name.endsWith(".png")) {
      await normalizeBloodPalette(path.join(blockDir, name));
    }
  }

  console.log("Fixed bloody grass color and normalized bloody block decal palette.");
}

await main();
