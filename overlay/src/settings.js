'use strict';

const fs = require('node:fs');
const path = require('node:path');

const CORNERS = Object.freeze(['top-right', 'top-left', 'bottom-right', 'bottom-left']);
const MIN_OPACITY = 0.2; // below this the overlay is effectively invisible and the user cannot tell why
const MAX_OPACITY = 1;

const DEFAULTS = Object.freeze({ overlayEnabled: true, corner: 'top-right', opacity: 1, startWithWindows: false });

const isBoolean = (value, fallback) => (typeof value === 'boolean' ? value : fallback);

/** Turns anything into valid settings: unknown keys are dropped, bad values fall back to the defaults. */
function normalize(raw) {
  const input = raw && typeof raw === 'object' && !Array.isArray(raw) ? raw : {};
  const opacity = Number.isFinite(input.opacity) ? Math.min(MAX_OPACITY, Math.max(MIN_OPACITY, input.opacity)) : DEFAULTS.opacity;
  return {
    overlayEnabled: isBoolean(input.overlayEnabled, DEFAULTS.overlayEnabled),
    corner: CORNERS.includes(input.corner) ? input.corner : DEFAULTS.corner,
    opacity,
    startWithWindows: isBoolean(input.startWithWindows, DEFAULTS.startWithWindows),
  };
}

/** The saved settings, or the defaults when the file is missing or unreadable. Never throws. */
function loadSettings(file) {
  try {
    return normalize(JSON.parse(fs.readFileSync(file, 'utf8')));
  } catch {
    return { ...DEFAULTS };
  }
}

/** Writes through a temp file and a rename, so a crash never leaves a half-written settings file. */
function saveSettings(file, settings) {
  fs.mkdirSync(path.dirname(file), { recursive: true });
  const temp = `${file}.tmp`;
  fs.writeFileSync(temp, JSON.stringify(normalize(settings), null, 2));
  fs.renameSync(temp, file);
}

module.exports = { loadSettings, saveSettings, normalize, DEFAULTS, CORNERS };
