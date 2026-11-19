const test = require('node:test');
const assert = require('node:assert/strict');
const { buildStatus } = require('../src/statusModel');

const advice = (item) => ({ item, reasoning: `why ${item}`, priority: 'CRITICAL', confidence: 'EXACT' });
const payload = (status, extra = {}) => ({ visible: false, advice: [], iconVersion: '16.22.1', status, ...extra });

test('bundles the wording, the stage line and the advice rows into one object for the window', () => {
  const status = buildStatus(payload('IN_GAME', { visible: true, advice: [advice('A')] }), { reachable: true });
  assert.equal(status.view.state, 'in-game');
  assert.deepEqual(status.stageLine.stages.filter((s) => s.current).map((s) => s.id), ['in-game']);
  assert.deepEqual(status.rows.map((r) => r.item), ['A']);
});

test('an unreachable backend gives a problem view, a problem stage line and no rows', () => {
  const status = buildStatus(null, { reachable: false, secondsSinceStart: 120 });
  assert.equal(status.view.state, 'unreachable');
  assert.equal(status.stageLine.problem, true);
  assert.deepEqual(status.rows, []);
});

test('a backend that is still starting lights the first stage', () => {
  const status = buildStatus(null, { reachable: false, secondsSinceStart: 2 });
  assert.equal(status.view.state, 'starting');
  assert.deepEqual(status.stageLine.stages.filter((s) => s.current).map((s) => s.id), ['starting']);
});

test('a problem state never carries stale advice rows from the payload', () => {
  const status = buildStatus(payload('ERROR', { advice: [advice('A')] }), { reachable: true });
  assert.equal(status.view.state, 'error');
  assert.deepEqual(status.rows, []);
});

test('the result can be sent over IPC: plain data only', () => {
  const status = buildStatus(payload('NO_GAME'), { reachable: true });
  assert.deepEqual(JSON.parse(JSON.stringify(status)), status);
});

test('garbage never throws', () => {
  for (const bad of [undefined, 7, 'x', [], {}]) {
    assert.doesNotThrow(() => buildStatus(bad, { reachable: true }));
  }
});
