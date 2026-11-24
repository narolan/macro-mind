const test = require('node:test');
const assert = require('node:assert/strict');
const Module = require('node:module');

/** Loads a preload script with a fake electron and returns what it exposes to the page. */
function load(file) {
  const calls = { on: [], invoke: [] };
  const exposed = {};
  const ipcRenderer = {
    on: (channel, handler) => { calls.on.push({ channel, handler }); return ipcRenderer; },
    invoke: (channel, ...args) => { calls.invoke.push({ channel, args }); return Promise.resolve('reply'); },
  };
  const fakeElectron = { contextBridge: { exposeInMainWorld: (name, api) => { exposed[name] = api; } }, ipcRenderer };
  const originalLoad = Module._load;
  Module._load = function (request, ...rest) { return request === 'electron' ? fakeElectron : originalLoad.call(this, request, ...rest); };
  try {
    delete require.cache[require.resolve(file)];
    require(file);
  } finally { Module._load = originalLoad; }
  return { exposed, calls };
}

test('overlay preload: exposes only onViewModel under window.macromind', () => {
  const { exposed } = load('../src/preload');
  assert.deepEqual(Object.keys(exposed), ['macromind']);
  assert.deepEqual(Object.keys(exposed.macromind), ['onViewModel', 'onSoundEnabled']);
});

test('overlay preload: onViewModel returns nothing, so the page never receives the ipcRenderer handle', () => {
  const { exposed } = load('../src/preload');
  assert.equal(exposed.macromind.onViewModel(() => {}), undefined);
});

test('overlay preload: forwards view models from the view-model channel without the ipc event', () => {
  const { exposed, calls } = load('../src/preload');
  const received = [];
  exposed.macromind.onViewModel((vm) => received.push(vm));
  assert.equal(calls.on[0].channel, 'view-model');
  calls.on[0].handler({ sender: 'secret' }, { visible: true });
  assert.deepEqual(received, [{ visible: true }]);
});

test('app preload: exposes exactly the calls the window needs under window.macromindApp', () => {
  const { exposed } = load('../src/appPreload');
  assert.deepEqual(Object.keys(exposed), ['macromindApp']);
  assert.deepEqual(Object.keys(exposed.macromindApp).sort(), ['getSettings', 'getStatus', 'getVersion', 'onStatus', 'previewOverlay', 'saveSettings']);
});

test('app preload: getSettings and getStatus ask the main process and return its answer', async () => {
  const { exposed, calls } = load('../src/appPreload');
  assert.equal(await exposed.macromindApp.getSettings(), 'reply');
  assert.equal(await exposed.macromindApp.getStatus(), 'reply');
  assert.deepEqual(calls.invoke.map((c) => c.channel), ['app:get-settings', 'app:get-status']);
});

test('app preload: saveSettings sends the settings and nothing else', async () => {
  const { exposed, calls } = load('../src/appPreload');
  await exposed.macromindApp.saveSettings({ corner: 'top-left' });
  assert.deepEqual(calls.invoke[0], { channel: 'app:save-settings', args: [{ corner: 'top-left' }] });
});

test('app preload: onStatus returns nothing and forwards only the payload', () => {
  const { exposed, calls } = load('../src/appPreload');
  const received = [];
  assert.equal(exposed.macromindApp.onStatus((status) => received.push(status)), undefined);
  assert.equal(calls.on[0].channel, 'app:status');
  calls.on[0].handler({ sender: 'secret' }, { state: 'waiting' });
  assert.deepEqual(received, [{ state: 'waiting' }]);
});
