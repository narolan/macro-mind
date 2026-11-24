const test = require('node:test');
const assert = require('node:assert/strict');
const { shouldPing } = require('../src/cues');
const { HIDDEN_VIEW } = require('../src/viewModel');

const row = (key, level = 'critical') => ({ item: key, level, key, reasoning: '', alternatives: [] });
const view = (rows = [], alerts = []) => ({ visible: true, rows, alerts, timers: [] });
const alert = (key) => ({ message: key, level: 'recommended', key });

test('pings when a critical row appears', () => {
  assert.equal(shouldPing(HIDDEN_VIEW, view([row('a')]), true), true);
});

test('pings when an alert appears', () => {
  assert.equal(shouldPing(HIDDEN_VIEW, view([], [alert('x')]), true), true);
});

test('does not ping for recommended or situational rows', () => {
  assert.equal(shouldPing(HIDDEN_VIEW, view([row('a', 'recommended'), row('b', 'situational')]), true), false);
});

test('does not ping again for an unchanged view', () => {
  assert.equal(shouldPing(view([row('a')], [alert('x')]), view([row('a')], [alert('x')]), true), false);
});

test('compares by key, not by count: a swapped critical row pings', () => {
  assert.equal(shouldPing(view([row('a')]), view([row('b')]), true), true);
});

test('does not ping when a cue goes away', () => {
  assert.equal(shouldPing(view([row('a')], [alert('x')]), view([row('a')]), true), false);
});

test('the first view the window sees is a baseline and never pings', () => {
  assert.equal(shouldPing(null, view([row('a')]), true), false);
});

test('never pings with sound off', () => {
  assert.equal(shouldPing(HIDDEN_VIEW, view([row('a')]), false), false);
});

test('a hidden next view never pings', () => {
  assert.equal(shouldPing(view([row('a')]), HIDDEN_VIEW, true), false);
});
