'use strict';
const test = require('node:test');
const assert = require('node:assert');
const path = require('node:path');
const { buildPlan, parseJavaMajor } = require('../scripts/prepareBackendPlan');

test('buildPlan uses gradlew.bat on win32 and gradlew elsewhere', () => {
  assert.strictEqual(path.basename(buildPlan({ repoRoot: '/r', platform: 'win32' }).gradle.cmd), 'gradlew.bat');
  assert.strictEqual(path.basename(buildPlan({ repoRoot: '/r', platform: 'darwin' }).gradle.cmd), 'gradlew');
});

test('buildPlan stages lib and runtime under overlay/build/backend', () => {
  const p = buildPlan({ repoRoot: '/r', platform: 'linux' });
  assert.strictEqual(p.libTarget, path.join('/r', 'overlay', 'build', 'backend', 'lib'));
  assert.strictEqual(p.runtimeTarget, path.join('/r', 'overlay', 'build', 'backend', 'runtime'));
  assert.strictEqual(p.jlink.args.at(-1), p.runtimeTarget);
  assert.ok(p.jlink.args[1].split(',').includes('jdk.httpserver'));
});

test('parseJavaMajor reads the version banner', () => {
  assert.strictEqual(parseJavaMajor('openjdk version "25.0.2" 2026-01-20'), 25);
  assert.strictEqual(parseJavaMajor('nonsense'), null);
});
