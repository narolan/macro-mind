'use strict';

const MAX_ROWS = 3;
const DISCONNECTED = Object.freeze({ visible: false, rows: [] });
const LEVELS = { CRITICAL: 'critical', RECOMMENDED: 'recommended' };

/** Turns the backend's /advice payload into what the window draws. Never throws: bad input hides the overlay. */
function toViewModel(payload) {
  if (!payload || payload.visible !== true || !Array.isArray(payload.advice)) {
    return DISCONNECTED;
  }
  const rows = payload.advice
    .filter((a) => a && typeof a.item === 'string' && a.item.length > 0)
    .slice(0, MAX_ROWS)
    .map((a) => ({
      item: a.item,
      reasoning: typeof a.reasoning === 'string' ? a.reasoning : '',
      level: LEVELS[a.priority] || 'situational',
      estimated: a.confidence === 'ESTIMATED',
      target: typeof a.target === 'string' ? a.target : null,
    }));
  return rows.length === 0 ? DISCONNECTED : { visible: true, rows };
}

module.exports = { toViewModel, MAX_ROWS, DISCONNECTED };
