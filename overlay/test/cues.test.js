const test = require('node:test');
const assert = require('node:assert/strict');
const { createCueTracker, cueKeys } = require('../src/cues');
const { HIDDEN_VIEW } = require('../src/viewModel');

const row = (item, level = 'critical') => ({ item, level, key: `${item}|reasoning|est`, reasoning: '', alternatives: [] });
const view = (rows = [], alerts = []) => ({ visible: true, rows, alerts, timers: [] });
const alert = (key) => ({ message: key, level: 'recommended', key });
/** A tracker already past its baseline view. */
const started = (now) => { const t = createCueTracker(now); t.shouldPing(HIDDEN_VIEW, true); return t; };

test('pings when a critical row appears', () => {
  assert.equal(started().shouldPing(view([row('a')]), true), true);
});

test('pings when an alert appears', () => {
  assert.equal(started().shouldPing(view([], [alert('x')]), true), true);
});

test('does not ping for recommended or situational rows', () => {
  assert.equal(started().shouldPing(view([row('a', 'recommended'), row('b', 'situational')]), true), false);
});

test('does not ping again for an unchanged view', () => {
  const t = started();
  t.shouldPing(view([row('a')], [alert('x')]), true);
  assert.equal(t.shouldPing(view([row('a')], [alert('x')]), true), false);
});

test('a swapped critical row pings', () => {
  const t = started();
  t.shouldPing(view([row('a')]), true);
  assert.equal(t.shouldPing(view([row('b')]), true), true);
});

test('reworded advice for the same item does not ping again', () => {
  const t = started();
  t.shouldPing(view([row('a')]), true);
  const reworded = { ...row('a'), key: 'a|other reasoning|other estimate' };
  assert.equal(t.shouldPing(view([reworded]), true), false);
});

test('does not ping when a cue goes away', () => {
  const t = started();
  t.shouldPing(view([row('a')], [alert('x')]), true);
  assert.equal(t.shouldPing(view([row('a')]), true), false);
});

test('the first view the window sees is a baseline and never pings', () => {
  assert.equal(createCueTracker().shouldPing(view([row('a')]), true), false);
});

test('never pings with sound off', () => {
  assert.equal(started().shouldPing(view([row('a')]), false), false);
});

test('a hidden view never pings', () => {
  const t = started();
  t.shouldPing(view([row('a')]), true);
  assert.equal(t.shouldPing(HIDDEN_VIEW, true), false);
});

test('a failed poll (hidden view) does not make already shown cues ping again', () => {
  const t = started();
  t.shouldPing(view([row('a')], [alert('x')]), true);
  t.shouldPing(HIDDEN_VIEW, true);
  assert.equal(t.shouldPing(view([row('a')], [alert('x')]), true), false);
});

test('cues are forgotten after a long absence, so a new game pings again', () => {
  let now = 0;
  const t = started(() => now);
  t.shouldPing(view([row('a')]), true);
  now = 1000;
  t.shouldPing(HIDDEN_VIEW, true);
  now = 1000 + 10 * 60 * 1000;
  assert.equal(t.shouldPing(view([row('a')]), true), true);
});

test('silent views (preview and its restore) never ping and leave the remembered cues alone', () => {
  const t = started();
  t.shouldPing(view([row('a')]), true);
  assert.equal(t.shouldPing({ ...view([row('p')]), silent: true }, true), false);
  assert.equal(t.shouldPing({ ...view([row('a')]), silent: true }, true), false);
  assert.equal(t.shouldPing(view([row('a')]), true), false);
});

test('tolerates a view with missing rows or alerts', () => {
  assert.deepEqual(cueKeys({ visible: true }), []);
  assert.equal(started().shouldPing({ visible: true, rows: null }, true), false);
});

test('cue keys use the item name for rows and the key for alerts', () => {
  assert.deepEqual(cueKeys(view([row('a')], [alert('x')])), ['row:a', 'alert:x']);
});
