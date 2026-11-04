// Saves that hang (a row lock held from another MySQL connection) must not hold up edits and reads, and writes of the
// same player must land in order. MySQL only. Also checks synchronous edits of offline players (load once, then instant).
const { execSync, spawn } = require('child_process');
const crypto = require('crypto');
const { sleep, check, send, command, connect, disconnect, results, failures } = require('./lib');
const P = process.env.PREFIX || 'Ss';
const { BENCH } = require('./lib');
const MYSQL = process.env.STORAGE !== 'yaml';
const sql = q => execSync(`docker exec aurora-bench-mysql mysql -uroot -pbench -N aurora -e "${q}"`, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
const restartA = () => execSync(`for i in $(seq 60); do pgrep -f "[b]ench.role=a" >/dev/null || break; perl -e 'select(undef,undef,undef,0.2)'; done; "${BENCH}/start.sh" "${BENCH}/srv-a" a >/dev/null 2>&1; for i in $(seq 120); do grep -q "Done (" "${BENCH}/srv-a/console.log" && break; perl -e 'select(undef,undef,undef,0.5)'; done`, { stdio: 'ignore', shell: '/bin/sh' });

function offlineUuid(name) {
  const h = crypto.createHash('md5').update('OfflinePlayer:' + name).digest();
  h[6] = (h[6] & 0x0f) | 0x30; h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}
// Holds the player's rows locked for the given seconds, so saving them waits that long
function lockRows(name, seconds) {
  const p = spawn('docker', ['exec', 'aurora-bench-mysql', 'mysql', '-uroot', '-pbench', 'aurora', '-e',
    `BEGIN; SELECT COUNT(*) FROM aurora_user_data WHERE player_uuid='${offlineUuid(name)}' FOR UPDATE; DO SLEEP(${seconds}); COMMIT;`], { stdio: 'ignore' });
  return new Promise(r => p.on('exit', r));
}
async function stored(name) {
  const m = await command('A', `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`));
  return m ? Number(m[1]) : 'TIMEOUT';
}
async function inspect(name) {
  const m = await command('A', `opbench inspect ${name}`, new RegExp(`BENCH inspect name=${name} (.*)$`));
  const d = m && m[1].match(/loaded=true diamonds=(\d+)/);
  return d ? Number(d[1]) : null;
}
async function now(op, name, n) {
  const m = await command('A', `opbench ${op} ${name} ${n}`, new RegExp(`BENCH ${op} name=${name} ok=(\\d+) of=\\d+ first=(\\S+)ms rest-max=(\\S+)ms max=(\\S+)ms`), 30000);
  return m ? { ok: Number(m[1]), first: parseFloat(m[2]), rest: parseFloat(m[3]), max: parseFloat(m[4]) } : { ok: -1, first: NaN, rest: NaN, max: NaN };
}
async function nowget(name) {
  const m = await command('A', `opbench nowget ${name}`, new RegExp(`BENCH nowget name=${name} diamonds=(\\S+) ms=(\\S+)`), 30000);
  return m ? { diamonds: m[1] === 'NULL' ? null : Number(m[1]), ms: parseFloat(m[2]) } : { diamonds: 'TIMEOUT', ms: NaN };
}
async function join(name) {
  const bot = await connect(name, 'A');
  for (let i = 0; i < 100 && (await inspect(name)) === null; i++) await sleep(100);
  return bot;
}
const fast = ms => ms < 50;

