// Offline edits are saved by the autosave: edit, wait past one autosave (1 minute in the bench config), crash, check
const { execSync } = require('child_process');
const { sleep, check, send, command, connect, disconnect } = require('./lib');
const P = process.env.PREFIX || 'As';
const { BENCH } = require('./lib');
async function stored(name) {
  const m = await command('A', `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`), 20000);
  return m && Number(m[1]);
}
const startA = () => execSync(`"${BENCH}/start.sh" "${BENCH}/srv-a" a; for i in $(seq 120); do grep -q "Done (" "${BENCH}/srv-a/console.log" && break; sleep 1; done`, { shell: '/bin/sh', stdio: 'ignore' });
(async () => {
  const name = `${P}x`;
  const b = await connect(name, 'A'); await sleep(1000); await disconnect(b); await sleep(1500);
  send('A', `stash add ${name} diamond 7`);
  await sleep(2000);
  check('offline edit is in the cached copy', await stored(name) === 7, `stored=${await stored(name)}`);
  await sleep(75000);
  execSync(`pkill -9 -f "[b]ench.role=a"; pkill -f "[t]ail -n0 -f cmds"; sleep 2`, { shell: '/bin/sh', stdio: 'ignore' });
  startA();
  check('saved by the autosave, survives a crash', await stored(name) === 7, `stored=${await stored(name)}`);
  process.exit(0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
