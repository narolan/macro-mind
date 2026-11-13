const test = require('node:test');
const assert = require('node:assert/strict');
const { toViewModel, MAX_ROWS, DISCONNECTED } = require('../src/viewModel');

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
  const many = Array.from({ length: 8 }, (_, i) => advice(`Item${i}`, 'RECOMMENDED'));
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
