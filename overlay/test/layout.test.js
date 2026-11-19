const test = require('node:test');
const assert = require('node:assert/strict');
const { overlayBounds } = require('../src/layout');

const AREA = { x: 0, y: 0, width: 1920, height: 1080 };
const SIZE = { width: 380, height: 340 };

test('places the overlay in each corner with the margin kept', () => {
  assert.deepEqual(overlayBounds(AREA, 'top-right', SIZE, 24), { x: 1516, y: 24, ...SIZE });
  assert.deepEqual(overlayBounds(AREA, 'top-left', SIZE, 24), { x: 24, y: 24, ...SIZE });
  assert.deepEqual(overlayBounds(AREA, 'bottom-right', SIZE, 24), { x: 1516, y: 716, ...SIZE });
  assert.deepEqual(overlayBounds(AREA, 'bottom-left', SIZE, 24), { x: 24, y: 716, ...SIZE });
});

test('respects a work area that does not start at the origin (second monitor, taskbar on the left)', () => {
  const area = { x: 1920, y: 40, width: 1280, height: 720 };
  assert.deepEqual(overlayBounds(area, 'top-left', SIZE, 24), { x: 1944, y: 64, ...SIZE });
  assert.deepEqual(overlayBounds(area, 'bottom-right', SIZE, 24), { x: 1920 + 1280 - 380 - 24, y: 40 + 720 - 340 - 24, ...SIZE });
});

test('an unknown corner falls back to the top right', () => {
  assert.deepEqual(overlayBounds(AREA, 'middle', SIZE, 24), overlayBounds(AREA, 'top-right', SIZE, 24));
  assert.deepEqual(overlayBounds(AREA, undefined, SIZE, 24), overlayBounds(AREA, 'top-right', SIZE, 24));
});

test('a screen smaller than the overlay never pushes it off the top-left of the work area', () => {
  const tiny = { x: 10, y: 20, width: 300, height: 200 };
  const bounds = overlayBounds(tiny, 'bottom-right', SIZE, 24);
  assert.ok(bounds.x >= tiny.x && bounds.y >= tiny.y);
});
