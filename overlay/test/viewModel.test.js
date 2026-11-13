const test = require('node:test');
const assert = require('node:assert/strict');
const { toViewModel, MAX_ROWS, MAX_ALTERNATIVES, DISCONNECTED } = require('../src/viewModel');

const advice = (item, priority, extra = {}) => ({
  item, reasoning: `why ${item}`, priority, category: 'DEFENSIVE', confidence: 'EXACT', target: null, ...extra,
});

test('hides when the backend says not visible', () => {
  const vm = toViewModel({ visible: false, advice: [advice('A', 'CRITICAL')] });
  assert.equal(vm.visible, false);
});

test('hides when visible but there is no advice', () => {
  assert.equal(toViewModel({ visible: true, advice: [] }).visible, false);
});

test('hides when the backend is unreachable (null payload)', () => {
  assert.deepEqual(toViewModel(null), DISCONNECTED);
});

test('hides on a malformed payload instead of throwing', () => {
  for (const bad of [undefined, 42, 'x', {}, { visible: true }, { visible: true, advice: 'no' }]) {
    assert.equal(toViewModel(bad).visible, false, JSON.stringify(bad));
  }
});

test('shows at most MAX_ROWS rows, keeping the order the backend ranked them in', () => {
  const many = Array.from({ length: 8 }, (_, i) => advice(`Item${i}`, 'RECOMMENDED', { reasoning: `reason ${i}` }));
  const vm = toViewModel({ visible: true, advice: many });
  assert.equal(vm.rows.length, MAX_ROWS);
  assert.deepEqual(vm.rows.map((r) => r.item), many.slice(0, MAX_ROWS).map((a) => a.item));
});

test('marks estimated advice and keeps exact advice unmarked', () => {
  const vm = toViewModel({ visible: true, advice: [advice('A', 'CRITICAL', { confidence: 'ESTIMATED' }), advice('B', 'CRITICAL')] });
  assert.equal(vm.rows[0].estimated, true);
  assert.equal(vm.rows[1].estimated, false);
});

test('maps priority to a css class and tolerates unknown priorities', () => {
  const known = toViewModel({ visible: true, advice: [advice('A', 'CRITICAL'), advice('B', 'RECOMMENDED'), advice('C', 'SITUATIONAL')] });
  assert.deepEqual(known.rows.map((r) => r.level), ['critical', 'recommended', 'situational']);
  assert.equal(toViewModel({ visible: true, advice: [advice('D', 'WEIRD')] }).rows[0].level, 'situational');
});

test('carries the target champion when there is one and null otherwise', () => {
  const vm = toViewModel({ visible: true, advice: [advice('A', 'CRITICAL', { target: 'Zed' }), advice('B', 'CRITICAL')] });
  assert.equal(vm.rows[0].target, 'Zed');
  assert.equal(vm.rows[1].target, null);
});

test('skips rows without an item name', () => {
  const vm = toViewModel({ visible: true, advice: [{ priority: 'CRITICAL' }, advice('Real', 'CRITICAL')] });
  assert.deepEqual(vm.rows.map((r) => r.item), ['Real']);
});

test('merges rows that share a reason and priority into one row listing the alternatives', () => {
  const vm = toViewModel({ visible: true, advice: [
    advice('Mortal Reminder', 'CRITICAL', { reasoning: 'heavy healing' }),
    advice('Chempunk Chainsword', 'CRITICAL', { reasoning: 'heavy healing' }),
    advice('Thornmail', 'CRITICAL', { reasoning: 'heavy healing' }),
  ] });
  assert.equal(vm.rows.length, 1);
  assert.equal(vm.rows[0].item, 'Mortal Reminder');
  assert.deepEqual(vm.rows[0].alternatives, ['Chempunk Chainsword', 'Thornmail']);
});

test('does not merge rows with the same reason but a different priority', () => {
  const vm = toViewModel({ visible: true, advice: [
    advice('A', 'CRITICAL', { reasoning: 'same' }), advice('B', 'RECOMMENDED', { reasoning: 'same' }),
  ] });
  assert.equal(vm.rows.length, 2);
});

test('merging frees row slots so more distinct reasons fit', () => {
  const vm = toViewModel({ visible: true, advice: [
    advice('A1', 'CRITICAL', { reasoning: 'r1' }), advice('A2', 'CRITICAL', { reasoning: 'r1' }),
    advice('B1', 'CRITICAL', { reasoning: 'r2' }), advice('B2', 'CRITICAL', { reasoning: 'r2' }),
    advice('C1', 'RECOMMENDED', { reasoning: 'r3' }),
  ] });
  assert.deepEqual(vm.rows.map((r) => r.item), ['A1', 'B1', 'C1']);
});

test('limits alternatives so a card stays short', () => {
  const same = Array.from({ length: 9 }, (_, i) => advice(`Item${i}`, 'CRITICAL', { reasoning: 'r' }));
  assert.equal(toViewModel({ visible: true, advice: same }).rows[0].alternatives.length, MAX_ALTERNATIVES);
});

test('a merged row is estimated if any of its items is, and keeps the first target', () => {
  const vm = toViewModel({ visible: true, advice: [
    advice('A', 'CRITICAL', { reasoning: 'r', target: 'Zed' }),
    advice('B', 'CRITICAL', { reasoning: 'r', confidence: 'ESTIMATED' }),
  ] });
  assert.equal(vm.rows[0].estimated, true);
  assert.equal(vm.rows[0].target, 'Zed');
});

test('a single-item row has no alternatives', () => {
  assert.deepEqual(toViewModel({ visible: true, advice: [advice('A', 'CRITICAL')] }).rows[0].alternatives, []);
});

test('builds an icon url from the patch version and item id', () => {
  const vm = toViewModel({ visible: true, iconVersion: '16.22.1', advice: [advice('Thornmail', 'CRITICAL', { itemId: 3075 })] });
  assert.equal(vm.rows[0].icon, 'https://ddragon.leagueoflegends.com/cdn/16.22.1/img/item/3075.png');
});

test('has no icon when the patch version or item id is missing or malformed', () => {
  assert.equal(toViewModel({ visible: true, iconVersion: '', advice: [advice('A', 'CRITICAL', { itemId: 3075 })] }).rows[0].icon, null);
  assert.equal(toViewModel({ visible: true, advice: [advice('A', 'CRITICAL', { itemId: 3075 })] }).rows[0].icon, null);
  assert.equal(toViewModel({ visible: true, iconVersion: '16.22.1', advice: [advice('A', 'CRITICAL')] }).rows[0].icon, null);
});

test('refuses an icon version that is not a plain patch number, so the url cannot be redirected', () => {
  for (const bad of ['../evil', 'x/y', '16.22.1/../../a', '16.22.1?x=1', 'http://evil']) {
    const vm = toViewModel({ visible: true, iconVersion: bad, advice: [advice('A', 'CRITICAL', { itemId: 3075 })] });
    assert.equal(vm.rows[0].icon, null, bad);
  }
});

test('refuses a non-integer item id', () => {
  for (const bad of ['3075', 3075.5, -1, '../1', null]) {
    const vm = toViewModel({ visible: true, iconVersion: '16.22.1', advice: [advice('A', 'CRITICAL', { itemId: bad })] });
    assert.equal(vm.rows[0].icon, null, String(bad));
  }
});
