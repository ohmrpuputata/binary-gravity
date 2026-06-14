import path from "node:path";
import sharp from "sharp";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const itemDir = path.join(
  root,
  "src", "main", "resources", "assets", "alien-invasion", "textures", "item",
);

const palettes = {
  alien: {
    cool: [116, 255, 72, 255],
    hot: [255, 226, 84, 255],
    flash: [229, 255, 205, 255],
  },
  gravity: {
    cool: [70, 229, 242, 255],
    hot: [169, 87, 255, 255],
    flash: [220, 255, 255, 255],
  },
  astral: {
    cool: [72, 231, 255, 255],
    hot: [179, 83, 255, 255],
    flash: [240, 247, 255, 255],
  },
};

function setPixel(data, width, height, x, y, color) {
  if (x < 0 || y < 0 || x >= width || y >= height) {
    return;
  }
  const offset = (y * width + x) * 4;
  data[offset] = color[0];
  data[offset + 1] = color[1];
  data[offset + 2] = color[2];
  data[offset + 3] = color[3];
}

function addCross(data, width, height, x, y, color, radius) {
  setPixel(data, width, height, x, y, color);
  for (let distance = 1; distance <= radius; distance++) {
    const faded = [
      color[0],
      color[1],
      color[2],
      Math.max(150, color[3] - distance * 45),
    ];
    setPixel(data, width, height, x + distance, y, faded);
    setPixel(data, width, height, x - distance, y, faded);
    setPixel(data, width, height, x, y + distance, faded);
    setPixel(data, width, height, x, y - distance, faded);
  }
}

async function variant(sourceName, outputName, painter) {
  const source = path.join(itemDir, sourceName);
  const { data, info } = await sharp(source)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });
  painter(data, info.width, info.height);
  await sharp(data, {
    raw: {
      width: info.width,
      height: info.height,
      channels: 4,
    },
  }).png().toFile(path.join(itemDir, outputName));
}

function scalePoint(width, height, x32, y32) {
  return [
    Math.round(x32 * width / 32),
    Math.round(y32 * height / 32),
  ];
}

async function makeStateSet(prefix, sourceName, palette, core32, muzzle32, withCharge) {
  const [coreX32, coreY32] = core32;
  const [muzzleX32, muzzleY32] = muzzle32;

  await variant(sourceName, `${prefix}_cooling.png`, (data, width, height) => {
    const [coreX, coreY] = scalePoint(width, height, coreX32, coreY32);
    const [muzzleX, muzzleY] = scalePoint(width, height, muzzleX32, muzzleY32);
    addCross(data, width, height, coreX, coreY, palette.hot, width >= 32 ? 2 : 1);
    setPixel(data, width, height, muzzleX, muzzleY, palette.cool);
  });

  await variant(sourceName, `${prefix}_firing.png`, (data, width, height) => {
    const [coreX, coreY] = scalePoint(width, height, coreX32, coreY32);
    const [muzzleX, muzzleY] = scalePoint(width, height, muzzleX32, muzzleY32);
    addCross(data, width, height, coreX, coreY, palette.flash, width >= 32 ? 2 : 1);
    addCross(data, width, height, muzzleX, muzzleY, palette.flash, width >= 32 ? 3 : 2);
    setPixel(data, width, height, muzzleX - 2, muzzleY + 1, palette.hot);
  });

  if (!withCharge) {
    return;
  }

  for (let stage = 1; stage <= 3; stage++) {
    await variant(sourceName, `${prefix}_charge_${stage}.png`, (data, width, height) => {
      const [coreX, coreY] = scalePoint(width, height, coreX32, coreY32);
      const [muzzleX, muzzleY] = scalePoint(width, height, muzzleX32, muzzleY32);
      const color = stage === 3 ? palette.flash : palette.cool;
      addCross(data, width, height, coreX, coreY, color, stage === 3 && width >= 32 ? 2 : 1);
      addCross(data, width, height, muzzleX, muzzleY, color, Math.min(stage, 2));
    });
  }
}

await makeStateSet(
  "alien_blaster",
  "alien_blaster.png",
  palettes.alien,
  [13, 13],
  [28, 5],
  true,
);

await makeStateSet(
  "gravity_gun",
  "gravity_gun.png",
  palettes.gravity,
  [16, 13],
  [27, 7],
  false,
);

await makeStateSet(
  "astral_prism_gun",
  "astral_prism_gun.png",
  palettes.astral,
  [16, 12],
  [29, 8],
  true,
);

await variant(
  "green_ray_blaster.png",
  "green_ray_blaster_cooling.png",
  (data, width, height) => {
    addCross(data, width, height, 11, 7, palettes.alien.hot, 1);
    setPixel(data, width, height, 15, 4, palettes.alien.cool);
  },
);

await variant(
  "green_ray_blaster_charge_3.png",
  "green_ray_blaster_firing.png",
  (data, width, height) => {
    addCross(data, width, height, 11, 7, palettes.alien.flash, 1);
    addCross(data, width, height, 15, 4, palettes.alien.flash, 2);
  },
);

console.log("Generated animated blaster texture states.");
