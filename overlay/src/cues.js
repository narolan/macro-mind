'use strict';

/** Keys of what deserves a sound: critical rows, and every alert (BUY_NOW, SPIKE and DAMAGE_MIX are all alerts). */
function cueKeys(view) {
  if (!view || !view.visible) return [];
  const rows = view.rows.filter((row) => row.level === 'critical').map((row) => `row:${row.key}`);
  return [...rows, ...view.alerts.map((alert) => `alert:${alert.key}`)];
}

/**
 * Whether to ping when the view goes from previous to next: only for a cue key that was not in previous.
 * A null previous (the first view this window sees) is a baseline and never pings.
 */
function shouldPing(previous, next, soundEnabled) {
  if (!soundEnabled || previous === null || previous === undefined) return false;
  const before = new Set(cueKeys(previous));
  return cueKeys(next).some((key) => !before.has(key));
}

// The sandboxed renderer loads this as a plain script (no require), so it is a global there and a module in the tests.
if (typeof module !== 'undefined') module.exports = { shouldPing, cueKeys };
