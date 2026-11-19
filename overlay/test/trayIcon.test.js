const test = require('node:test');
const assert = require('node:assert/strict');
const { trayIconBitmap } = require('../src/trayIcon');

// The bitmap is BGRA, the byte order Electron's nativeImage.createFromBitmap expects.
const pixel = (icon, x, y) => {
  const i = (y * icon.width + x) * 4;
  return { b: icon.buffer[i], g: icon.buffer[i + 1], r: icon.buffer[i + 2], a: icon.buffer[i + 3] };
};

test('has the requested size and exactly four bytes per pixel', () => {
  const icon = trayIconBitmap('ok', 16);
  assert.equal(icon.width, 16);
  assert.equal(icon.height, 16);
  assert.equal(icon.buffer.length, 16 * 16 * 4);
});

test('the middle is solid in the tone colour and the corners are transparent', () => {
  const ok = trayIconBitmap('ok', 16);
  assert.deepEqual(pixel(ok, 8, 8), { r: 0x5f, g: 0xbf, b: 0x9c, a: 255 });
  assert.equal(pixel(ok, 0, 0).a, 0);
  assert.equal(pixel(ok, 15, 15).a, 0);
});

test('ok, warn and bad are three different colours a user can tell apart', () => {
  const centres = ['ok', 'warn', 'bad'].map((tone) => JSON.stringify(pixel(trayIconBitmap(tone, 16), 8, 8)));
  assert.equal(new Set(centres).size, 3);
});

test('an unknown tone is grey, never one of the status colours', () => {
  const unknown = pixel(trayIconBitmap('mystery', 16), 8, 8);
  for (const tone of ['ok', 'warn', 'bad']) {
    assert.notDeepEqual(unknown, pixel(trayIconBitmap(tone, 16), 8, 8));
  }
  assert.equal(unknown.a, 255);
});

test('the edge is anti-aliased: some pixels are partly transparent', () => {
  const icon = trayIconBitmap('ok', 32);
  const partial = [];
  for (let y = 0; y < 32; y += 1) for (let x = 0; x < 32; x += 1) {
    const a = pixel(icon, x, y).a;
    if (a > 0 && a < 255) partial.push(a);
  }
  assert.ok(partial.length > 0);
});
