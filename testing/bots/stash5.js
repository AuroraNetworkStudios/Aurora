// An admin takes 5 diamonds from an offline player's stash through the offline view, once and only once.
// Counts what the admin really has on the server (clear ... 0), not just what the bot thinks it has.
const { sleep, send, command, connect, disconnect, check } = require('./lib');
const P = process.env.PREFIX || 'St';
const t = `${P}t`, admin = `${P}a`;
const diamondsIn = slots => slots.filter(s => s && s.name === 'diamond').reduce((a, s) => a + s.count, 0);
const nextWindow = (bot, ms = 5000) => new Promise(r => { const tm = setTimeout(() => r(null), ms); bot.once('windowOpen', w => { clearTimeout(tm); r(w); }); });
async function serverCount(name, item) {
  const m = await command('A', `clear ${name} ${item} 0`, new RegExp(`(Found (\\d+) matching item|No items were found on player ${name})`));
  return m ? (m[2] !== undefined ? Number(m[2]) : 0) : 'TIMEOUT';
}
async function stored(name) {
  const m = await command('A', `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`));
  return m && m[1];
}
(async () => {
  let tb = await connect(t, 'A'); await sleep(1000); await disconnect(tb);
  send('A', `stash add ${t} diamond 5`);
  await sleep(1500);
  const a = await connect(admin, 'A');
  send('A', `op ${admin}`); await sleep(1500);
  let opened = 0; a.on('windowOpen', () => opened++);
  let w = nextWindow(a); a.chat(`/stash view ${t}`); w = await w; await sleep(300);
  const slot = a.currentWindow.slots.findIndex((s, i) => i < 54 && s && s.name === 'diamond');
  await a.clickWindow(slot, 0, 0).catch(() => {});
  await sleep(1500);
  const client = diamondsIn(a.inventory.slots);
  const cursor = a.inventory.selectedItem ? `${a.inventory.selectedItem.name}x${a.inventory.selectedItem.count}` : 'empty';
  const server = await serverCount(admin, 'diamond'), left = await stored(t);
  console.log(`RESULT client=${client} server=${server} stored=${left} cursor=${cursor}`);
  check('the admin got the 5 diamonds and the stash is empty', server === 5 && left === '0', `server=${server} stored=${left}`);
  a.closeWindow(a.currentWindow);
  await sleep(1500);
  const after = await serverCount(admin, 'diamond');
  check('still 5 after closing the menu', after === 5, `server=${after} windows-opened=${opened}`);
  await disconnect(a);
  process.exit(0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
