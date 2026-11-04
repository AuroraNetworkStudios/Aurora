// A player quits right as the server stops: their data is saved and their sync flag released
const { execSync } = require('child_process');
const { sleep, check, send, command, connect, results, failures } = require('./lib');
const P = process.env.PREFIX || 'Qs';
const { BENCH } = require('./lib');
const sql = q => execSync(`docker exec aurora-bench-mysql mysql -uroot -pbench -N aurora -e "${q}"`, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
const restartA = () => execSync(`for i in $(seq 60); do pgrep -f "[b]ench.role=a" >/dev/null || break; perl -e 'select(undef,undef,undef,0.2)'; done; "${BENCH}/start.sh" "${BENCH}/srv-a" a >/dev/null 2>&1; for i in $(seq 120); do grep -q "Done (" "${BENCH}/srv-a/console.log" && break; perl -e 'select(undef,undef,undef,0.5)'; done`, { stdio: 'ignore', shell: '/bin/sh' });
async function stored(name) {
  const m = await command('A', `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`));
  return m ? Number(m[1]) : 'TIMEOUT';
}
async function loaded(name) {
  const m = await command('A', `opbench inspect ${name}`, new RegExp(`BENCH inspect name=${name} (.*)$`));
  return m && /loaded=true/.test(m[1]);
}

(async () => {
  for (const delay of [0, 5, 15, 30, 60, 120]) {
    const n = `${P}${delay}`;
    const bot = await connect(n, 'A');
    for (let i = 0; i < 50 && !(await loaded(n)); i++) await sleep(100);
    send('A', `opbench add ${n} 5`);
    await sleep(500);
    bot.quit();
    await sleep(delay);
    send('A', 'stop');
    restartA();
    const v = await stored(n);
    const flags = sql('SELECT COUNT(*) FROM aurora_sync');
    check(`quit ${delay}ms before stop: saved and released`, v === 5 && flags === '0', `stored=${v} flags=${flags}`);
  }
  console.log(`${results.filter(r => r.ok).length}/${results.length} checks passed`);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
