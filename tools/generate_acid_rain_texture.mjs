import fs from "node:fs";
import os from "node:os";
import path from "node:path";
import { execFileSync } from "node:child_process";
import sharp from "sharp";
import { fileURLToPath } from "node:url";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const minecraftJar = path.join(
  os.homedir(),
  ".gradle", "caches", "fabric-loom", "1.21.1", "minecraft-client.jar",
);
const sourceDir = path.join(root, "build", "vanilla-texture-source");
const sourceRelative = "assets/minecraft/textures/environment/rain.png";
const source = path.join(sourceDir, sourceRelative);
const output = path.join(
  root,
  "src", "main", "resources", "assets", "minecraft",
  "textures", "environment", "rain.png",
);
const preview = path.join(root, "build", "reports", "acid_rain_texture.png");

fs.mkdirSync(sourceDir, { recursive: true });
fs.mkdirSync(path.dirname(output), { recursive: true });
fs.mkdirSync(path.dirname(preview), { recursive: true });

if (!fs.existsSync(source)) {
  execFileSync("jar", ["xf", minecraftJar, sourceRelative], {
    cwd: sourceDir,
    stdio: "inherit",
  });
}

const { data, info } = await sharp(source)
  .ensureAlpha()
  .raw()
  .toBuffer({ resolveWithObject: true });

const pixels = Buffer.from(data);
for (let y = 0; y < info.height; y++) {
  for (let x = 0; x < info.width; x++) {
    const offset = (y * info.width + x) * 4;
    const alpha = pixels[offset + 3];
    if (alpha === 0) continue;

    const brightness =
      pixels[offset] * 0.2126 +
      pixels[offset + 1] * 0.7152 +
      pixels[offset + 2] * 0.0722;
    const pulse = ((x * 7 + y * 3) % 19) / 18;

    pixels[offset] = Math.round(55 + brightness * 0.16 + pulse * 12);
    pixels[offset + 1] = Math.round(126 + brightness * 0.42 + pulse * 24);
    pixels[offset + 2] = Math.round(22 + brightness * 0.08);
    pixels[offset + 3] = Math.min(220, Math.round(alpha * 0.88));
  }
}

await sharp(pixels, {
  raw: {
    width: info.width,
    height: info.height,
    channels: 4,
  },
})
  .png({ palette: true, colours: 64, dither: 0 })
  .toFile(output);

const enlarged = await sharp(output)
  .resize(info.width * 4, info.height * 4, { kernel: "nearest" })
  .png()
  .toBuffer();

await sharp({
  create: {
    width: info.width * 4 + 32,
    height: info.height * 4 + 64,
    channels: 4,
    background: "#121720",
  },
})
  .composite([
    {
      input: Buffer.from(`<svg width="${info.width * 4 + 32}" height="64" xmlns="http://www.w3.org/2000/svg">
        <text x="16" y="26" fill="#f4f7fa" font-family="Segoe UI" font-size="18" font-weight="700">Acid rain</text>
        <text x="16" y="47" fill="#8f9bad" font-family="Segoe UI" font-size="11">Vanilla rain atlas, acid palette, 4× preview</text>
      </svg>`),
      left: 0,
      top: 0,
    },
    { input: enlarged, left: 16, top: 64 },
  ])
  .png()
  .toFile(preview);

console.log(`Generated acid rain texture: ${output}`);
