import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const textures = path.join(
  root,
  "src", "main", "resources", "assets", "alien-invasion", "textures",
);
const itemDir = path.join(textures, "item");
const armorDir = path.join(textures, "models", "armor");
const reportDir = path.join(root, "build", "reports");

const armorSources = [
  ["platinum_layer_1.png", "astral_prism_layer_1.png"],
  ["platinum_layer_2.png", "astral_prism_layer_2.png"],
];

const itemSources = [
  ["platinum_helmet.png", "astral_prism_helmet.png"],
  ["platinum_chestplate.png", "astral_prism_chestplate.png"],
  ["platinum_leggings.png", "astral_prism_leggings.png"],
  ["platinum_boots.png", "astral_prism_boots.png"],
];

const clamp = (value, min = 0, max = 255) =>
  Math.max(min, Math.min(max, Math.round(value)));

function mix(a, b, amount) {
  return a.map((value, index) => clamp(value + (b[index] - value) * amount));
}

async function recolorAstral(source, destination) {
  const { data, info } = await sharp(source)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });

  for (let index = 0; index < data.length; index += 4) {
    const alpha = data[index + 3];
    if (alpha === 0) continue;

    const red = data[index];
    const green = data[index + 1];
    const blue = data[index + 2];
    const luminance = (red * 0.24 + green * 0.58 + blue * 0.18) / 255;
    const cyanAccent = blue - red > 80 && green - red > 55;

    let color;
    if (cyanAccent) {
      color = mix([22, 99, 142], [105, 241, 255], Math.min(1, luminance * 1.25));
    } else if (luminance < 0.38) {
      color = mix([31, 16, 69], [78, 39, 145], luminance / 0.38);
    } else {
      color = mix([78, 39, 145], [184, 116, 255], (luminance - 0.38) / 0.62);
    }

    data[index] = color[0];
    data[index + 1] = color[1];
    data[index + 2] = color[2];
  }

  await sharp(data, {
    raw: {
      width: info.width,
      height: info.height,
      channels: 4,
    },
  }).png().toFile(destination);
}

function createCanvas() {
  return new Uint8Array(16 * 16 * 4);
}

function setPixel(canvas, x, y, color) {
  if (x < 0 || y < 0 || x >= 16 || y >= 16) return;
  const index = (y * 16 + x) * 4;
  canvas[index] = color[0];
  canvas[index + 1] = color[1];
  canvas[index + 2] = color[2];
  canvas[index + 3] = color[3] ?? 255;
}

function shieldBounds(y) {
  if (y === 0) return [6, 9];
  if (y === 1) return [4, 11];
  if (y <= 4) return [3, 12];
  if (y <= 9) return [2, 13];
  if (y <= 11) return [3, 12];
  if (y <= 13) return [4, 11];
  if (y === 14) return [5, 10];
  return [7, 8];
}

function createShieldFace(back = false) {
  const canvas = createCanvas();
  for (let y = 0; y < 16; y++) {
    const [left, right] = shieldBounds(y);
    for (let x = left; x <= right; x++) {
      const edge = x === left || x === right || y === 0 || y === 15
        || (y > 0 && (
          x < shieldBounds(y - 1)[0] || x > shieldBounds(y - 1)[1]
        ))
        || (y < 15 && (
          x < shieldBounds(y + 1)[0] || x > shieldBounds(y + 1)[1]
        ));
      const stripe = (x + y) % 4 === 0;
      let color = edge
        ? [8, 58, 42]
        : stripe ? [36, 173, 94] : [24, 137, 74];
      if (back) {
        color = edge
          ? [13, 49, 39]
          : stripe ? [31, 113, 75] : [23, 82, 61];
      }
      setPixel(canvas, x, y, color);
    }
  }

  if (back) {
    for (let y = 5; y <= 11; y++) {
      setPixel(canvas, 7, y, [55, 67, 64]);
      setPixel(canvas, 8, y, [92, 105, 100]);
    }
    for (let x = 5; x <= 10; x++) {
      setPixel(canvas, x, 5, [50, 61, 58]);
      setPixel(canvas, x, 11, [50, 61, 58]);
    }
  } else {
    for (let y = 4; y <= 11; y++) {
      const halfWidth = y < 6 || y > 9 ? 1 : 2;
      for (let x = 8 - halfWidth; x <= 7 + halfWidth; x++) {
        const edge = x === 8 - halfWidth || x === 7 + halfWidth
          || y === 4 || y === 11;
        setPixel(canvas, x, y, edge ? [17, 88, 91] : [90, 239, 220]);
      }
    }
    setPixel(canvas, 7, 6, [210, 255, 237]);
    setPixel(canvas, 8, 7, [174, 255, 230]);
    setPixel(canvas, 7, 9, [58, 210, 190]);
    setPixel(canvas, 8, 10, [35, 171, 159]);
  }
  return canvas;
}

function createShieldMaterial(kind) {
  const canvas = createCanvas();
  const palettes = {
    edge: [[7, 48, 39], [15, 86, 58], [39, 157, 88], [91, 220, 133]],
    handle: [[35, 31, 29], [66, 57, 51], [103, 91, 78], [142, 128, 108]],
  };
  const palette = palettes[kind];
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      const shade = (x + y * 2) % 7 === 0 ? 3 : (x + y) % 3;
      setPixel(canvas, x, y, palette[shade]);
    }
  }
  return canvas;
}

async function writeRawTexture(destination, data) {
  await sharp(Buffer.from(data), {
    raw: { width: 16, height: 16, channels: 4 },
  }).png().toFile(destination);
}

async function makePreview() {
  const files = [
    "astral_prism_helmet.png",
    "astral_prism_chestplate.png",
    "astral_prism_leggings.png",
    "astral_prism_boots.png",
    "emeradium_shield.png",
    "emeradium_shield_back.png",
  ];
  const composites = [];
  for (let index = 0; index < files.length; index++) {
    const image = await sharp(path.join(itemDir, files[index]))
      .resize(128, 128, { kernel: "nearest" })
      .png()
      .toBuffer();
    composites.push({
      input: image,
      left: index * 144 + 8,
      top: 8,
    });
  }
  fs.mkdirSync(reportDir, { recursive: true });
  await sharp({
    create: {
      width: files.length * 144,
      height: 144,
      channels: 4,
      background: "#202532",
    },
  }).composite(composites)
    .png()
    .toFile(path.join(reportDir, "armor_and_shield_preview.png"));
}

fs.mkdirSync(itemDir, { recursive: true });
fs.mkdirSync(armorDir, { recursive: true });

for (const [sourceName, destinationName] of armorSources) {
  await recolorAstral(
    path.join(armorDir, sourceName),
    path.join(armorDir, destinationName),
  );
}

for (const [sourceName, destinationName] of itemSources) {
  await recolorAstral(
    path.join(itemDir, sourceName),
    path.join(itemDir, destinationName),
  );
}

await writeRawTexture(
  path.join(itemDir, "emeradium_shield.png"),
  createShieldFace(false),
);
await writeRawTexture(
  path.join(itemDir, "emeradium_shield_back.png"),
  createShieldFace(true),
);
await writeRawTexture(
  path.join(itemDir, "emeradium_shield_edge.png"),
  createShieldMaterial("edge"),
);
await writeRawTexture(
  path.join(itemDir, "emeradium_shield_handle.png"),
  createShieldMaterial("handle"),
);

await makePreview();

console.log("Generated unified Astral Prism armor and 3D shield textures.");
