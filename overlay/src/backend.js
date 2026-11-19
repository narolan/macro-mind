'use strict';

const { spawn } = require('node:child_process');
const path = require('node:path');

const DEFAULT_RESTART_DELAY_MS = 1000;
const DEFAULT_MAX_RESTARTS = 5;
const STOP_TIMEOUT_MS = 3000;

/** How to start the Java backend that is bundled with the installed app. */
function backendCommand({ resourcesPath, platform, port }) {
  const java = platform === 'win32' ? 'java.exe' : 'java';
  const root = path.join(resourcesPath, 'backend');
  return {
    command: path.join(root, 'runtime', 'bin', java),
    args: ['-Xmx256m', `-Dmacromind.port=${port}`, '-cp', path.join(root, 'lib', '*'), 'com.macromind.Main'],
  };
}

/**
 * Keeps the backend process alive: restarts it when it dies (up to a limit, so a broken install does not spin),
 * and stops it for good on stop(). Never throws: problems go to onError / onGiveUp.
 */
function createBackend({
  command, args, restartDelayMs = DEFAULT_RESTART_DELAY_MS, maxRestarts = DEFAULT_MAX_RESTARTS,
  onStart = () => {}, onLog = () => {}, onError = () => {}, onGiveUp = () => {},
}) {
  let child = null;
  let stopping = false;
  let restarts = 0;
  let timer = null;
  let starts = 0;

  function launch() {
    starts += 1;
    onStart(starts);
    let proc;
    try {
      proc = spawn(command, args, { stdio: ['ignore', 'pipe', 'pipe'], windowsHide: true });
    } catch (error) {
      onError(error);
      return;
    }
    child = proc;
    for (const stream of [proc.stdout, proc.stderr]) {
      let pending = '';
      stream.setEncoding('utf8');
      stream.on('data', (chunk) => {
        const lines = (pending + chunk).split(/\r?\n/);
        pending = lines.pop();
        lines.filter(Boolean).forEach(onLog);
      });
    }
    proc.on('error', (error) => { onError(error); });
    proc.on('exit', () => {
      if (child === proc) child = null;
      if (stopping) return;
      if (restarts >= maxRestarts) { onGiveUp(); return; }
      restarts += 1;
      timer = setTimeout(launch, restartDelayMs);
    });
  }

  return {
    async start() {
      stopping = false;
      launch();
    },
    isRunning: () => child !== null && child.exitCode === null,
    async stop() {
      stopping = true;
      clearTimeout(timer);
      const proc = child;
      if (!proc || proc.exitCode !== null) return;
      await new Promise((resolve) => {
        const force = setTimeout(() => proc.kill('SIGKILL'), STOP_TIMEOUT_MS);
        proc.once('exit', () => { clearTimeout(force); resolve(); });
        proc.kill();
      });
    },
  };
}

module.exports = { createBackend, backendCommand };
