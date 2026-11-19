'use strict';

const { describeStatus } = require('./status');
const { stageLine, adviceRows } = require('./appView');

const HEALTHY_STATES = new Set(['starting', 'waiting', 'in-game']);

/**
 * Everything the app window needs, as plain data safe to send over IPC.
 * A problem state never carries advice rows: stale advice next to "something went wrong" would read as working.
 */
function buildStatus(payload, context) {
  const view = describeStatus(payload, context);
  return {
    view,
    stageLine: stageLine(view.state),
    rows: HEALTHY_STATES.has(view.state) ? adviceRows(payload) : [],
  };
}

module.exports = { buildStatus };
