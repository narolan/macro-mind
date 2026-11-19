const test = require('node:test');
const assert = require('node:assert/strict');
const { describeStatus } = require('../src/status');

const payload = (status, extra = {}) => ({ visible: false, advice: [], iconVersion: '16.22.1', status, ...extra });

test('a backend that does not answer is reported as starting, then as unreachable once it has had time', () => {
  assert.equal(describeStatus(null, { reachable: false, secondsSinceStart: 3 }).state, 'starting');
  assert.equal(describeStatus(null, { reachable: false, secondsSinceStart: 60 }).state, 'unreachable');
});

test('NO_GAME is a healthy waiting state, with wording that says what to do', () => {
  const view = describeStatus(payload('NO_GAME'), { reachable: true });
  assert.equal(view.state, 'waiting');
  assert.equal(view.tone, 'ok');
  assert.match(view.detail, /start a (League )?(match|game)/i);
});

test('IN_GAME is healthy and says advice is live', () => {
  const view = describeStatus(payload('IN_GAME', { visible: true, advice: [{ item: 'A' }] }), { reachable: true });
  assert.equal(view.state, 'in-game');
  assert.equal(view.tone, 'ok');
});

test('NO_DATA is a problem and says why nothing can be advised', () => {
  const view = describeStatus(payload('NO_DATA'), { reachable: true });
  assert.equal(view.state, 'no-data');
  assert.equal(view.tone, 'bad');
  assert.match(view.detail, /internet|item data/i);
});

test('ERROR is a problem and points at the log', () => {
  const view = describeStatus(payload('ERROR'), { reachable: true });
  assert.equal(view.state, 'error');
  assert.equal(view.tone, 'bad');
  assert.match(view.detail, /log/i);
});

test('an unknown or missing status never throws and is shown as an error, not as healthy', () => {
  for (const bad of [payload('WHATEVER'), payload(undefined), {}, 42, 'x']) {
    const view = describeStatus(bad, { reachable: true });
    assert.equal(view.tone, 'bad', JSON.stringify(bad));
  }
});

test('every view has a title and a detail, so the window is never blank', () => {
  const inputs = [[null, { reachable: false, secondsSinceStart: 1 }], [null, { reachable: false, secondsSinceStart: 99 }],
    ...['NO_GAME', 'IN_GAME', 'NO_DATA', 'ERROR'].map((s) => [payload(s), { reachable: true }])];
  for (const [p, ctx] of inputs) {
    const view = describeStatus(p, ctx);
    assert.ok(view.title.length > 0 && view.detail.length > 0);
  }
});

test('the item data version is surfaced when known and omitted otherwise', () => {
  assert.equal(describeStatus(payload('NO_GAME'), { reachable: true }).dataVersion, '16.22.1');
  assert.equal(describeStatus(payload('NO_GAME', { iconVersion: '' }), { reachable: true }).dataVersion, null);
});
