const test = require('node:test');
const assert = require('node:assert/strict');
const { applyFeatureToggles, HIDDEN_VIEW } = require('../src/viewModel');

const row = { key: 'r' };
const alert = { message: 'm', level: 'critical', key: 'a' };
const timer = { name: 'Dragon', secondsLeft: 30 };
const view = { visible: true, rows: [row], alerts: [alert], timers: [timer] };

test('everything stays when both features are on', () => {
  assert.equal(applyFeatureToggles(view, { alertsEnabled: true, timersEnabled: true }), view);
});

test('alerts are dropped when switched off', () => {
  assert.deepEqual(applyFeatureToggles(view, { alertsEnabled: false, timersEnabled: true }), { ...view, alerts: [] });
});

test('timers are dropped when switched off', () => {
  assert.deepEqual(applyFeatureToggles(view, { alertsEnabled: true, timersEnabled: false }), { ...view, timers: [] });
});

test('the overlay hides when switching off leaves nothing to draw', () => {
  const only = { visible: true, rows: [], alerts: [alert], timers: [timer] };
  assert.equal(applyFeatureToggles(only, { alertsEnabled: false, timersEnabled: false }), HIDDEN_VIEW);
});

test('a hidden view stays hidden', () => {
  assert.equal(applyFeatureToggles(HIDDEN_VIEW, { alertsEnabled: false, timersEnabled: false }), HIDDEN_VIEW);
});
