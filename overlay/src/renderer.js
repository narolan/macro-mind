'use strict';

const list = document.getElementById('advice');

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text; // textContent only: advice text is never parsed as HTML
  return node;
}

// Cards already on screen are kept, so a card that did not change is never touched (and never re-animates).
let shown = new Map();

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

// The renderer is a small script, so it cannot require() the view-model module; it keeps its own key per row.
const keyOf = (row) => JSON.stringify([row.item, row.level, row.reasoning, row.estimated, row.target, row.alternatives, row.icon]);

window.macromind.onViewModel((vm) => {
  const rowsByKey = new Map(vm.rows.map((row) => [keyOf(row), row]));
  const next = new Map();
  for (const [key, row] of rowsByKey) next.set(key, shown.get(key) || cardFor(row));
  for (const [key, card] of shown) if (!next.has(key)) card.remove();       // only departed cards leave
  let previous = null;
  for (const card of next.values()) {                                        // only new or moved cards are placed
    const expected = previous ? previous.nextSibling : list.firstChild;
    if (card !== expected) list.insertBefore(card, expected);
    previous = card;
  }
  shown = next;
});
