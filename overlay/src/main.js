'use strict';

const { app, BrowserWindow, Menu, Tray, ipcMain, nativeImage, screen } = require('electron');
const fs = require('node:fs');
const path = require('node:path');
const { fetchAdvice, startPolling } = require('./poller');
const { createBackend, backendCommand } = require('./backend');
const { loadSettings, saveSettings, normalize } = require('./settings');
const { overlayBounds } = require('./layout');
const { trayIconBitmap } = require('./trayIcon');
const { buildStatus } = require('./statusModel');

const PORT = process.env.MACROMIND_PORT || '8765';
const ADVICE_URL = process.env.MACROMIND_URL || `http://127.0.0.1:${PORT}/advice`;
const POLL_INTERVAL_MS = 1000;
const VIEW_MODEL_CHANNEL = 'view-model'; // must match the channel preload.js listens on
const WINDOW_WIDTH = 380;
const WINDOW_HEIGHT = 340;
const SCREEN_EDGE_MARGIN = 24;
const RAISE_INTERVAL_MS = 1000;
let raiseTimer = null;
const ALWAYS_ON_TOP_LEVEL = 'screen-saver'; // keeps the overlay above full-screen games
const TRAY_ICON_SIZE = 16;
const HIDDEN_FLAG = '--hidden';

let overlay = null;
let appWindow = null;
let tray = null;
let backend = null;
let settings = null;
let settingsFile = null;
let logFile = null;
let currentStatus = null;
let lastStatusJson = null;
let lastTone = null;
let lastOverlayVisible = false;
let isQuitting = false;
const startedAt = Date.now();

function log(message) {
  const line = `${new Date().toISOString()} ${message}\n`;
  try {
    if (logFile) fs.appendFileSync(logFile, line);
  } catch { /* logging must never crash the app */ }
  console.error(line.trimEnd());
}

process.on('uncaughtException', (error) => log(`uncaughtException: ${error && error.stack || error}`));
process.on('unhandledRejection', (reason) => log(`unhandledRejection: ${reason && reason.stack || reason}`));

// Windows only ever show their own page: refuse popups and any navigation away from it.
function lockDownNavigation(window) {
  window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', (event) => event.preventDefault());
}

function startBackend() {
  fs.mkdirSync(app.getPath('logs'), { recursive: true });
  logFile = path.join(app.getPath('logs'), 'backend.log');
  if (!app.isPackaged) return; // development: `./gradlew run` is expected to be running already
  const { command, args } = backendCommand({ resourcesPath: process.resourcesPath, platform: process.platform, port: PORT });
  backend = createBackend({
    command, args,
    onStart: (n) => log(`backend start #${n}`),
    onLog: (line) => log(`[backend] ${line}`),
    onError: (error) => log(`backend error: ${error && error.stack || error}`),
    onGiveUp: () => log('backend gave up restarting'),
  });
  backend.start().catch((error) => log(`backend start failed: ${error && error.stack || error}`));
}

