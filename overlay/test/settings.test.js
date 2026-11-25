const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { loadSettings, saveSettings, normalize, DEFAULTS, CORNERS } = require('../src/settings');

const tempFile = () => path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'mm-settings-')), 'settings.json');

test('a missing file gives the defaults', () => {
  assert.deepEqual(loadSettings(tempFile()), DEFAULTS);
});

test('a corrupt file gives the defaults instead of crashing the app', () => {
  const file = tempFile();
  fs.writeFileSync(file, '{ not json');
  assert.deepEqual(loadSettings(file), DEFAULTS);
});

test('settings survive a save and a load', () => {
  const file = tempFile();
  const chosen = { overlayEnabled: false, corner: 'bottom-left', opacity: 0.6, startWithWindows: true, soundEnabled: false, alertsEnabled: false, timersEnabled: false };
  saveSettings(file, chosen);
  assert.deepEqual(loadSettings(file), chosen);
});

test('saving creates the folder and leaves no temp files behind', () => {
  const file = path.join(fs.mkdtempSync(path.join(os.tmpdir(), 'mm-settings-')), 'nested', 'settings.json');
  saveSettings(file, DEFAULTS);
  assert.deepEqual(fs.readdirSync(path.dirname(file)), ['settings.json']);
});

test('unknown keys are dropped and missing keys fall back to defaults', () => {
  assert.deepEqual(normalize({ corner: 'top-left', evil: '<script>' }), { ...DEFAULTS, corner: 'top-left' });
});

test('a corner that is not one of the known four falls back to the default', () => {
  for (const bad of ['middle', '', null, 7, undefined, '../x']) {
    assert.equal(normalize({ corner: bad }).corner, DEFAULTS.corner, String(bad));
  }
  assert.deepEqual([...CORNERS].sort(), ['bottom-left', 'bottom-right', 'top-left', 'top-right']);
});

test('opacity is clamped so the overlay can never be made invisible or out of range', () => {
  assert.equal(normalize({ opacity: 0 }).opacity, 0.2);
  assert.equal(normalize({ opacity: -5 }).opacity, 0.2);
  assert.equal(normalize({ opacity: 9 }).opacity, 1);
  assert.equal(normalize({ opacity: 0.5 }).opacity, 0.5);
  for (const bad of ['0.5', NaN, null, Infinity, undefined]) {
    assert.equal(normalize({ opacity: bad }).opacity, DEFAULTS.opacity, String(bad));
  }
});

test('booleans must really be booleans', () => {
  assert.equal(normalize({ overlayEnabled: 'false' }).overlayEnabled, DEFAULTS.overlayEnabled);
  assert.equal(normalize({ startWithWindows: 1 }).startWithWindows, DEFAULTS.startWithWindows);
  assert.equal(normalize({ soundEnabled: 'no' }).soundEnabled, true);
  assert.equal(normalize({ soundEnabled: false }).soundEnabled, false);
  assert.equal(normalize({ alertsEnabled: 'no' }).alertsEnabled, true);
  assert.equal(normalize({ timersEnabled: 0 }).timersEnabled, true);
  assert.equal(normalize({ alertsEnabled: false, timersEnabled: false }).timersEnabled, false);
  assert.equal(normalize({ overlayEnabled: false }).overlayEnabled, false);
});

test('normalize never throws on garbage input', () => {
  for (const bad of [null, undefined, 42, 'x', [], () => {}]) {
    assert.deepEqual(normalize(bad), DEFAULTS);
  }
});