(async () => {
  // Synchronous edits of an offline player: loaded on the first call only, instant after
  {
    const n = `${P}a`;
    await disconnect(await join(n)); await sleep(2500);
    send('A', `stash add ${n} diamond 3`); await sleep(1500);
    const add = await now('nowadd', n, 200);
    const take = await now('nowtake', n, 50);
    const get = await nowget(n);
    check('offline: 200 sync adds and 50 takes all made', add.ok === 200 && take.ok === 50, `adds=${add.ok} takes=${take.ok}`);
    check('offline: calls after the first are instant', fast(add.rest) && fast(take.max) && fast(get.ms), `first=${add.first}ms rest-max=${add.rest}ms take-max=${take.max}ms get=${get.ms}ms`);
    check('offline: read sees the edits', get.diamonds === 153, `diamonds=${get.diamonds}`);
    const refused = await now('nowtake', n, 200);
    check('offline: takes past zero are refused', refused.ok === 153, `ok=${refused.ok}`);
  }
  // Rejoining right after quitting, while the quit is being saved: never back to older data
  {
    const n = `${P}r`;
    let wrong = [];
    for (let i = 1; i <= 15; i++) {
      const bot = await join(n);
      const before = await inspect(n);
      if (before !== i - 1) wrong.push(`join ${i}: ${before}`);
      send('A', `opbench nowadd ${n} 1`);
      for (let j = 0; j < 20 && (await inspect(n)) !== i; j++) await sleep(50);
      bot.quit();
      await sleep(i * 7 % 60);
    }
    await sleep(3000);
    check('quick rejoins: every session started from the newest data', wrong.length === 0, wrong.join(', '));
    check('quick rejoins: all edits saved', await stored(n) === 15, `stored=${await stored(n)}`);
  }
  if (MYSQL) {
    // A session save that hangs doesn't hold up edits, and a later save lands after it
    {
      const n = `${P}b`;
      const bot = await join(n);
      send('A', `opbench add ${n} 5`); await sleep(500);
      send('A', `opbench save ${n}`); await sleep(1000);
      const lock = lockRows(n, 6); await sleep(700);
      send('A', `opbench save ${n}`); await sleep(500);
      const add = await now('nowadd', n, 20);
      check('session save hanging: sync edits are instant', add.ok === 20 && fast(add.max), `ok=${add.ok} max=${add.max}ms`);
      const m = await command('A', `opbench save ${n}`, new RegExp(`BENCH save name=${n} result=(\\S+) ms=(\\S+)`), 20000);
      await lock;
      check('the second save waited for the first', m && m[1] === 'true' && parseFloat(m[2]) > 2000, m ? `${m[1]} ${m[2]}ms` : 'no answer');
      await sleep(500);
      check('storage has the newest copy', await stored(n) === 25, `stored=${await stored(n)}`);
      await disconnect(bot);
    }
    // A quit save that hangs: edits go to their offline copy right away, and are saved after it
    {
      const n = `${P}c`;
      const bot = await join(n);
      send('A', `opbench add ${n} 5`); await sleep(500);
      send('A', `opbench save ${n}`); await sleep(1000);
      const lock = lockRows(n, 6); await sleep(700);
      await disconnect(bot); await sleep(1500);
      const add = await now('nowadd', n, 3);
      const get = await nowget(n);
      check('quit save hanging: sync edits of the copy are instant', add.ok === 3 && fast(add.max) && fast(get.ms), `ok=${add.ok} max=${add.max}ms get=${get.ms}ms`);
      check('quit save hanging: reads see the edits', get.diamonds === 8, `diamonds=${get.diamonds}`);
      await lock; await sleep(1500);
      check('quit saved and its sync flag released', sql(`SELECT COUNT(*) FROM aurora_sync WHERE player_uuid='${offlineUuid(n)}'`) === '0');
      const after = await nowget(n);
      check('copy edited after the quit was taken stays in memory', after.diamonds === 8, `diamonds=${after.diamonds}`);
      send('A', 'stop'); restartA();
      check('edits after the quit survive a restart', await stored(n) === 8, `stored=${await stored(n)}`);
    }
    // Rejoining while their quit save hangs: the session goes on with the copy and its edits
    {
      const n = `${P}d`;
      let bot = await join(n);
      send('A', `opbench add ${n} 5`); await sleep(500);
      send('A', `opbench save ${n}`); await sleep(1000);
      const lock = lockRows(n, 6); await sleep(700);
      await disconnect(bot); await sleep(1500);
      const add = await now('nowadd', n, 2);
      bot = await join(n);
      const online = await inspect(n);
      check('rejoin during a hanging quit save: session has the edits', add.ok === 2 && online === 7, `ok=${add.ok} session=${online}`);
      await lock; await sleep(1000);
      send('A', `opbench add ${n} 1`); await sleep(500);
      await disconnect(bot); await sleep(3000);
      check('rejoin during a hanging quit save: saved right after', await stored(n) === 8, `stored=${await stored(n)}`);
    }
    // A clean quit copy is dropped once saved (only edited ones stay in memory)
    {
      const n = `${P}e`;
      const bot = await join(n);
      send('A', `opbench add ${n} 4`); await sleep(500);
      await disconnect(bot); await sleep(2500);
      const m = await command('A', `opbench inspect ${n}`, new RegExp(`BENCH inspect name=${n} (.*)$`));
      check('quit: saved and released', await stored(n) === 4 && sql(`SELECT COUNT(*) FROM aurora_sync WHERE player_uuid='${offlineUuid(n)}'`) === '0', `stored=${await stored(n)}`);
      check('quit: nothing left in the session cache', m && !/loaded=/.test(m[1]), m && m[1]);
    }
  }
  console.log(`${results.filter(r => r.ok).length}/${results.length} checks passed`);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
