'use strict';

const list = document.getElementById('advice');
const alertList = document.getElementById('alerts');
const timerList = document.getElementById('timers');

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text; // textContent only: advice text is never parsed as HTML
  return node;
}

function cardFor(row) {
  const li = element('li', `row ${row.level}`);
  const name = element('div', 'name');
  if (row.icon) {
    const icon = element('img', 'icon');
    icon.src = row.icon;
    icon.alt = '';
    icon.addEventListener('error', () => icon.remove()); // offline or unknown item: just no icon
    name.append(icon);
  }
  name.append(element('span', '', row.item));
  if (row.estimated) name.append(element('span', 'badge', 'est.'));
  li.append(name, element('div', 'reason', row.reasoning));
  if (row.alternatives.length > 0) li.append(element('div', 'alternatives', `or ${row.alternatives.join(', ')}`));
  if (row.target) li.append(element('div', 'target', `vs ${row.target}`));
  return li;
}

// Cards already on screen are kept, so a card that did not change is never touched (and never re-animates).
let shown = new Map();

/** Key -> card for the new rows: an existing card is reused, only new rows build one. */
function nextCards(rows) {
  return new Map(rows.map((row) => [row.key, shown.get(row.key) || cardFor(row)]));
}

function removeDeparted(next) {
  for (const [key, card] of shown) {
    if (!next.has(key)) card.remove();
  }
}

/** Puts the cards in order, moving only the ones that are not already in the right place. */
function placeInOrder(next) {
  let previous = null;
  for (const card of next.values()) {
    const expected = previous ? previous.nextSibling : list.firstChild;
    if (card !== expected) list.insertBefore(card, expected);
    previous = card;
  }
}

let shownAlerts = '';

/** Alerts are replaced only when they change, so an unchanged alert never re-animates. */
function renderAlerts(alerts) {
  const key = alerts.map((alert) => alert.key).join('\n');
  if (key === shownAlerts) return;
  shownAlerts = key;
  alertList.replaceChildren(...alerts.map((alert) => element('li', `alert ${alert.level}`, alert.message)));
}

const timerNodes = new Map(); // name -> { li, left }

/** Timers are updated in place: the same node keeps its place while its seconds tick down. */
function renderTimers(timers) {
  const names = new Set(timers.map((timer) => timer.name));
  for (const [name, node] of timerNodes) {
    if (!names.has(name)) {
      node.li.remove();
      timerNodes.delete(name);
    }
  }
  for (const timer of timers) {
    if (!timerNodes.has(timer.name)) {
      const left = element('span', 'left');
      const li = element('li', 'timer');
      li.append(element('span', '', timer.name), left);
      timerNodes.set(timer.name, { li, left });
      timerList.append(li);
    }
    timerNodes.get(timer.name).left.textContent = `${timer.secondsLeft}s`;
  }
}

const TONE_HZ = 880;
const TONE_SECONDS = 0.18;
const TONE_GAIN = 0.15;
const TONE_FLOOR = 0.001; // exponential ramps cannot reach 0
let soundEnabled = true;
const cueTracker = createCueTracker(); // its first view is only a baseline
let audio = null;

/** A short synthesized beep, no audio files. A failure here must never stop the drawing. */
function ping() {
  try {
    audio = audio || new AudioContext();
    const osc = audio.createOscillator();
    const gain = audio.createGain();
    osc.frequency.value = TONE_HZ;
    gain.gain.setValueAtTime(TONE_GAIN, audio.currentTime);
    gain.gain.exponentialRampToValueAtTime(TONE_FLOOR, audio.currentTime + TONE_SECONDS);
    osc.connect(gain).connect(audio.destination);
    osc.start();
    osc.stop(audio.currentTime + TONE_SECONDS);
  } catch { /* no audio device: stay silent */ }
}

window.macromind.onSoundEnabled((enabled) => { soundEnabled = enabled; });

window.macromind.onViewModel((viewModel) => {
  try {
    if (cueTracker.shouldPing(viewModel, soundEnabled)) ping(); // createCueTracker comes from cues.js
  } catch { /* a cue problem must never stop the drawing */ }
  renderAlerts(viewModel.alerts || []);
  renderTimers(viewModel.timers || []);
  const next = nextCards(viewModel.rows);
  removeDeparted(next);
  placeInOrder(next);
  shown = next;
});
