const test = require('node:test');
const assert = require('node:assert/strict');
const { startPolling } = require('../src/poller');

const advice = (item) => ({ visible: true, advice: [{ item, reasoning: `why ${item}`, priority: 'CRITICAL' }] });
const flush = () => new Promise((resolve) => setImmediate(resolve));

/** Runs the loop on a fake clock; returns the views it delivered and a way to advance time. */
function harness(responses, options = {}) {
  const views = [];
  const errors = [];
  let call = 0;
  const fetch = async () => {
    const response = responses[Math.min(call, responses.length - 1)];
    call += 1;
    if (response instanceof Error) throw response;
    return response;
  };
  const stop = startPolling({ fetch, onView: (view) => views.push(view), intervalMs: 1000, onError: (e) => errors.push(e), ...options });
  return { views, errors, stop, calls: () => call };
}

test('delivers the first view immediately', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const { views, stop } = harness([advice('A')]);
  await flush();
  assert.equal(views.length, 1);
  assert.equal(views[0].rows[0].item, 'A');
  stop();
});

test('does not deliver an unchanged view again', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const { views, stop } = harness([advice('A'), advice('A'), advice('A')]);
  await flush();
  t.mock.timers.tick(1000); await flush();
  t.mock.timers.tick(1000); await flush();
  assert.equal(views.length, 1);
  stop();
});

test('delivers a view again when it changes', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const { views, stop } = harness([advice('A'), advice('B')]);
  await flush();
  t.mock.timers.tick(1000); await flush();
  assert.deepEqual(views.map((v) => v.rows[0].item), ['A', 'B']);
  stop();
});

test('a backend that goes away hides the view once, not on every tick', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const { views, stop } = harness([advice('A'), null, null]);
  await flush();
  t.mock.timers.tick(1000); await flush();
  t.mock.timers.tick(1000); await flush();
  assert.deepEqual(views.map((v) => v.visible), [true, false]);
  stop();
});

test('schedules the next poll only after the previous one has finished, so polls never overlap', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  let release;
  let started = 0;
  const fetch = () => { started += 1; return new Promise((resolve) => { release = () => resolve(advice('A')); }); };
  const stop = startPolling({ fetch, onView: () => {}, intervalMs: 1000, onError: () => {} });
  await flush();
  t.mock.timers.tick(5000); await flush();
  assert.equal(started, 1, 'a slow poll must not be joined by a second one');
  release(); await flush();
  t.mock.timers.tick(1000); await flush();
  assert.equal(started, 2);
  stop();
});

test('an error in the delivery callback is reported and the loop carries on', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const errors = [];
  let delivered = 0;
  const stop = startPolling({
    fetch: async () => advice(`item${delivered}`),
    onView: () => { delivered += 1; if (delivered === 1) throw new Error('window gone'); },
    intervalMs: 1000,
    onError: (e) => errors.push(e),
  });
  await flush();
  t.mock.timers.tick(1000); await flush();
  assert.equal(errors.length, 1);
  assert.match(String(errors[0]), /window gone/);
  assert.equal(delivered, 2, 'the loop kept polling after the failure');
  stop();
});

test('stop() ends the loop', async (t) => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const { calls, stop } = harness([advice('A')]);
  await flush();
  stop();
  t.mock.timers.tick(10000); await flush();
  assert.equal(calls(), 1);
});
