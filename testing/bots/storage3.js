// 3.0 storage fixes: an emoji in player data saves on MySQL (utf8mb4), and an idle player isn't saved again by every
// autosave (dirty flags are cleared after a save). Needs user-auto-save-in-minutes: 1.
const { sleep, check, command, connect, disconnect, results, failures } = require('./lib');
const P = process.env.PREFIX || 'St';
const { BENCH } = require('./lib');
const fs = require('fs');
const autosaves = () => (fs.readFileSync(`${BENCH}/srv-a/console.log`, 'utf8').match(/Auto background saved user data for \d+\/\d+ online players/g) || []);

(async () => {
  const n = `${P}e`;
  const bot = await connect(n, 'A');
  await sleep(2500);
  const m = await command('A', `opbench addemoji ${n}`, new RegExp(`BENCH addemoji name=${n} result=(\\S+)`));
  check('emoji item added', m && m[1] === 'true', m && m[0]);
  await disconnect(bot);
  await sleep(3000);
  const s = await command('A', `opbench meta ${n}`, new RegExp(`BENCH meta name=${n} title=(.*)$`));
  check('emoji text loads back', s && s[1].trim() === 'Shiny \u{1F48E}', s && s[0]);
  // From the database itself, not a copy kept in memory because the save failed
  if (process.env.STORAGE !== 'yaml') {
    const row = require('child_process').execSync(`docker exec aurora-bench-mysql mysql -uroot -pbench -N aurora -e "SELECT COUNT(*) FROM aurora_user_data WHERE holder='aurora/meta' AND data LIKE '%Shiny%'"`, { stdio: ['ignore', 'pipe', 'ignore'] }).toString().trim();
    check('emoji text is in the database', row === '1', `rows=${row}`);
  }

  // Idle player: the first autosave after a change saves them, the next ones don't
  const idle = `${P}i`;
  const b2 = await connect(idle, 'A');
  await sleep(2500);
  await command('A', `opbench nowadd ${idle} 1`, new RegExp(`BENCH nowadd name=${idle}`));
  const before = autosaves().length;
  await sleep(135000);
  const lines = autosaves().slice(before);
  check('idle player saved once, not by every autosave', lines.length === 1, JSON.stringify(lines));
  await disconnect(b2);
  console.log(`${results.filter(r => r.ok).length}/${results.length} checks passed`);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
