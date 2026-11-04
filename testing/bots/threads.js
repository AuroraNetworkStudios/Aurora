// Aurora calls plugins on the thread that owns the player (required on Folia, harmless on Paper):
// the user loaded event, rewards (one failing reward doesn't stop the rest), and kicks during /aurora dbmigrate.
// Starts on YAML storage, dbmigrate moves it to MySQL.
const fs = require('fs');
const { sleep, check, send, command, connect, waitLog, logSize, results, failures } = require('./lib');
const P = process.env.PREFIX || 'Th';
const { BENCH } = require('./lib');
const consoleText = () => fs.readFileSync(`${BENCH}/srv-a/console.log`, 'utf8');

(async () => {
  const n = `${P}a`;
  const from = logSize('A');
  const bot = await connect(n, 'A');
  const loaded = await waitLog('A', new RegExp(`BENCH loadedevent name=${n} owned=(\\S+)`), from, 15000);
  check('user loaded event runs on the player\'s thread', loaded && loaded[1] === 'true', loaded && loaded[0]);

  const r = logSize('A');
  send('A', `opbench rewards ${n}`);
  const owned = await waitLog('A', new RegExp(`BENCH reward name=${n} owned=(\\S+)`), r, 15000);
  const done = await waitLog('A', new RegExp(`BENCH rewards done name=${n} error=(\\S+)`), r, 15000);
  check('rewards run on the player\'s thread, after one that failed', owned && owned[1] === 'true', owned && owned[0]);
  check('rewards finish even though one failed', done && done[1] === 'null', done && done[0]);

  let kicked = null;
  bot.once('kicked', reason => { kicked = JSON.stringify(reason); });
  bot.once('end', () => { if (!kicked) kicked = 'ended'; });
  send('A', 'aurora dbmigrate 2');
  for (let i = 0; i < 100 && !kicked; i++) await sleep(200);
  check('dbmigrate kicks online players', !!kicked, kicked);
  await sleep(3000);

  const text = consoleText();
  const threadErrors = (text.match(/Thread failed main thread check|not owned by the current region|Accessing entity state off owning region|IllegalStateException: .*(thread|region)/gi) || []);
  check('no thread errors in the console', threadErrors.length === 0, threadErrors.slice(0, 3).join(' | '));
  console.log(`${results.filter(x => x.ok).length}/${results.length} checks passed`);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
