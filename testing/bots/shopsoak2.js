// Shop soak: players switch servers, rejoin and go offline while both servers send them deposits (applyChange),
// withdrawals (requestChange) and transfers (request, then apply). Every answer is checked against the stored data at the end.
const fs = require('fs');
const { SERVERS, sleep, check, send, command, connect, disconnect, logSize, results, failures } = require('./lib');
const mineflayer = require('mineflayer');
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
const P = process.env.PREFIX || 'H';
const PLAYERS = Number(process.env.PLAYERS || 6);
const SECONDS = Number(process.env.SECONDS || 90);
const RATE = Number(process.env.RATE || 30);
const SRV = (process.env.SERVERS || 'A,B').split(',');

(async () => {
  const names = Array.from({ length: PLAYERS }, (_, i) => `${P}p${i}`);
  const from = Object.fromEntries(SRV.map(s => [s, logSize(s)]));
  const pick = () => names[Math.floor(Math.random() * names.length)];
  const moves = { switch: 0, rejoin: 0, offline: 0, errors: 0 };
  const end = Date.now() + SECONDS * 1000;

  // Everyone starts with some diamonds so withdrawals can work
  for (const n of names) send(SRV[0], `opbench add ${n} 20`);
  await sleep(2000);

  const sender = (async () => {
    let i = 0;
    while (Date.now() < end) {
      const server = SRV[i++ % SRV.length];
      const r = Math.random();
      if (r < 0.4) send(server, `opbench add ${pick()} ${1 + Math.floor(Math.random() * 3)}`);
      else if (r < 0.8) send(server, `opbench take ${pick()} ${1 + Math.floor(Math.random() * 4)}`);
      else { const a = pick(); let b = pick(); while (b === a) b = pick(); send(server, `opbench transfer ${a} ${b} ${1 + Math.floor(Math.random() * 4)}`); }
      await sleep(1000 / RATE);
    }
  })();

  const quit = bot => new Promise(r => { if (bot.ended) return r(); bot.once('end', r); bot.quit(); setTimeout(r, 5000); });
  const proxyPlayer = async name => {
    while (Date.now() < end) {
      try {
        const bot = await joinProxy(name);
        let server = 'a';
        const switches = Math.floor(Math.random() * 4);
        for (let s = 0; s < switches && Date.now() < end; s++) {
          await sleep(500 + Math.random() * 2000);
          server = server === 'a' ? 'b' : 'a';
          bot.chat(`/server ${server}`);
          moves.switch++;
        }
        await sleep(300 + Math.random() * 1500);
        await quit(bot);
      } catch (e) { moves.errors++; console.log(`   ${e.message}`); }
      if (Math.random() < 0.4) { moves.offline++; await sleep(1000 + Math.random() * 2500); } else { moves.rejoin++; await sleep(200); }
    }
  };
  const player = async name => {
    if (process.env.PROXY) return proxyPlayer(name);
    let server = SRV[Math.floor(Math.random() * SRV.length)];
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
      else if (r < 0.5 || SRV.length === 1) { moves.rejoin++; await sleep(Math.random() * 100); }
      else { moves.switch++; server = server === 'A' ? 'B' : 'A'; await sleep(Math.random() * 100); }
    }
  };

  await Promise.all([sender, ...names.map(player)]);
  // Let the last requests finish
  await sleep(5000);

  const expected = Object.fromEntries(names.map(n => [n, 0]));
  const counts = { adds: 0, takes: 0, takesRefused: 0, transfers: 0, transfersRefused: 0, errors: 0 };
  const latencies = [];
  for (const s of SRV) {
    const text = fs.readFileSync(SERVERS[s].dir + '/console.log').subarray(from[s]).toString('utf8');
    for (const m of text.matchAll(/BENCH add name=(\S+) amount=(\d+) sent/g)) { if (m[1] in expected) { expected[m[1]] += Number(m[2]); counts.adds++; } }
    for (const m of text.matchAll(/BENCH take name=(\S+) amount=(\d+) result=(\S+) ms=(\S+)/g)) {
      if (!(m[1] in expected)) continue;
      latencies.push(Number(m[4]));
      if (m[3] === 'true') { expected[m[1]] -= Number(m[2]); counts.takes++; }
      else if (m[3] === 'false') counts.takesRefused++;
      else counts.errors++;
    }
    for (const m of text.matchAll(/BENCH transfer from=(\S+) to=(\S+) amount=(\d+) result=(\S+)/g)) {
      if (m[4] === 'true') { expected[m[1]] -= Number(m[3]); expected[m[2]] += Number(m[3]); counts.transfers++; }
      else if (m[4] === 'false') counts.transfersRefused++;
      else counts.errors++;
    }
  }
  latencies.sort((a, b) => a - b);
  const pct = p => latencies[Math.min(latencies.length - 1, Math.floor(p / 100 * latencies.length))];
  console.log(`moves: ${JSON.stringify(moves)}, ${JSON.stringify(counts)}`);
  console.log(`withdrawal answer: p50=${pct(50)}ms p95=${pct(95)}ms p99=${pct(99)}ms max=${latencies[latencies.length - 1]}ms`);
  check('every request got an answer', counts.errors === 0, JSON.stringify(counts));

  for (const n of names) {
    let value;
    for (let i = 0; i < 30; i++) {
      const m = await command(SRV[0], `opbench stored ${n}`, new RegExp(`BENCH stored name=${n} diamonds=(\\S+)`));
      value = m && Number(m[1]);
      if (value === expected[n]) break;
      await sleep(300);
    }
    check(`${n}: stored matches the answers (${expected[n]})`, value === expected[n], `stored=${value}`);
  }
  const p = await command(SRV[0], 'opbench pending', /BENCH pending users=(\d+) flags=(\S+)/);
  check('no pending changes and no leftover sync flags', p && p[1] === '0' && (p[2] === '0' || p[2] === 'n/a'), p && p[0]);
  for (const s of SRV) {
    const text = fs.readFileSync(SERVERS[s].dir + '/console.log').subarray(from[s]).toString('utf8');
    const late = (text.match(/gave up waiting/g) || []).length;
    const errors = text.split('\n').filter(l => /ERROR|SEVERE|Exception/.test(l) && !/StacklessClosed/.test(l));
    check(`server ${s}: no errors and no answers after giving up`, late === 0 && errors.length === 0, `${late} late, ${errors[0] || ''}`);
  }
  console.log(`${results.filter(r => r.ok).length}/${results.length} checks passed`);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
