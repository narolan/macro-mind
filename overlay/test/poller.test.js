const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { fetchAdvice } = require('../src/poller');

const serve = (handler) => new Promise((resolve) => {
  const server = http.createServer(handler).listen(0, '127.0.0.1', () => resolve(server));
});

test('returns parsed JSON from the backend', async () => {
  const server = await serve((req, res) => res.end(JSON.stringify({ visible: true, advice: [] })));
  try {
    assert.deepEqual(await fetchAdvice(`http://127.0.0.1:${server.address().port}/advice`), { visible: true, advice: [] });
  } finally { server.close(); }
});

test('returns null when nothing is listening', async () => {
  assert.equal(await fetchAdvice('http://127.0.0.1:1/advice'), null);
});

test('returns null on a non-200 answer or invalid JSON', async () => {
  const bad = await serve((req, res) => { res.statusCode = 403; res.end('no'); });
  const junk = await serve((req, res) => res.end('not json'));
  try {
    assert.equal(await fetchAdvice(`http://127.0.0.1:${bad.address().port}/advice`), null);
    assert.equal(await fetchAdvice(`http://127.0.0.1:${junk.address().port}/advice`), null);
  } finally { bad.close(); junk.close(); }
});

test('gives up on a stalled backend instead of hanging', async () => {
  const stalled = await serve(() => {});
  try {
    const started = Date.now();
    assert.equal(await fetchAdvice(`http://127.0.0.1:${stalled.address().port}/advice`, 300), null);
    assert.ok(Date.now() - started < 2000);
  } finally { stalled.closeAllConnections(); stalled.close(); }
});
