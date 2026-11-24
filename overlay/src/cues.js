'use strict';

const FORGET_AFTER_HIDDEN_MS = 5 * 60 * 1000; // a gap this long means a new game: its cues are new again

/** Keys of what deserves a sound: critical rows (by item, so rewording never re-pings) and every alert. */
function cueKeys(view) {
  if (!view || !view.visible) return [];
  const rows = (view.rows || []).filter((row) => row.level === 'critical').map((row) => `row:${row.item}`);
  return [...rows, ...(view.alerts || []).map((alert) => `alert:${alert.key}`)];
}

/**
 * Remembers which cues were already shown. shouldPing(view, soundEnabled) is true only for a cue not shown before.
 * The first view is a baseline and never pings. Hidden views (a failed poll) keep the memory; silent views
 * (the Preview button and its restore) neither ping nor touch it.
 */
function createCueTracker(now = Date.now) {
  let shown = new Set();
  let started = false;
  let hiddenSince = null;
  return {
    shouldPing(view, soundEnabled) {
      if (view && view.silent) return false;
      if (!view || !view.visible) {
        if (hiddenSince === null) hiddenSince = now();
        started = true;
        return false;
      }
      if (hiddenSince !== null && now() - hiddenSince >= FORGET_AFTER_HIDDEN_MS) shown = new Set();
      hiddenSince = null;
      const keys = cueKeys(view);
      const isNew = keys.some((key) => !shown.has(key));
      shown = new Set(keys);
      const ping = soundEnabled && started && isNew;
      started = true;
      return ping;
    },
  };
}

// The sandboxed renderer loads this as a plain script (no require), so it is a global there and a module in the tests.
if (typeof module !== 'undefined') module.exports = { createCueTracker, cueKeys };
