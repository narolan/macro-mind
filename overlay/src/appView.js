'use strict';

const { toRows } = require('./viewModel');

const WINDOW_MAX_ROWS = 6; // the overlay shows 3; the window has room for more
const STAGES = Object.freeze(['starting', 'waiting', 'in-game']);
const HEALTHY = new Set(STAGES);

/**
 * The stage line for a status state. Healthy states light one stage and mark the earlier ones done; a problem lights
 * none and sets `problem`, so a broken app can never look like a healthy one that is merely early in the line.
 */
function stageLine(state) {
  const index = STAGES.indexOf(state);
  return {
    problem: !HEALTHY.has(state),
    stages: STAGES.map((id, position) => ({ id, current: position === index, done: index > position })),
  };
}

/** The advice rows the app window lists. */
function adviceRows(payload) {
  return toRows(payload, WINDOW_MAX_ROWS);
}

module.exports = { stageLine, adviceRows, WINDOW_MAX_ROWS };
