const test = require('node:test');
const assert = require('node:assert/strict');
const Module = require('node:module');

/** Loads preload.js with a fake electron and returns the API it exposes to the page. */
function loadExposedApi() {
  const calls = { on: [] };
  let exposed;
  const fakeElectron = {
    contextBridge: { exposeInMainWorld: (name, api) => { exposed = { name, api }; } },
    ipcRenderer: { on: (channel, handler) => { calls.on.push({ channel, handler }); return fakeElectron.ipcRenderer; } },
  };
  const originalLoad = Module._load;
  Module._load = function (request, ...rest) { return request === 'electron' ? fakeElectron : originalLoad.call(this, request, ...rest); };
  try {
    delete require.cache[require.resolve('../src/preload')];
    require('../src/preload');
  } finally { Module._load = originalLoad; }
  return { exposed, calls };
}

test('exposes only onViewModel under window.macromind', () => {
  const { exposed } = loadExposedApi();
  assert.equal(exposed.name, 'macromind');
  assert.deepEqual(Object.keys(exposed.api), ['onViewModel']);
});

test('onViewModel returns nothing, so the page never receives the ipcRenderer handle', () => {
  const { exposed } = loadExposedApi();
  assert.equal(exposed.api.onViewModel(() => {}), undefined);
});

test('forwards view models from the view-model channel to the callback without the ipc event', () => {
  const { exposed, calls } = loadExposedApi();
  const received = [];
  exposed.api.onViewModel((vm) => received.push(vm));
  assert.equal(calls.on[0].channel, 'view-model');
  calls.on[0].handler({ sender: 'secret' }, { visible: true });
  assert.deepEqual(received, [{ visible: true }]);
});
