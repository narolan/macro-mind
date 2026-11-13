const test = require('node:test');
const assert = require('node:assert/strict');
const { toViewModel, viewKey, rowKey } = require('../src/viewModel');

const advice = (item, extra = {}) => ({ item, reasoning: `why ${item}`, priority: 'CRITICAL', category: 'DEFENSIVE', confidence: 'EXACT', target: null, ...extra });
const view = (adviceList, iconVersion = '16.22.1') => toViewModel({ visible: true, iconVersion, advice: adviceList });

test('every row carries a key equal to rowKey of its own content, so the renderer needs no second copy of the formula', () => {
  for (const row of view([advice('A', { itemId: 1 }), advice('B', { reasoning: 'other', itemId: 2 })]).rows) {
    const { key, ...content } = row;
    assert.equal(key, rowKey(content));
  }
});

test('row keys are different for different rows and equal for identical ones', () => {
  const first = view([advice('A'), advice('B', { reasoning: 'other' })]).rows;
  const again = view([advice('A'), advice('B', { reasoning: 'other' })]).rows;
  assert.notEqual(first[0].key, first[1].key);
  assert.deepEqual(first.map((r) => r.key), again.map((r) => r.key));
});

test('viewKey changes when the estimated flag, the target or the icon changes', () => {
  const base = viewKey(view([advice('A', { itemId: 1 })]));
  assert.notEqual(base, viewKey(view([advice('A', { itemId: 1, confidence: 'ESTIMATED' })])));
  assert.notEqual(base, viewKey(view([advice('A', { itemId: 1, target: 'Zed' })])));
  assert.notEqual(base, viewKey(view([advice('A', { itemId: 1 })], '16.23.1')));
});

test('a hidden view has an empty key however it became hidden', () => {
  assert.equal(viewKey(toViewModel(null)), '');
  assert.equal(viewKey(toViewModel({ visible: false, advice: [] })), '');
});

test('the view model is not changed by reading its key', () => {
  const vm = view([advice('A')]);
  const before = JSON.stringify(vm);
  viewKey(vm);
  assert.equal(JSON.stringify(vm), before);
});
