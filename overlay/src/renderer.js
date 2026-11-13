'use strict';

const list = document.getElementById('advice');

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text; // textContent only: advice text is never parsed as HTML
  return node;
}

// Cards already on screen are kept (not re-created), so the 1s refresh never replays the fade-in.
const shown = new Map();

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

window.macromind.onViewModel((vm) => {
  const next = new Map();
  for (const row of vm.rows) {
    const key = JSON.stringify([row.item, row.level, row.reasoning, row.estimated, row.target, row.alternatives]);
    next.set(key, shown.get(key) || cardFor(row));
  }
  shown.clear();
  next.forEach((card, key) => shown.set(key, card));
  list.replaceChildren(...next.values());
});
