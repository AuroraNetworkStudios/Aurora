// Two servers sharing MySQL behind Velocity, offline-player-cache off (the 2.6 join/quit handover): players switch
// servers, quit and come back while getting edits on the server they are on. Every edit that was made must be in the
// storage at the end, exactly once, and edits on the server they aren't on are refused.
const mineflayer = require('mineflayer');
const { sleep, check, command, results, failures } = require('./lib');
const P = process.env.PREFIX || 'H';
const PLAYERS = Number(process.env.PLAYERS || 6), SECONDS = Number(process.env.SECONDS || 90);

function joinProxy(name) {
  return new Promise((resolve, reject) => {
    const bot = mineflayer.createBot({ host: '127.0.0.1', port: 25600, username: name, version: '26.1', auth: 'offline', hideErrors: true, physicsEnabled: false });
    require('./lib').stayStill(bot);
    const timer = setTimeout(() => { bot.end(); reject(new Error(`${name} proxy join timeout`)); }, 30000);
    bot.once('spawn', () => { clearTimeout(timer); resolve(bot); });
    bot.once('kicked', r => { clearTimeout(timer); reject(new Error(`${name} kicked: ${JSON.stringify(r)}`)); });
    bot.on('error', () => {});
  });
}
const quit = bot => new Promise(r => { if (bot.ended) return r(); bot.once('end', r); bot.quit(); setTimeout(r, 5000); });
async function nowadd(server, name) {
  const m = await command(server, `opbench nowadd ${name} 1`, new RegExp(`BENCH nowadd name=${name} ok=(\\d+)`), 15000);
  return m ? Number(m[1]) : null;
}

(async () => {
  const names = Array.from({ length: PLAYERS }, (_, i) => `${P}p${i}`);
  const made = Object.fromEntries(names.map(n => [n, 0]));
  const stats = { switch: 0, reconnect: 0, offline: 0, errors: 0, made: 0, refusedHere: 0, madeElsewhere: 0, noAnswer: 0 };
  const end = Date.now() + SECONDS * 1000;
  const player = async name => {
    while (Date.now() < end) {
      try {
        const bot = await joinProxy(name);
        let server = 'A';
        const switches = Math.floor(Math.random() * 4);
        for (let s = 0; s <= switches && Date.now() < end; s++) {
          if (s > 0) {
            server = server === 'A' ? 'B' : 'A';
            bot.chat(`/server ${server.toLowerCase()}`);
            stats.switch++;
          }
          const edits = 1 + Math.floor(Math.random() * 4);
          for (let e = 0; e < edits; e++) {
            await sleep(150 + Math.random() * 500);
            const other = server === 'A' ? 'B' : 'A';
            const [here, there] = await Promise.all([nowadd(server, name), nowadd(other, name)]);
            if (here === null || there === null) stats.noAnswer++;
            made[name] += (here || 0) + (there || 0);
            stats.made += here || 0;
            if (here === 0) stats.refusedHere++;
            stats.madeElsewhere += there || 0;
          }
        }
        await quit(bot);
      } catch (e) { stats.errors++; console.log(`   ${e.message}`); }
      if (Math.random() < 0.4) { stats.offline++; await sleep(500 + Math.random() * 2500); } else { stats.reconnect++; await sleep(100); }
    }
  };
  await Promise.all(names.map(player));
  console.log(`stats: ${JSON.stringify(stats)}`);
  await sleep(4000);
  for (const n of names) {
    const m = await command('A', `opbench stored ${n}`, new RegExp(`BENCH stored name=${n} diamonds=(\\S+)`));
    const value = m && Number(m[1]);
    check(`${n}: every edit made is stored once`, value === made[n], `stored=${value} made=${made[n]}`);
  }
  check('edits on the other server were refused (offline-player-cache off)', stats.madeElsewhere === 0, `made elsewhere=${stats.madeElsewhere}`);
  const p = await command('A', 'opbench pending', /BENCH pending users=(\d+) flags=(\S+)/);
  check('no leftover sync flags', p && p[2] === '0', p && p[0]);
  console.log(`${results.filter(r => r.ok).length}/${results.length} checks passed`);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
