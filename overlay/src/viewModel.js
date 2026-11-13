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
 */

/** @type {ViewModel} */
const HIDDEN_VIEW = Object.freeze({ visible: false, rows: [] });

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

/**
 * Turns the backend's /advice payload into what the window draws. Never throws: bad input hides the overlay.
 * @param {unknown} payload
 * @returns {ViewModel}
 */
function toViewModel(payload) {
  if (!payload || payload.visible !== true || !Array.isArray(payload.advice)) {
    return HIDDEN_VIEW;
  }
  const rows = payload.advice.filter(hasItemName).map((advice) => toRowContent(advice, payload.iconVersion));
  const shown = groupRows(rows).slice(0, MAX_ROWS).map((content) => ({ ...content, key: rowKey(content) }));
  return shown.length === 0 ? HIDDEN_VIEW : { visible: true, rows: shown };
}

/**
 * A stable string for a whole view; equal keys mean nothing visible changed.
 * @param {ViewModel} viewModel
 * @returns {string}
 */
function viewKey(viewModel) {
  return viewModel.visible ? viewModel.rows.map((row) => row.key).join('\n') : '';
}

module.exports = { toViewModel, viewKey, rowKey, MAX_ROWS, MAX_ALTERNATIVES, HIDDEN_VIEW };
