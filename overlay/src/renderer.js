'use strict';

const list = document.getElementById('advice');

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text; // textContent only: advice text is never parsed as HTML
  return node;
}

window.macromind.onViewModel((vm) => {
  list.replaceChildren(...vm.rows.map((row) => {
    const li = element('li', `row ${row.level}`);
    const name = element('div', 'name', row.item);
    if (row.estimated) name.append(element('span', 'badge', 'est.'));
    li.append(name, element('div', 'reason', row.reasoning));
    if (row.alternatives.length > 0) li.append(element('div', 'alternatives', `or ${row.alternatives.join(', ')}`));
    if (row.target) li.append(element('div', 'target', `vs ${row.target}`));
    return li;
  }));
});
