'use strict';

const { app, BrowserWindow, screen } = require('electron');
const path = require('node:path');
const { fetchAdvice, startPolling } = require('./poller');

const ADVICE_URL = process.env.MACROMIND_URL || 'http://127.0.0.1:8765/advice';
const POLL_INTERVAL_MS = 1000;
const VIEW_MODEL_CHANNEL = 'view-model'; // must match the channel preload.js listens on
const WINDOW_WIDTH = 380;
const WINDOW_HEIGHT = 340;
const SCREEN_EDGE_MARGIN = 24;
const ALWAYS_ON_TOP_LEVEL = 'screen-saver'; // keeps the overlay above full-screen games

function createWindow() {
  const { workArea } = screen.getPrimaryDisplay();
  const window = new BrowserWindow({
    width: WINDOW_WIDTH,
    height: WINDOW_HEIGHT,
    x: workArea.x + workArea.width - WINDOW_WIDTH - SCREEN_EDGE_MARGIN,
    y: workArea.y + SCREEN_EDGE_MARGIN,
    transparent: true,
    frame: false,
    resizable: false,
    focusable: false, // must never take keyboard focus from the game
    skipTaskbar: true,
    hasShadow: false,
    show: false,
    webPreferences: { preload: path.join(__dirname, 'preload.js'), contextIsolation: true, sandbox: true },
  });
  window.setAlwaysOnTop(true, ALWAYS_ON_TOP_LEVEL);
  window.setIgnoreMouseEvents(true, { forward: true }); // click-through
  lockDownNavigation(window);
  window.loadFile(path.join(__dirname, 'overlay.html'));
  return window;
}

// The overlay only ever shows its own page: refuse popups and any navigation away from it.
function lockDownNavigation(window) {
  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', (event) => event.preventDefault());
}

function syncVisibility(window, visible) {
  if (visible && !window.isVisible()) window.showInactive();
  if (!visible && window.isVisible()) window.hide();
}

function show(window, view) {
  if (window.isDestroyed()) return;
  window.webContents.send(VIEW_MODEL_CHANNEL, view);
  syncVisibility(window, view.visible);
}

app.whenReady().then(() => {
  const window = createWindow();
  // Start only once the page has loaded: a view sent earlier would reach a renderer that has not subscribed yet,
  // and an unchanged view is never sent again, so it would be lost for good.
  window.webContents.once('did-finish-load', () => {
    startPolling({
      fetch: () => fetchAdvice(ADVICE_URL),
      onView: (view) => show(window, view),
      intervalMs: POLL_INTERVAL_MS,
    });
  });
});

app.on('window-all-closed', () => app.quit());
