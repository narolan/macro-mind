'use strict';

const test = require('node:test');
const assert = require('node:assert');
const { EventEmitter } = require('node:events');
const { startAutoUpdate } = require('../src/updater');

function fakeUpdater(checkImpl) {
  const u = new EventEmitter();
  u.checks = 0;
  u.checkForUpdates = () => { u.checks++; return checkImpl(); };
  return u;
}

test('checksOnStartAndOnInterval, downloadingAndInstallingOnQuit', () => {
  const updater = fakeUpdater(() => Promise.resolve());
  let tick;
  startAutoUpdate({ updater, log: () => {}, setIntervalFn: (fn) => { tick = fn; } });
  assert.strictEqual(updater.checks, 1);
  tick();
  assert.strictEqual(updater.checks, 2);
  assert.strictEqual(updater.autoDownload, true);
  assert.strictEqual(updater.autoInstallOnAppQuit, true);
});

test('logsCheckFailuresInsteadOfThrowing', async () => {
  const logs = [];
  const updater = fakeUpdater(() => Promise.reject(new Error('offline')));
  startAutoUpdate({ updater, log: (m) => logs.push(m), setIntervalFn: () => null });
  await new Promise((r) => setImmediate(r));
  assert.ok(logs.some((m) => m.includes('update check failed')));
});

test('callsOnDownloadedWhenAnUpdateIsDownloaded', () => {
  const updater = fakeUpdater(() => Promise.resolve());
  let downloaded = 0;
  startAutoUpdate({ updater, log: () => {}, setIntervalFn: () => null, onDownloaded: () => { downloaded++; } });
  updater.emit('update-downloaded', { version: '9.9.9' });
  assert.strictEqual(downloaded, 1);
});
