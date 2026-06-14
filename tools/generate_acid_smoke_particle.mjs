import fs from "node:fs";
import path from "node:path";
import sharp from "sharp";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const textureDir = path.join(
  root,
  "src", "main", "resources", "assets", "alien-invasion", "textures", "particle",
);
const previewPath = path.join(root, "build", "reports", "acid_smoke_particle.png");
fs.mkdirSync(textureDir, { recursive: true });
fs.mkdirSync(path.dirname(previewPath), { recursive: true });

const size = 16;
const frames = [
  { radius: 3.2, holes: 0, alpha: 220 },
  { radius: 4.2, holes: 1, alpha: 205 },
  { radius: 5.3, holes: 2, alpha: 185 },
  { radius: 6.2, holes: 3, alpha: 160 },
  { radius: 7.0, holes: 4, alpha: 125 },
  { radius: 7.7, holes: 5, alpha: 88 },
];

function hash(x, y, frame) {
  let value = (x * 73856093) ^ (y * 19349663) ^ (frame * 83492791);
  value = Math.imul(value ^ (value >>> 13), 1274126177);
  return ((value ^ (value >>> 16)) >>> 0) / 0xffffffff;
}

function makeFrame(frameIndex, config) {
  const pixels = Buffer.alloc(size * size * 4);
  const cx = 7.5 + Math.sin(frameIndex * 1.7) * 0.7;
  const cy = 8.5 - frameIndex * 0.35;

  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const dx = x - cx;
      const dy = (y - cy) * 1.08;
      const wobble = (hash(x >> 1, y >> 1, frameIndex) - 0.5) * 2.1;
      const distance = Math.sqrt(dx * dx + dy * dy) + wobble;
      if (distance > config.radius) continue;

      const edge = Math.max(0, Math.min(1, (config.radius - distance) / 2.2));
      const noise = hash(x, y, frameIndex);
      if (config.holes > 0 && noise < config.holes * 0.018 && distance < config.radius - 1) {
        continue;
      }

      const core = Math.max(0, 1 - distance / Math.max(config.radius, 1));
      const offset = (y * size + x) * 4;
      pixels[offset] = Math.round(82 + core * 54 + noise * 12);
      pixels[offset + 1] = Math.round(154 + core * 75 + noise * 18);
      pixels[offset + 2] = Math.round(20 + core * 24);
      pixels[offset + 3] = Math.round(config.alpha * (0.45 + edge * 0.55));
    }
  }
  return pixels;
}

const previewTiles = [];
for (let i = 0; i < frames.length; i++) {
  const pixels = makeFrame(i, frames[i]);
  const output = path.join(textureDir, `acid_smoke_${i}.png`);
  await sharp(pixels, { raw: { width: size, height: size, channels: 4 } })
    .png({ palette: true, colours: 48, dither: 0 })
    .toFile(output);
  previewTiles.push(await sharp(output).resize(128, 128, { kernel: "nearest" }).png().toBuffer());
}

await sharp({
  create: {
    width: previewTiles.length * 144 + 16,
    height: 184,
    channels: 4,
    background: "#121720",
  },
})
  .composite([
    {
      input: Buffer.from(`<svg width="${previewTiles.length * 144 + 16}" height="48" xmlns="http://www.w3.org/2000/svg">
        <text x="16" y="29" fill="#f4f7fa" font-family="Segoe UI" font-size="18" font-weight="700">Acid smoke animation</text>
      </svg>`),
      left: 0,
      top: 0,
    },
    ...previewTiles.map((input, index) => ({ input, left: 16 + index * 144, top: 48 })),
  ])
  .png()
  .toFile(previewPath);

console.log(`Generated ${frames.length} acid smoke frames and preview.`);
