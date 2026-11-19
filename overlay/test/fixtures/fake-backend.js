// A stand-in for the Java backend, so the launcher can be tested without Java.
const mode = process.argv[2];
if (mode === 'chatty') console.log('hello from backend');
if (mode === 'die-always') setTimeout(() => process.exit(1), 30);
if (mode === 'die-once') {
  const fs = require('node:fs');
  const os = require('node:os');
  const path = require('node:path');
  const marker = path.join(os.tmpdir(), `mm-fake-${process.ppid}`);
  if (!fs.existsSync(marker)) { fs.writeFileSync(marker, '1'); setTimeout(() => process.exit(1), 30); }
}
setInterval(() => {}, 1000); // stay alive like a server
process.on('SIGTERM', () => process.exit(0));
