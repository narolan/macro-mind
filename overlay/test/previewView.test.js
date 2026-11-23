'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');
const { PREVIEW_VIEW } = require('../src/previewView');

test('previewShowsThreeRowsOfDifferentLevels', () => {
  assert.equal(PREVIEW_VIEW.visible, true);
  assert.deepEqual(PREVIEW_VIEW.rows.map((r) => r.level), ['critical', 'recommended', 'situational']);
});
