'use strict';

const MAX_ROWS = 3;
const MAX_ALTERNATIVES = 3;
const DISCONNECTED = Object.freeze({ visible: false, rows: [] });
const LEVELS = { CRITICAL: 'critical', RECOMMENDED: 'recommended' };

function toRow(a) {
  return {
    item: a.item,
    reasoning: typeof a.reasoning === 'string' ? a.reasoning : '',
    level: LEVELS[a.priority] || 'situational',
    estimated: a.confidence === 'ESTIMATED',
    target: typeof a.target === 'string' ? a.target : null,
    alternatives: [],
  };
}

/** Rows with the same reason and priority become one row: the first item leads, the rest are alternatives. */
function groupRows(rows) {
  const groups = new Map();
  for (const row of rows) {
    const key = `${row.level}|${row.reasoning}`;
    const group = groups.get(key);
    if (!group) {
      groups.set(key, { ...row });
      continue;
    }
    if (group.alternatives.length < MAX_ALTERNATIVES) group.alternatives = [...group.alternatives, row.item];
    group.estimated = group.estimated || row.estimated;
    group.target = group.target || row.target;
  }
  return [...groups.values()];
}

/** Turns the backend's /advice payload into what the window draws. Never throws: bad input hides the overlay. */
function toViewModel(payload) {
  if (!payload || payload.visible !== true || !Array.isArray(payload.advice)) {
    return DISCONNECTED;
  }
  const rows = groupRows(
    payload.advice.filter((a) => a && typeof a.item === 'string' && a.item.length > 0).map(toRow),
  ).slice(0, MAX_ROWS);
  return rows.length === 0 ? DISCONNECTED : { visible: true, rows };
}

module.exports = { toViewModel, MAX_ROWS, MAX_ALTERNATIVES, DISCONNECTED };
