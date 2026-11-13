'use strict';

const { app, BrowserWindow, screen } = require('electron');
const path = require('node:path');
const { fetchAdvice } = require('./poller');
const { toViewModel, viewKey } = require('./viewModel');

const ADVICE_URL = process.env.MACROMIND_URL || 'http://127.0.0.1:8765/advice';
const POLL_INTERVAL_MS = 1000;
const WIDTH = 380;
const HEIGHT = 340;
const MARGIN = 24;

function createWindow() {
  const { workArea } = screen.getPrimaryDisplay();
  const window = new BrowserWindow({
    width: WIDTH,
    height: HEIGHT,
    x: workArea.x + workArea.width - WIDTH - MARGIN,
    y: workArea.y + MARGIN,
    transparent: true,
    frame: false,
    resizable: false,
    focusable: false, // must never take keyboard focus from the game
    skipTaskbar: true,
    hasShadow: false,
    show: false,
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, sandbox: true },
  });
  window.setAlwaysOnTop(true, 'screen-saver');
  window.setIgnoreMouseEvents(true, { forward: true }); // click-through
  window.loadFile(path.join(__dirname, 'overlay.html'));
  return window;
}

let lastKey = null;

async function tick(window) {
  if (window.isDestroyed()) return;
  const vm = toViewModel(await fetchAdvice(ADVICE_URL));
  if (window.isDestroyed()) return;
  const key = viewKey(vm);
  if (key !== lastKey) { // an unchanged view is never re-sent, so nothing in the window is touched
    lastKey = key;
    window.webContents.send('view-model', vm);
  }
  if (vm.visible && !window.isVisible()) window.showInactive();
  if (!vm.visible && window.isVisible()) window.hide();
}

app.whenReady().then(() => {
  const window = createWindow();
  setInterval(() => { tick(window).catch(() => {}); }, POLL_INTERVAL_MS);
});

app.on('window-all-closed', () => app.quit());
