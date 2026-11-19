const test = require('node:test');
const assert = require('node:assert/strict');
const { stageLine, adviceRows } = require('../src/appView');

const lit = (line) => line.stages.filter((s) => s.current).map((s) => s.id);
const done = (line) => line.stages.filter((s) => s.done).map((s) => s.id);

test('starting lights the first stage and nothing is done yet', () => {
  const line = stageLine('starting');
  assert.deepEqual(lit(line), ['starting']);
  assert.deepEqual(done(line), []);
});

test('waiting lights the second stage and marks starting as done', () => {
  const line = stageLine('waiting');
  assert.deepEqual(lit(line), ['waiting']);
  assert.deepEqual(done(line), ['starting']);
});

test('in game lights the last stage and marks both earlier ones done', () => {
  const line = stageLine('in-game');
  assert.deepEqual(lit(line), ['in-game']);
  assert.deepEqual(done(line), ['starting', 'waiting']);
});

test('the three stages are always listed in the same order', () => {
  for (const state of ['starting', 'waiting', 'in-game', 'no-data', 'error', 'unreachable']) {
    assert.deepEqual(stageLine(state).stages.map((s) => s.id), ['starting', 'waiting', 'in-game'], state);
  }
});

test('a problem state lights no stage and flags the line as a problem, so it cannot look healthy', () => {
  for (const state of ['no-data', 'error', 'unreachable', 'something-new']) {
    const line = stageLine(state);
    assert.deepEqual(lit(line), [], state);
    assert.equal(line.problem, true, state);
  }
});

test('a healthy state is not flagged as a problem', () => {
  for (const state of ['starting', 'waiting', 'in-game']) assert.equal(stageLine(state).problem, false, state);
});

const advice = (item, extra = {}) => ({ item, reasoning: `why ${item}`, priority: 'CRITICAL', confidence: 'EXACT', ...extra });

test('advice rows are the same grouped rows the overlay shows, with plain level words', () => {
  const rows = adviceRows({ visible: true, iconVersion: '16.22.1', advice: [advice('A', { itemId: 1 })] });
  assert.equal(rows.length, 1);
  assert.equal(rows[0].item, 'A');
  assert.equal(rows[0].level, 'critical');
  assert.match(rows[0].icon, /16\.22\.1\/img\/item\/1\.png$/);
});

test('the window lists advice even when the overlay is hidden, because "visible" is about the overlay, not the data', () => {
  const rows = adviceRows({ visible: false, iconVersion: '', advice: [advice('A'), advice('B', { reasoning: 'other' })] });
  assert.deepEqual(rows.map((r) => r.item), ['A', 'B']);
});

test('merges rows that share a reason and keeps the alternatives', () => {
  const rows = adviceRows({ visible: true, advice: [advice('A', { reasoning: 'same' }), advice('B', { reasoning: 'same' })] });
  assert.equal(rows.length, 1);
  assert.deepEqual(rows[0].alternatives, ['B']);
});

test('shows more rows than the overlay does, since a window has room', () => {
  const many = Array.from({ length: 9 }, (_, i) => advice(`Item${i}`, { reasoning: `reason ${i}` }));
  assert.equal(adviceRows({ visible: true, advice: many }).length, 6);
});

test('bad input gives no rows and never throws', () => {
  for (const bad of [null, undefined, 3, 'x', {}, { advice: 'no' }, { advice: [null, 5, {}] }]) {
    assert.deepEqual(adviceRows(bad), [], JSON.stringify(bad));
  }
});
