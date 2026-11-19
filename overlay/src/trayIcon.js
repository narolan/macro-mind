'use strict';

// Same colours as the app window's lamp, so the tray and the window always agree.
const TONES = Object.freeze({
  ok: [0x5f, 0xbf, 0x9c],
  warn: [0xe0, 0xb0, 0x4b],
  bad: [0xe8, 0x73, 0x5f],
});
const NEUTRAL = [0x8a, 0x8f, 0x92];

/**
 * A round, anti-aliased status dot as a raw BGRA bitmap (what Electron's nativeImage.createFromBitmap takes).
 * Drawn in code so no image files have to be shipped or kept in sync with the colours.
 */
function trayIconBitmap(tone, size = 16) {
  const [r, g, b] = TONES[tone] || NEUTRAL;
  const buffer = Buffer.alloc(size * size * 4);
  const centre = (size - 1) / 2;
  const radius = size / 2 - 1;
  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const distance = Math.hypot(x - centre, y - centre);
      const coverage = Math.min(1, Math.max(0, radius - distance + 0.5)); // 1 inside, fading to 0 across one pixel
      const i = (y * size + x) * 4;
      buffer[i] = b;
      buffer[i + 1] = g;
      buffer[i + 2] = r;
      buffer[i + 3] = Math.round(coverage * 255);
    }
  }
  return { buffer, width: size, height: size };
}

module.exports = { trayIconBitmap, TONES };
