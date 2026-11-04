// Pending changes on YAML storage: one server (A)
const { execSync } = require('child_process');
const { sleep, check, send, command, connect, disconnect, results, failures } = require('./lib');
const P = process.env.PREFIX || 'Y';
const { BENCH } = require('./lib');

async function inspect(name) {
  const m = await command('A', `opbench inspect ${name}`, new RegExp(`BENCH inspect name=${name} (.*)$`));
  const get = k => (m[1].match(new RegExp(`${k}=(\\S+)`)) || [])[1];
  return { loaded: get('loaded') === 'true', diamonds: get('diamonds') !== undefined ? Number(get('diamonds')) : null, raw: m[1] };
}
async function stored(name) {
  const m = await command('A', `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`));
  return m ? (m[1] === 'FAILED' ? 'FAILED' : Number(m[1])) : 'TIMEOUT';
}
async function until(fn, predicate, timeoutMs = 10000) {
  const start = Date.now(); let value;
  while (Date.now() - start < timeoutMs) { value = await fn(); if (predicate(value)) return [value, Date.now() - start]; await sleep(100); }
  return [value, Date.now() - start];
}
function restartA() {
  execSync(`for i in $(seq 60); do pgrep -f "[b]ench.role=a" >/dev/null || break; perl -e 'select(undef,undef,undef,0.5)'; done; "${BENCH}/start.sh" "${BENCH}/srv-a" a; for i in $(seq 120); do grep -q "Done (" "${BENCH}/srv-a/console.log" && break; perl -e 'select(undef,undef,undef,0.5)'; done`, { stdio: 'ignore', shell: '/bin/sh' });
}

(async () => {
  {
    send('A', `opbench add ${P}1 5`);
    const [v, ms] = await until(() => stored(`${P}1`), v => v === 5);
    check('Y1 offline change written to the file', v === 5, `stored=${v} after ${ms}ms`);
  }
  {
    const bot = await connect(`${P}2`, 'A');
    await until(() => inspect(`${P}2`), s => s.loaded);
    send('A', `opbench add ${P}2 4`);
    const [s, ms] = await until(() => inspect(`${P}2`), s => s.diamonds === 4, 3000);
    check('Y2 online change applied right away', s.diamonds === 4, `${s.raw} after ${ms}ms`);
    await disconnect(bot);
    const [v] = await until(() => stored(`${P}2`), v => v === 4);
    check('Y2 saved on quit', v === 4, `stored=${v}`);
  }
  {
    const bot = await connect(`${P}3`, 'A');
    await until(() => inspect(`${P}3`), s => s.loaded);
    for (let i = 0; i < 40; i++) { send('A', `opbench add ${P}3 1`); if (i === 20) disconnect(bot); await sleep(10); }
    const [v, ms] = await until(() => stored(`${P}3`), v => v === 40);
    check('Y3 changes during quit: none lost', v === 40, `stored=${v} after ${ms}ms`);
  }
  {
    // Quick rejoins to the same server while changes keep coming
    let sent = 0;
    for (let r = 0; r < 15; r++) {
      const bot = await connect(`${P}4`, 'A');
      for (let i = 0; i < 5; i++) { send('A', `opbench add ${P}4 1`); sent++; await sleep(20 + Math.random() * 80); }
      await disconnect(bot);
      send('A', `opbench add ${P}4 1`); sent++;
      await sleep(Math.random() * 60);
    }
    const [v] = await until(() => stored(`${P}4`), v => v === sent);
    check(`Y4 15 quick rejoins with ${sent} changes: none lost`, v === sent, `stored=${v}`);
  }
  {
    const m = await command('A', `opbench burst 250 ${P}5x`, /BENCH burst count=250 same=false (.*)$/, 60000);
    console.log(`   ${m && m[1]}`);
    // The burst can report done while there happen to be no change rows for a moment, give the rest time to land
    await sleep(2000);
    const v = await command('A', `opbench verify 250 ${P}5x 1`, /BENCH verify prefix=\S+ ok=(\d+) wrong=(\d+)/, 60000);
    check('Y5 250 offline players at once', v && v[1] === '250', v && v[0]);
  }
  {
    // Stop right after changes: offline ones are only in memory until applied
    const bot = await connect(`${P}6`, 'A');
    await until(() => inspect(`${P}6`), s => s.loaded);
    for (let i = 0; i < 20; i++) { send('A', `opbench add ${P}6 1`); send('A', `opbench add ${P}7 1`); }
    send('A', 'stop');
    restartA();
    await sleep(1000);
    check('Y6 stop right after changes to an online player: kept', await stored(`${P}6`) === 20);
    check('Y6 stop right after changes to an offline player: kept', await stored(`${P}7`) === 20);
  }
  console.log(`${results.filter(r => r.ok).length}/${results.length} checks passed`);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
