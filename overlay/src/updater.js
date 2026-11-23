'use strict';

const CHECK_INTERVAL_MS = 4 * 60 * 60 * 1000;

// Checks GitHub Releases (see publish in electron-builder.yml), downloads a newer version in the background
// and installs it on the next quit. `updater` is injected so tests need no Electron.
function startAutoUpdate({ updater, log, intervalMs = CHECK_INTERVAL_MS, setIntervalFn = setInterval }) {
  updater.autoDownload = true;
  updater.autoInstallOnAppQuit = true;
  updater.on('update-available', (info) => log(`update available: ${info.version}`));
  updater.on('update-downloaded', (info) => log(`update ${info.version} downloaded, installs on quit`));
  updater.on('error', (error) => log(`update failed: ${error && error.stack || error}`));
  const check = () => updater.checkForUpdates().catch((error) => log(`update check failed: ${error && error.stack || error}`));
  check();
  const timer = setIntervalFn(check, intervalMs);
  if (timer && timer.unref) timer.unref();
  return timer;
}

module.exports = { startAutoUpdate, CHECK_INTERVAL_MS };
