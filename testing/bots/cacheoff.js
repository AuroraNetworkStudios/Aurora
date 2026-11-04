// With offline-player-cache off: offline players aren't changed, online ones still are
const { sleep, check, send, command, connect, disconnect } = require('./lib');
const P = process.env.PREFIX || 'Co';
async function stored(name) {
  const m = await command('A', `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`));
  return m && Number(m[1]);
}
(async () => {
  const off = `${P}off`, on = `${P}on`;
  let b = await connect(off, 'A'); await sleep(1000); await disconnect(b); await sleep(1000);
  const err = await command('A', `stash add ${off} diamond 5`, /An error occurred|Items are added/, 10000);
  await sleep(1000);
  check('offline player: stash add refused with an error', err && /error/i.test(err[0]) && await stored(off) === 0, `${err && err[0]} stored=${await stored(off)}`);
  b = await connect(on, 'A'); await sleep(1500);
  send('A', `stash add ${on} diamond 5`);
  await sleep(1000);
  await disconnect(b); await sleep(1500);
  check('online player: stash add still works and is saved on quit', await stored(on) === 5, `stored=${await stored(on)}`);
  process.exit(0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
