'use strict';
const path = require('node:path');

// jdeps reports these for the backend; TLS (ddragon) is exercised in the staged runtime check.
const MODULES = ['java.base', 'java.net.http', 'jdk.httpserver', 'java.logging', 'java.desktop', 'java.sql'];
const REQUIRED_JAVA_MAJOR = 25;

function parseJavaMajor(versionOutput) {
  const m = /version "(\d+)/.exec(versionOutput);
  return m ? Number(m[1]) : null;
}

function buildPlan({ repoRoot, platform }) {
  const win = platform === 'win32';
  const staging = path.join(repoRoot, 'overlay', 'build', 'backend');
  return {
    staging,
    gradle: { cmd: path.join(repoRoot, win ? 'gradlew.bat' : 'gradlew'), args: ['installDist'], cwd: repoRoot },
    libSource: path.join(repoRoot, 'build', 'install', 'MacroMind', 'lib'),
    libTarget: path.join(staging, 'lib'),
    runtimeTarget: path.join(staging, 'runtime'),
    jlink: {
      cmd: 'jlink',
      args: ['--add-modules', MODULES.join(','), '--strip-debug', '--no-header-files', '--no-man-pages',
        '--compress', 'zip-6', '--output', path.join(staging, 'runtime')],
    },
  };
}

module.exports = { buildPlan, parseJavaMajor, MODULES, REQUIRED_JAVA_MAJOR };
