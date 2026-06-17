import path from "node:path";
import sharp from "sharp";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const vanilla = path.join(root, "build", "vanilla-texture-source", "assets", "minecraft", "textures");
const modTextures = path.join(root, "src", "main", "resources", "assets", "alien-invasion", "textures");

const clamp = (value) => Math.max(0, Math.min(255, Math.round(value)));
const lerp = (a, b, t) => a + (b - a) * t;
const mix = (a, b, t) => [
  clamp(lerp(a[0], b[0], t)),
  clamp(lerp(a[1], b[1], t)),
  clamp(lerp(a[2], b[2], t)),
  255,
];

function luminance(r, g, b) {
  return (r * 0.2126 + g * 0.7152 + b * 0.0722) / 255;
}

function ramp(value, colors) {
  if (value <= 0.5) {
    return mix(colors[0], colors[1], value * 2);
  }
  return mix(colors[1], colors[2], (value - 0.5) * 2);
}

async function recolorPng(source, destination, colors, accent = null, curvePower = 1) {
  const { data, info } = await sharp(source)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });

  for (let i = 0; i < data.length; i += 4) {
    const alpha = data[i + 3];
    if (alpha === 0) {
      continue;
    }
    const value = Math.pow(luminance(data[i], data[i + 1], data[i + 2]), curvePower);
    const color = ramp(value, colors);
    data[i] = color[0];
    data[i + 1] = color[1];
    data[i + 2] = color[2];
    data[i + 3] = alpha;
  }

  if (accent) {
    for (const [x, y, color] of accent) {
      const offset = (y * info.width + x) * 4;
      if (offset >= 0 && offset < data.length && data[offset + 3] > 0) {
        data[offset] = color[0];
        data[offset + 1] = color[1];
        data[offset + 2] = color[2];
      }
    }
  }

  await sharp(data, {
    raw: {
      width: info.width,
      height: info.height,
      channels: 4,
    },
  }).png().toFile(destination);
}

const palladium = [
  [40, 73, 77],
  [96, 149, 146],
  [185, 231, 219],
];
const platinum = [
  [58, 62, 70],
  [139, 148, 158],
  [238, 244, 246],
];
const nibirium = [
  [22, 12, 40],
  [80, 42, 119],
  [211, 164, 245],
];
const nibiriumAccent = [99, 226, 193, 255];

const block = (name) => path.join(modTextures, "block", name);
const item = (name) => path.join(modTextures, "item", name);
const vanillaBlock = (name) => path.join(vanilla, "block", name);
const vanillaItem = (name) => path.join(vanilla, "item", name);

await recolorPng(vanillaBlock("anvil.png"), block("palladium_anvil_side.png"), palladium);
await recolorPng(vanillaBlock("anvil_top.png"), block("palladium_anvil_top.png"), palladium, [
  [7, 4, [116, 238, 211, 255]],
  [8, 4, [116, 238, 211, 255]],
]);
await recolorPng(vanillaBlock("chipped_anvil_top.png"), block("chipped_palladium_anvil_top.png"), palladium, [
  [5, 5, [116, 238, 211, 255]],
]);
await recolorPng(vanillaBlock("damaged_anvil_top.png"), block("damaged_palladium_anvil_top.png"), palladium, [
  [10, 4, [116, 238, 211, 255]],
]);

await recolorPng(vanillaBlock("anvil.png"), block("platinum_anvil_side.png"), platinum);
await recolorPng(vanillaBlock("anvil_top.png"), block("platinum_anvil_top.png"), platinum, [
  [7, 4, [204, 245, 255, 255]],
  [8, 4, [204, 245, 255, 255]],
]);
await recolorPng(vanillaBlock("chipped_anvil_top.png"), block("chipped_platinum_anvil_top.png"), platinum, [
  [5, 5, [204, 245, 255, 255]],
]);
await recolorPng(vanillaBlock("damaged_anvil_top.png"), block("damaged_platinum_anvil_top.png"), platinum, [
  [10, 4, [204, 245, 255, 255]],
]);

const tools = ["axe", "hoe", "pickaxe", "shovel", "sword"];
for (const tool of tools) {
  await recolorPng(vanillaItem(`netherite_${tool}.png`), item(`nibirium_${tool}.png`), nibirium, [
    [10, 5, nibiriumAccent],
    [11, 5, nibiriumAccent],
    [12, 4, nibiriumAccent],
  ], 0.45);
}

console.log("Fixed flat anvil textures and low-readability nibirium tools.");
