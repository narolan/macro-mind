'use strict';

const MAX_ROWS = 3;
const MAX_ALTERNATIVES = 3;
const ICON_HOST = 'https://ddragon.leagueoflegends.com/cdn';
const PATCH_VERSION = /^\d+(\.\d+){1,3}$/;
const DEFAULT_LEVEL = 'situational';
const LEVELS = Object.freeze({ CRITICAL: 'critical', RECOMMENDED: 'recommended' });

/**
 * @typedef {object} Row
 * @property {string} item          Name of the item to buy.
 * @property {string} reasoning     Why it is advised.
 * @property {string} level         'critical' | 'recommended' | 'situational' (drives the card colour).
 * @property {boolean} estimated    True when the advice rests on estimated enemy stats.
 * @property {string|null} target   The enemy champion the advice is about, if any.
 * @property {string|null} icon     Item icon URL, or null when none can be built.
 * @property {string[]} alternatives Other items with the same reason.
 * @property {string} key           Identifies what the row looks like; equal keys mean the same card.
 *
 * @typedef {object} ViewModel
 * @property {boolean} visible
 * @property {Row[]} rows
 * @property {{message: string, level: string, key: string}[]} alerts  Heads-ups drawn above the cards.
 * @property {{name: string, secondsLeft: number}[]} timers  Objective countdowns.
 */

/** @type {ViewModel} */
const HIDDEN_VIEW = Object.freeze({ visible: false, rows: [], alerts: [], timers: [] });

/** Only a plain patch number and a positive integer id may reach the url; anything else means no icon. */
function iconUrl(version, itemId) {
  if (typeof version !== 'string' || !PATCH_VERSION.test(version)) return null;
  if (!Number.isInteger(itemId) || itemId <= 0) return null;
  return `${ICON_HOST}/${version}/img/item/${itemId}.png`;
}

const hasItemName = (advice) => Boolean(advice) && typeof advice.item === 'string' && advice.item.length > 0;

/** Everything about a row except its key. */
function toRowContent(advice, iconVersion) {
  return {
    item: advice.item,
    reasoning: typeof advice.reasoning === 'string' ? advice.reasoning : '',
    level: LEVELS[advice.priority] || DEFAULT_LEVEL,
    estimated: advice.confidence === 'ESTIMATED',
    target: typeof advice.target === 'string' ? advice.target : null,
    icon: iconUrl(iconVersion, advice.itemId),
    alternatives: [],
  };
}

/** A stable string for a row's content: two rows with the same key look the same. */
function rowKey(content) {
  return JSON.stringify([content.item, content.level, content.reasoning, content.estimated, content.target,
    content.alternatives, content.icon]);
}

/** A copy of group that also includes row's item as an alternative (up to the limit), and row's flags. */
function mergeInto(group, row) {
  return {
    ...group,
    alternatives: group.alternatives.length < MAX_ALTERNATIVES ? [...group.alternatives, row.item] : group.alternatives,
    estimated: group.estimated || row.estimated,
    target: group.target || row.target,
  };
}

/** Rows with the same reason and priority become one row: the first item leads, the rest are alternatives. */
function groupRows(rows) {
  const groups = new Map();
  for (const row of rows) {
    const groupId = `${row.level}|${row.reasoning}`;
    groups.set(groupId, groups.has(groupId) ? mergeInto(groups.get(groupId), row) : row);
  }
  return [...groups.values()];
}

/** The alerts of a payload that have a message; level is the same colour scale as the cards. */
function toAlerts(payload) {
  if (!Array.isArray(payload.alerts)) return [];
  return payload.alerts
    .filter((alert) => Boolean(alert) && typeof alert.message === 'string' && alert.message.length > 0)
    .map((alert) => ({ message: alert.message, level: LEVELS[alert.priority] || DEFAULT_LEVEL, key: `${alert.priority}|${alert.message}` }));
}

/** The timers of a payload that have a name and a whole number of seconds. */
function toTimers(payload) {
  if (!Array.isArray(payload.timers)) return [];
  return payload.timers
    .filter((timer) => Boolean(timer) && typeof timer.name === 'string' && timer.name.length > 0 && Number.isFinite(timer.secondsLeft))
    .map((timer) => ({ name: timer.name, secondsLeft: Math.round(timer.secondsLeft) }));
}

/**
 * Turns the backend's /advice payload into what the window draws. Never throws: bad input hides the overlay.
 * @param {unknown} payload
 * @returns {ViewModel}
 */
function toViewModel(payload) {
  if (!payload || payload.visible !== true) {
    return HIDDEN_VIEW;
  }
  const rows = toRows(payload, MAX_ROWS);
  const alerts = toAlerts(payload);
  const timers = toTimers(payload);
  return rows.length + alerts.length + timers.length === 0 ? HIDDEN_VIEW : { visible: true, rows, alerts, timers };
}

/**
 * The advice in a payload as merged rows, at most maxRows of them. Unlike toViewModel it ignores payload.visible, which
 * says whether the overlay should show, not whether there is advice. Never throws.
 * @param {unknown} payload
 * @param {number} maxRows
 * @returns {Row[]}
 */
function toRows(payload, maxRows) {
  if (!payload || !Array.isArray(payload.advice)) {
    return [];
  }
  const rows = payload.advice.filter(hasItemName).map((advice) => toRowContent(advice, payload.iconVersion));
  return groupRows(rows).slice(0, maxRows).map((content) => ({ ...content, key: rowKey(content) }));
}

/**
 * A stable string for a whole view; equal keys mean nothing visible changed.
 * @param {ViewModel} viewModel
 * @returns {string}
 */
function viewKey(viewModel) {
  if (!viewModel.visible) return '';
  const parts = [...viewModel.rows.map((row) => row.key), ...viewModel.alerts.map((alert) => alert.key),
    ...viewModel.timers.map((timer) => `${timer.name}:${timer.secondsLeft}`)];
  return parts.join('\n');
}

module.exports = { toViewModel, toRows, viewKey, rowKey, MAX_ROWS, MAX_ALTERNATIVES, HIDDEN_VIEW };
