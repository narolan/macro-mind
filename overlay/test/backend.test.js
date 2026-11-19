const test = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { createBackend, backendCommand } = require('../src/backend');

const FAKE = path.join(__dirname, 'fixtures', 'fake-backend.js');
const node = process.execPath;
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function until(condition, timeoutMs = 4000) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    if (await condition()) return true;
    await sleep(25);
  }
  return false;
}

const make = (mode, extra = {}) => createBackend({ command: node, args: [FAKE, mode], restartDelayMs: 50, maxRestarts: 2, ...extra });

test('start() resolves once the process is running', async () => {
  const backend = make('ok');
  await backend.start();
  assert.equal(backend.isRunning(), true);
  await backend.stop();
});

test('stop() ends the process and is safe to call twice', async () => {
  const backend = make('ok');
  await backend.start();
  await backend.stop();
  assert.equal(backend.isRunning(), false);
  await assert.doesNotReject(backend.stop());
});

test('a backend that dies is restarted', async () => {
  const starts = [];
  const backend = make('die-once', { onStart: (n) => starts.push(n) });
  await backend.start();
  assert.ok(await until(() => starts.length >= 2), 'expected a second start');
  await backend.stop();
});

test('restarts stop after maxRestarts so a broken install does not spin forever', async () => {
  const starts = [];
  const exits = [];
  const backend = make('die-always', { onStart: (n) => starts.push(n), onGiveUp: () => exits.push('gave-up') });
  await backend.start();
  assert.ok(await until(() => exits.length === 1));
  assert.equal(starts.length, 3, 'the first start plus maxRestarts=2 restarts');
  assert.equal(backend.isRunning(), false);
});

test('stop() prevents any further restart', async () => {
  const starts = [];
  const backend = make('die-once', { onStart: (n) => starts.push(n), restartDelayMs: 300 });
  await backend.start();
  await backend.stop();
  await sleep(500);
  assert.equal(starts.length, 1);
});

test('a command that does not exist is reported, not thrown', async () => {
  const errors = [];
  const backend = createBackend({ command: '/definitely/not/here', args: [], onError: (e) => errors.push(e), maxRestarts: 0 });
  await assert.doesNotReject(backend.start());
  assert.ok(await until(() => errors.length > 0));
  assert.equal(backend.isRunning(), false);
});

test('output of the backend is passed to onLog line by line', async () => {
  const lines = [];
  const backend = make('chatty', { onLog: (line) => lines.push(line) });
  await backend.start();
  assert.ok(await until(() => lines.includes('hello from backend')));
  await backend.stop();
});

test('backendCommand points at the bundled runtime and the app jars next to the app', () => {
  const cmd = backendCommand({ resourcesPath: '/app/resources', platform: 'win32', port: 8765 });
  assert.equal(cmd.command, path.join('/app/resources', 'backend', 'runtime', 'bin', 'java.exe'));
  assert.ok(cmd.args.includes('-cp'));
  assert.ok(cmd.args.some((a) => a.includes(path.join('backend', 'lib'))));
  assert.ok(cmd.args.includes('com.macromind.Main'));
  assert.ok(cmd.args.includes('-Dmacromind.port=8765'));
});

test('backendCommand uses a plain java binary on macOS and Linux', () => {
  const cmd = backendCommand({ resourcesPath: '/app/resources', platform: 'darwin', port: 9000 });
  assert.equal(path.basename(cmd.command), 'java');
  assert.ok(cmd.args.includes('-Dmacromind.port=9000'));
});
