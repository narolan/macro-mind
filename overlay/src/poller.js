'use strict';

const http = require('node:http');
const { toViewModel, viewKey } = require('./viewModel');

const DEFAULT_TIMEOUT_MS = 800;

/** GET the advice JSON. Resolves null (never rejects) when the backend is down, slow, or answers badly. */
function fetchAdvice(url, timeoutMs = DEFAULT_TIMEOUT_MS) {
  return new Promise((resolve) => {
    const request = http.get(url, { timeout: timeoutMs }, (response) => {
      if (response.statusCode !== 200) {
        response.resume();
        resolve(null);
        return;
      }
      let body = '';
      response.setEncoding('utf8');
      response.on('data', (chunk) => { body += chunk; });
      response.on('end', () => {
        try { resolve(JSON.parse(body)); } catch { resolve(null); }
      });
      response.on('error', () => resolve(null));
    });
    request.on('timeout', () => { request.destroy(); resolve(null); });
    request.on('error', () => resolve(null));
  });
}

/**
 * Polls and hands each CHANGED view to onView. The next poll is scheduled only after the current one is done, so
 * polls never overlap. Returns a function that stops the loop.
 *
 * @param {object} options
 * @param {() => Promise<unknown>} options.fetch   Resolves the raw /advice payload, or null.
 * @param {(view: import('./viewModel').ViewModel) => void} options.onView
 * @param {number} options.intervalMs
 * @param {(error: unknown) => void} [options.onError]
 */
function startPolling({ fetch, onView, intervalMs, onError = (error) => console.error('MacroMind overlay tick failed', error) }) {
  let lastKey = null; // an unchanged view is never re-sent, so nothing in the window is touched
  let timer = null;
  let stopped = false;

  async function tick() {
    try {
      const view = toViewModel(await fetch());
      const key = viewKey(view);
      if (key !== lastKey) {
        lastKey = key;
        onView(view);
      }
    } catch (error) {
      onError(error);
    }
    if (!stopped) timer = setTimeout(tick, intervalMs);
  }

  tick();
  return () => {
    stopped = true;
    clearTimeout(timer);
  };
}

module.exports = { fetchAdvice, startPolling };
