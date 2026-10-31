const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');

const root = path.resolve(__dirname, '../..');
const runtime = path.join(root, 'app/src/main/assets/artwork');
const masters = path.join(__dirname, 'masters');

async function exportArtwork() {
  fs.mkdirSync(masters, { recursive: true });
  let entries;
  if (process.argv[2]) {
    entries = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
    for (const entry of entries) {
      fs.copyFileSync(entry.sourcePath, path.join(masters, `${entry.name}.png`));
      delete entry.sourcePath;
    }
    fs.writeFileSync(path.join(__dirname, 'prompts.json'), JSON.stringify(entries, null, 2) + '\n');
  } else {
    entries = JSON.parse(fs.readFileSync(path.join(__dirname, 'prompts.json'), 'utf8'));
  }
  const manifest = [];
  const tiles = [];
  for (const [index, entry] of entries.entries()) {
    const source = path.join(masters, `${entry.name}.png`);
    const sourceMetadata = await sharp(source).metadata();
    const { data, info } = await sharp(source).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
    let left = info.width, top = info.height, right = -1, bottom = -1;
    for (let y = 0; y < info.height; y++) {
      for (let x = 0; x < info.width; x++) {
        if (data[(y * info.width + x) * 4 + 3] > 0) {
          left = Math.min(left, x); top = Math.min(top, y);
          right = Math.max(right, x); bottom = Math.max(bottom, y);
        }
      }
    }
    if (right < left) throw new Error(`Empty image: ${entry.name}`);
    const width = 1600;
    const height = entry.name.startsWith('onboarding_') ? 1000 : 640;
    const inner = await sharp(source)
      .extract({ left, top, width: right - left + 1, height: bottom - top + 1 })
      .resize({ width: Math.floor(width * 0.76), height: Math.floor(height * 0.76), fit: 'inside', kernel: 'lanczos3' })
      .png().toBuffer();
    const innerMetadata = await sharp(inner).metadata();
    const padX = Math.floor((width - innerMetadata.width) / 2);
    const padY = Math.floor((height - innerMetadata.height) / 2);
    const canvas = await sharp(inner).extend({
      left: padX, right: width - innerMetadata.width - padX,
      top: padY, bottom: height - innerMetadata.height - padY,
      background: { r: 0, g: 0, b: 0, alpha: 0 }
    }).png().toBuffer();
    let encoded, quality = 94;
    while (true) {
      encoded = await sharp(canvas).withIccProfile('srgb')
        .webp({ quality, alphaQuality: 100, effort: 6, smartSubsample: true }).toBuffer();
      if (encoded.length <= 400000 || quality <= 86) break;
      quality -= 2;
    }
    if (encoded.length > 400000) throw new Error(`File exceeds budget: ${entry.name}`);
    const output = path.join(runtime, `${entry.name}.webp`);
    fs.writeFileSync(output, encoded);
    const metadata = await sharp(output).metadata();
    if (metadata.width !== width || metadata.height !== height || !metadata.hasAlpha || !metadata.hasProfile) {
      throw new Error(`Invalid export: ${entry.name}`);
    }
    manifest.push({
      name: entry.name, file: path.relative(root, output), source: `design/artwork/masters/${entry.name}.png`,
      width, height, aspectRatio: height === 1000 ? '8:5' : '5:2', format: 'webp',
      colorSpace: 'sRGB', alpha: true, bytes: encoded.length, quality,
      sourceDimensions: [sourceMetadata.width, sourceMetadata.height],
      safeMargins: 'At least 12% transparent padding on every side',
      fit: 'ContentScale.Fit; no crop', appearance: 'Shared light/dark',
      generator: 'Built-in image_gen; prompts.json contains generation and correction prompts'
    });
    for (let appearance = 0; appearance < 2; appearance++) {
      const thumbnail = await sharp(encoded).resize({ width: 400, height: 250, fit: 'contain',
        background: appearance ? '#174A94' : '#E0EBFF' }).flatten({ background: appearance ? '#174A94' : '#E0EBFF' }).png().toBuffer();
      tiles.push({ input: thumbnail, left: appearance * 400, top: index * 280 + 30 });
    }
    const label = Buffer.from(`<svg width="800" height="30"><rect width="800" height="30" fill="#F6F7FB"/><text x="12" y="21" font-family="sans-serif" font-size="16" fill="#172033">${String(index + 1).padStart(2, '0')}. ${entry.name} — ${width}×${height}</text></svg>`);
    tiles.push({ input: label, left: 0, top: index * 280 });
    console.log(`${entry.name}: ${width}x${height}, ${Math.round(encoded.length / 1024)} KiB, quality ${quality}`);
  }
  fs.writeFileSync(path.join(__dirname, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n');
  await sharp({ create: { width: 800, height: entries.length * 280, channels: 3, background: '#F6F7FB' } })
    .composite(tiles).png().toFile(path.join(__dirname, 'review-light-dark.png'));
  console.log(`Exported ${manifest.length} images, ${Math.round(manifest.reduce((sum, item) => sum + item.bytes, 0) / 1024)} KiB total.`);
}

exportArtwork().catch(error => { console.error(error); process.exitCode = 1; });
