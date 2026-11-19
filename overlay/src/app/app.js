'use strict';

const api = window.macromindApp;
const $ = (id) => document.getElementById(id);

// ---- tabs (standard roving-tabindex tablist) ----
const tabs = [...document.querySelectorAll('[role="tab"]')];

function selectTab(tab) {
  for (const other of tabs) {
    const active = other === tab;
    other.setAttribute('aria-selected', String(active));
    other.tabIndex = active ? 0 : -1;
    $(other.getAttribute('aria-controls')).hidden = !active;
  }
  tab.focus();
}

tabs.forEach((tab, index) => {
  tab.addEventListener('click', () => selectTab(tab));
  tab.addEventListener('keydown', (event) => {
    const step = { ArrowDown: 1, ArrowRight: 1, ArrowUp: -1, ArrowLeft: -1 }[event.key];
    if (step === undefined) return;
    event.preventDefault();
    selectTab(tabs[(index + step + tabs.length) % tabs.length]);
  });
});

// ---- status ----
function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text; // textContent only: nothing from the backend is ever parsed as HTML
  return node;
}

function renderStages(line, tone) {
  const list = $('stages');
  list.dataset.tone = tone;
  list.dataset.problem = String(line.problem);
  // a problem is not a stage: it gets its own square node at the head of the line and lights nothing else
  const problem = list.querySelector('[data-stage="problem"]');
  problem.hidden = !line.problem;
  if (line.problem) problem.setAttribute('aria-current', 'step'); else problem.removeAttribute('aria-current');
  for (const stage of line.stages) {
    const item = list.querySelector(`[data-stage="${stage.id}"]`);
    if (stage.current) item.setAttribute('aria-current', 'step'); else item.removeAttribute('aria-current');
    item.dataset.done = String(stage.done);
  }
}

function renderAdvice(rows) {
  const list = $('advice-list');
  list.replaceChildren(...rows.map((row) => {
    const item = el('li', 'advice-item');
    item.dataset.level = row.level;
    if (row.icon) {
      const icon = el('img');
      icon.src = row.icon;
      icon.alt = '';
      icon.addEventListener('error', () => icon.replaceWith(el('span', 'noimg'))); // offline: keep the layout
      item.append(icon);
    } else {
      item.append(el('span', 'noimg'));
    }
    const name = el('div', 'advice-name', row.item);
    if (row.estimated) name.append(el('span', 'est', 'est.'));
    item.append(name, el('div', 'advice-reason', row.reasoning));
    if (row.alternatives.length > 0) item.append(el('div', 'advice-alt', `or ${row.alternatives.join(', ')}`));
    return item;
  }));
  $('advice-empty').hidden = rows.length > 0;
}

function renderStatus(status) {
  const view = status.view;
  $('state-title').textContent = view.title;
  $('state-detail').textContent = view.detail;
  $('state').dataset.tone = view.tone;
  renderStages(status.stageLine, view.tone);
  renderAdvice(status.rows);
  $('advice').hidden = status.stageLine.problem; // no advice list next to a problem: an empty list would read as "all fine"
  $('data-version').textContent = view.dataVersion ? `Item data: patch ${view.dataVersion}` : 'Item data: not loaded yet';
}

// ---- settings: saved the moment a control changes, with plain confirmation ----
const controls = { overlayEnabled: $('overlayEnabled'), corner: $('corner'), opacity: $('opacity'), startWithWindows: $('startWithWindows') };

function readControls() {
  return {
    overlayEnabled: controls.overlayEnabled.checked,
    corner: controls.corner.value,
    opacity: Number(controls.opacity.value) / 100,
    startWithWindows: controls.startWithWindows.checked,
  };
}

function showControls(settings) {
  controls.overlayEnabled.checked = settings.overlayEnabled;
  controls.corner.value = settings.corner;
  controls.opacity.value = String(Math.round(settings.opacity * 100));
  controls.startWithWindows.checked = settings.startWithWindows;
  $('opacity-value').textContent = `${controls.opacity.value}%`;
}

let savedTimer = null;
async function save() {
  $('opacity-value').textContent = `${controls.opacity.value}%`;
  try {
    showControls(await api.saveSettings(readControls())); // show what was really stored, not what was typed
    $('saved').textContent = 'Saved';
  } catch {
    $('saved').textContent = 'Could not save. Your changes will be lost when the app closes.';
  }
  clearTimeout(savedTimer);
  savedTimer = setTimeout(() => { $('saved').textContent = ''; }, 2500);
}

Object.values(controls).forEach((control) => control.addEventListener('change', save));
controls.opacity.addEventListener('input', () => { $('opacity-value').textContent = `${controls.opacity.value}%`; });

// ---- start ----
api.onStatus(renderStatus);
api.getStatus().then(renderStatus).catch(() => {});
api.getSettings().then(showControls).catch(() => {});