function createOverlay() {
  const window = new BrowserWindow({
    width: WINDOW_WIDTH,
    height: WINDOW_HEIGHT,
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

function applyOverlaySettings() {
  if (!overlay || overlay.isDestroyed()) return;
  const bounds = overlayBounds(screen.getPrimaryDisplay().workArea, settings.corner,
    { width: WINDOW_WIDTH, height: WINDOW_HEIGHT }, SCREEN_EDGE_MARGIN);
  overlay.setBounds(bounds);
  overlay.setOpacity(settings.opacity);
  syncOverlayVisibility();
}

function syncOverlayVisibility() {
  if (!overlay || overlay.isDestroyed()) return;
  const visible = settings.overlayEnabled && lastOverlayVisible;
  if (visible && !overlay.isVisible()) overlay.showInactive();
  if (!visible && overlay.isVisible()) overlay.hide();
  if (visible && !raiseTimer) raiseTimer = setInterval(raiseOverlay, RAISE_INTERVAL_MS);
  if (!visible && raiseTimer) { clearInterval(raiseTimer); raiseTimer = null; }
}

// The game window can re-take the top of the z-order when it is clicked; re-assert ours.
function raiseOverlay() {
  if (!overlay || overlay.isDestroyed() || !overlay.isVisible()) return;
  overlay.setAlwaysOnTop(true, ALWAYS_ON_TOP_LEVEL);
  overlay.moveTop();
}

function showOverlayView(view) {
  if (!overlay || overlay.isDestroyed()) return;
  overlay.webContents.send(VIEW_MODEL_CHANNEL, view);
  lastOverlayVisible = view.visible;
  syncOverlayVisibility();
}

function createAppWindow() {
  const window = new BrowserWindow({
    width: 920, height: 640, minWidth: 640, minHeight: 480,
    backgroundColor: '#0f181c',
    show: false,
    webPreferences: {
      preload: path.join(__dirname, 'appPreload.js'),
      contextIsolation: true, sandbox: true, nodeIntegration: false,
    },
  });
  window.setMenuBarVisibility(false);
  lockDownNavigation(window);
  window.on('close', (event) => {
    if (isQuitting) return;
    event.preventDefault(); // the app lives in the tray
    window.hide();
  });
  window.loadFile(path.join(__dirname, 'app', 'index.html'));
  return window;
}

function showAppWindow() {
  if (!appWindow || appWindow.isDestroyed()) return;
  if (appWindow.isMinimized()) appWindow.restore();
  appWindow.show();
  appWindow.focus();
}

function trayImage(tone) {
  const { buffer, width, height } = trayIconBitmap(tone, TRAY_ICON_SIZE);
  return nativeImage.createFromBitmap(buffer, { width, height });
}

function buildTrayMenu() {
  return Menu.buildFromTemplate([
    { label: 'Open MacroMind', click: showAppWindow },
    {
      label: 'Show overlay', type: 'checkbox', checked: settings.overlayEnabled,
      click: (item) => { applySettings({ ...settings, overlayEnabled: item.checked }); },
    },
    { type: 'separator' },
    { label: 'Quit MacroMind', click: () => { app.quit(); } },
  ]);
}

function createTray() {
  tray = new Tray(trayImage('warn'));
  tray.on('click', showAppWindow);
  tray.setContextMenu(buildTrayMenu());
}

function applySettings(next) {
  settings = normalize(next);
  saveSettings(settingsFile, settings);
  applyOverlaySettings();
  if (tray) tray.setContextMenu(buildTrayMenu());
  if (app.isPackaged && (process.platform === 'win32' || process.platform === 'darwin')) { // dev runs cannot register a login item
    app.setLoginItemSettings({ openAtLogin: settings.startWithWindows, args: [HIDDEN_FLAG] });
  }
  if (appWindow && !appWindow.isDestroyed()) appWindow.webContents.send('app:settings', settings);
  return settings;
}

function updateStatus(payload) {
  const status = buildStatus(payload, { reachable: payload !== null, secondsSinceStart: (Date.now() - startedAt) / 1000 });
  currentStatus = status;
  const json = JSON.stringify(status);
  if (json === lastStatusJson) return;
  lastStatusJson = json;
  if (tray && !tray.isDestroyed()) {
    tray.setToolTip(status.view.title);
    if (status.view.tone !== lastTone) tray.setImage(trayImage(status.view.tone));
    lastTone = status.view.tone;
  }
  if (appWindow && !appWindow.isDestroyed()) appWindow.webContents.send('app:status', status);
}

function startStatusLoop() {
  const tick = async () => {
    try { updateStatus(await fetchAdvice(ADVICE_URL)); } catch (error) { log(`status tick failed: ${error && error.stack || error}`); }
    setTimeout(tick, POLL_INTERVAL_MS);
  };
  tick();
}

// A handler that throws would surface as an unhandled rejection in the renderer, so failures are logged and answered with null.
function handle(channel, fn) {
  ipcMain.handle(channel, async (_event, ...args) => {
    try { return await fn(...args); } catch (error) { log(`ipc ${channel} failed: ${error && error.stack || error}`); return null; }
  });
}

function registerIpc() {
  handle('app:get-settings', () => settings);
  handle('app:save-settings', (raw) => applySettings(raw));
  handle('app:get-status', () => currentStatus);
}

async function quit() {
  try { if (backend) await backend.stop(); } catch (error) { log(`backend stop failed: ${error && error.stack || error}`); }
  app.exit(0);
}

if (!app.requestSingleInstanceLock()) {
  app.exit(0);
} else {
  app.on('second-instance', showAppWindow);
  app.on('before-quit', (event) => {
    if (isQuitting) return;
    isQuitting = true;
    event.preventDefault();
    quit();
  });
  app.on('window-all-closed', () => { /* the tray keeps the app alive */ });

  app.whenReady().then(() => {
    startBackend();
    settingsFile = path.join(app.getPath('userData'), 'settings.json');
    settings = loadSettings(settingsFile);
    registerIpc();
    createTray();
    appWindow = createAppWindow();
    if (!process.argv.includes(HIDDEN_FLAG)) appWindow.once('ready-to-show', showAppWindow);
    overlay = createOverlay();
    applyOverlaySettings();
    // Start only once the page has loaded: a view sent earlier would reach a renderer that has not subscribed yet,
    // and an unchanged view is never sent again, so it would be lost for good.
    overlay.webContents.once('did-finish-load', () => {
      startPolling({
        fetch: () => fetchAdvice(ADVICE_URL),
        onView: showOverlayView,
        intervalMs: POLL_INTERVAL_MS,
        onError: (error) => log(`overlay tick failed: ${error && error.stack || error}`),
      });
    });
    startStatusLoop();
  }).catch((error) => log(`startup failed: ${error && error.stack || error}`));
}
