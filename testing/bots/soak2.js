// Soak: several players switch servers, rejoin and go offline while both servers keep sending them changes
const { sleep, check, send, command, connect, disconnect, results, failures } = require('./lib');
const P = process.env.PREFIX || 'S';
const PLAYERS = Number(process.env.PLAYERS || 6);
const SECONDS = Number(process.env.SECONDS || 90);
const RATE = Number(process.env.RATE || 25); // changes per second, all players together
const SERVERS = (process.env.SERVERS || 'A,B').split(',');

async function stored(server, name) {
  const m = await command(server, `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`));
  return m ? (m[1] === 'FAILED' ? 'FAILED' : Number(m[1])) : 'TIMEOUT';
}

(async () => {
  const names = Array.from({ length: PLAYERS }, (_, i) => `${P}p${i}`);
  const sent = Object.fromEntries(names.map(n => [n, 0]));
  const moves = { switch: 0, rejoin: 0, offline: 0, errors: 0 };
  const end = Date.now() + SECONDS * 1000;

  const sender = (async () => {
    let i = 0;
    while (Date.now() < end) {
      const n = names[Math.floor(Math.random() * names.length)];
      const amount = 1 + Math.floor(Math.random() * 3);
      send(SERVERS[i++ % SERVERS.length], `opbench add ${n} ${amount}`);
      sent[n] += amount;
      await sleep(1000 / RATE);
    }
  })();

  const player = async name => {
    let server = SERVERS[Math.floor(Math.random() * SERVERS.length)];
    while (Date.now() < end) {
      try {
        const bot = await connect(name, server);
        await sleep(200 + Math.random() * 3000);
        await disconnect(bot);
      } catch (e) {
        moves.errors++;
        console.log(`   ${e.message}`);
      }
      const r = Math.random();
      if (r < 0.25) { moves.offline++; await sleep(1000 + Math.random() * 3000); }
      else if (r < 0.5 || SERVERS.length === 1) { moves.rejoin++; await sleep(Math.random() * 100); }
      else { moves.switch++; server = server === 'A' ? 'B' : 'A'; await sleep(Math.random() * 100); }
    }
  };

  await Promise.all([sender, ...names.map(player)]);
  const total = Object.values(sent).reduce((a, b) => a + b, 0);
  console.log(`sent ${total} diamonds in changes, moves: ${JSON.stringify(moves)}`);

  await sleep(3000);
  for (const n of names) {
    let value;
    for (let i = 0; i < 50; i++) {
      value = await stored(SERVERS[0], n);
      if (value === sent[n]) break;
      await sleep(200);
    }
    check(`${n}: stored ${sent[n]}`, value === sent[n], `stored=${value}`);
  }
  const p = await command(SERVERS[0], 'opbench pending', /BENCH pending users=(\d+) flags=(\S+)/);
  check('no pending changes and no leftover sync flags', p && p[1] === '0' && (p[2] === '0' || p[2] === 'n/a'), p && p[0]);
  console.log(`${results.filter(r => r.ok).length}/${results.length} checks passed`);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
