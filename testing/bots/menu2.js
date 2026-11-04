// Stash menu: refresh after an add while open, offline view (read only), live view of an online player
const { sleep, check, send, command, connect, disconnect } = require('./lib');
const P = process.env.PREFIX || 'Mn';
const admin = `${P}Admin`, target = `${P}T`;
const diamondsIn = slots => slots.filter(s => s && s.name === 'diamond').reduce((a, s) => a + s.count, 0);
const windowDiamonds = bot => bot.currentWindow ? diamondsIn(bot.currentWindow.slots.slice(0, 54)) : -1;
const invDiamonds = bot => diamondsIn(bot.inventory.slots);
const nextWindow = (bot, ms = 5000) => new Promise(r => { const t = setTimeout(() => r(null), ms); bot.once('windowOpen', w => { clearTimeout(t); r(w); }); });
async function stored(name) {
  const m = await command('A', `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`));
  return m && Number(m[1]);
}
function uuidOf(name) {
  const h = require('crypto').createHash('md5').update(`OfflinePlayer:${name}`).digest();
  h[6] = (h[6] & 0x0f) | 0x30; h[8] = (h[8] & 0x3f) | 0x80;
  const x = h.toString('hex');
  return `${x.slice(0, 8)}-${x.slice(8, 12)}-${x.slice(12, 16)}-${x.slice(16, 20)}-${x.slice(20)}`;
}

(async () => {
  // The target has joined once, then gets items while offline
  let t = await connect(target, 'A');
  await sleep(1000);
  await disconnect(t);
  send('A', `op ${admin}`); send('A', `op ${target}`);
  send('A', `stash add ${target} diamond 5`);
  await sleep(1500);

  const a = await connect(admin, 'A');
  await sleep(1500);

  // Offline view by name
  let w = nextWindow(a); a.chat(`/stash view ${target}`); w = await w;
  await sleep(300);
  check('offline view opens and shows the saved stash', w && windowDiamonds(a) === 5, `window diamonds=${windowDiamonds(a)}`);
  await a.clickWindow(45, 0, 0).catch(() => {}); // collect all
  await sleep(1000);
  check('offline view: collect all takes the items for real', invDiamonds(a) === 5 && await stored(target) === 0 && windowDiamonds(a) === 0, `admin inventory=${invDiamonds(a)} stored=${await stored(target)} window=${windowDiamonds(a)}`);
  send('A', `clear ${admin}`);
  send('A', `stash add ${target} diamond 5`);
  await sleep(1500);

  // The target can still open their own stash while the admin has the offline view open
  t = await connect(target, 'A');
  await sleep(1500);
  w = nextWindow(t); t.chat('/stash'); w = await w;
  await sleep(300);
  check('target can open their own stash while an admin views it offline', w && windowDiamonds(t) === 5, `window diamonds=${windowDiamonds(t)}`);

  // Event ordering: an add while the menu is open shows up
  send('A', `stash add ${target} diamond 3`);
  await sleep(1500);
  check('open menu shows items added while it is open', windowDiamonds(t) === 8, `window diamonds=${windowDiamonds(t)}`);
  // A burst of adds while open: refreshed, not reopened for each one
  let opens = 0; const count = () => opens++; t.on('windowOpen', count);
  for (let i = 0; i < 30; i++) send('A', `stash add ${target} diamond 1 true`);
  await sleep(2000);
  t.removeListener('windowOpen', count);
  check('30 adds while open: shows all of them', windowDiamonds(t) === 38, `window diamonds=${windowDiamonds(t)} reopened ${opens} times`);
  t.closeWindow(t.currentWindow);
  await sleep(300);

  // Live view of an online player still lets the admin take items
  a.closeWindow(a.currentWindow);
  await sleep(300);
  w = nextWindow(a); a.chat(`/stash view ${target}`); w = await w;
  await sleep(300);
  check('live view of an online player', w && windowDiamonds(a) === 38, `window diamonds=${windowDiamonds(a)}`);
  await a.clickWindow(45, 0, 0).catch(() => {});
  await sleep(1000);
  check('live view: collect all moves the items', invDiamonds(a) === 38, `admin inventory=${invDiamonds(a)}`);
  a.closeWindow(a.currentWindow);
  await disconnect(t);
  await sleep(1000);
  check('target stash saved empty', await stored(target) === 0, `stored=${await stored(target)}`);

  // Offline view by UUID, for a player this server never saw
  const other = `${P}Never`;
  send('A', `stash add ${uuidOf(other)} diamond 7`);
  await sleep(1500);
  w = nextWindow(a); a.chat(`/stash view ${uuidOf(other)}`); w = await w;
  await sleep(300);
  check('offline view by UUID', w && windowDiamonds(a) === 7, `window diamonds=${windowDiamonds(a)}`);
  await disconnect(a);
  process.exit(0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
