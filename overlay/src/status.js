'use strict';

const STARTUP_GRACE_SECONDS = 20; // the backend needs a moment to load Riot's item data before it answers

const KNOWN = Object.freeze({
  NO_GAME: {
    state: 'waiting', tone: 'ok', title: 'Waiting for a game',
    detail: 'MacroMind is running. Start a League match and item advice will appear on your screen.',
  },
  IN_GAME: {
    state: 'in-game', tone: 'ok', title: 'In game',
    detail: 'Reading your match. Advice shows up on screen when you are dead or when something urgent changes.',
  },
  NO_DATA: {
    state: 'no-data', tone: 'bad', title: "Can't load item data",
    detail: "MacroMind could not download Riot's item data. Check your internet connection, then restart the app.",
  },
  ERROR: {
    state: 'error', tone: 'bad', title: 'Something went wrong',
    detail: "The game answered but MacroMind could not read it. Details are in the log file; the next patch may have changed it.",
  },
});

const UNKNOWN = { state: 'error', tone: 'bad', title: 'Something went wrong', detail: 'MacroMind got an answer it does not understand. Details are in the log file.' };

/**
 * What the app window shows. Never throws; anything unexpected is shown as a problem, never as healthy.
 * @param {unknown} payload   The backend's /advice payload, or null when it did not answer.
 * @param {{reachable: boolean, secondsSinceStart?: number}} context
 */
function describeStatus(payload, { reachable, secondsSinceStart = 0 }) {
  if (!reachable) {
    return secondsSinceStart < STARTUP_GRACE_SECONDS
      ? { state: 'starting', tone: 'warn', title: 'Starting…', detail: 'MacroMind is starting up. This takes a few seconds.', dataVersion: null }
      : { state: 'unreachable', tone: 'bad', title: 'MacroMind is not responding', detail: 'The background service is not answering. Restart the app; if it keeps happening, check the log file.', dataVersion: null };
  }
  const known = payload && typeof payload === 'object' ? KNOWN[payload.status] : undefined;
  const version = payload && typeof payload.iconVersion === 'string' && payload.iconVersion.length > 0 ? payload.iconVersion : null;
  return { ...(known || UNKNOWN), dataVersion: version };
}

module.exports = { describeStatus, STARTUP_GRACE_SECONDS };
