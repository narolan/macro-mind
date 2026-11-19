#!/usr/bin/env node
'use strict';
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const { buildPlan, parseJavaMajor, REQUIRED_JAVA_MAJOR } = require('./prepareBackendPlan');

function fail(msg) {
  console.error(`prepare-backend: ${msg}`);
  process.exit(1);
}

function run(cmd, args, opts = {}) {
  const r = spawnSync(cmd, args, { stdio: 'inherit', shell: process.platform === 'win32' && cmd.endsWith('.bat'), ...opts });
  if (r.error || r.status !== 0) fail(`${cmd} ${args.join(' ')} failed${r.error ? `: ${r.error.message}` : ` (exit ${r.status})`}`);
}

const plan = buildPlan({ repoRoot: path.resolve(__dirname, '..', '..'), platform: process.platform });

const java = spawnSync('java', ['-version'], { encoding: 'utf8' });
if (java.error) fail('java not found on PATH; install JDK 25 (it must include jlink).');
const major = parseJavaMajor(`${java.stderr}${java.stdout}`);
if (major !== REQUIRED_JAVA_MAJOR) fail(`JDK ${REQUIRED_JAVA_MAJOR} required, found ${major ?? 'unknown'}.`);
if (spawnSync('jlink', ['--version']).error) fail('jlink not found on PATH; install a full JDK 25, not a JRE.');

fs.rmSync(plan.staging, { recursive: true, force: true });
fs.mkdirSync(plan.staging, { recursive: true });

run(plan.gradle.cmd, plan.gradle.args, { cwd: plan.gradle.cwd });
if (!fs.existsSync(plan.libSource)) fail(`expected ${plan.libSource} after installDist`);
fs.cpSync(plan.libSource, plan.libTarget, { recursive: true });
run(plan.jlink.cmd, plan.jlink.args);
console.log(`prepare-backend: staged ${plan.staging}`);
