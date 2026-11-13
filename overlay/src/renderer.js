'use strict';

const list = document.getElementById('advice');

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

window.macromind.onViewModel((viewModel) => {
  const next = nextCards(viewModel.rows);
  removeDeparted(next);
  placeInOrder(next);
  shown = next;
});
