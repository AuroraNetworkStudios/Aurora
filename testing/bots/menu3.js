// Own stash menu: take items (refresh), close, reopen; admin live view closes the target's own menu
const { sleep, check, send, connect, disconnect } = require('./lib');
const P = process.env.PREFIX || 'Mr';
const diamondsIn = slots => slots.filter(s => s && s.name === 'diamond').reduce((a, s) => a + s.count, 0);
const windowDiamonds = bot => bot.currentWindow ? diamondsIn(bot.currentWindow.slots.slice(0, 54)) : -1;
const nextWindow = (bot, ms = 4000) => new Promise(r => { const t = setTimeout(() => r(null), ms); bot.once('windowOpen', w => { clearTimeout(t); r(w); }); });
(async () => {
  const t = await connect(`${P}T`, 'A'), a = await connect(`${P}Admin`, 'A');
  send('A', `op ${P}T`); send('A', `op ${P}Admin`);
  await sleep(1500);
  send('A', `stash add ${P}T diamond 70`);
  await sleep(1000);
  let w = nextWindow(t); t.chat('/stash'); w = await w; await sleep(300);
  const slot = t.currentWindow.slots.findIndex((s, i) => i < 54 && s && s.name === 'diamond');
  await t.clickWindow(slot, 0, 0).catch(() => {});
  await sleep(800);
  check('taking an item refreshes the menu', windowDiamonds(t) === 6 || windowDiamonds(t) === 64, `window=${windowDiamonds(t)}`);
  t.closeWindow(t.currentWindow); await sleep(500);
  w = nextWindow(t); t.chat('/stash'); w = await w; await sleep(300);
  check('reopen after a refresh and a close works', !!w, `window=${windowDiamonds(t)}`);
  // Admin opens the live view: the target's own menu is closed, one menu per stash
  let closed = false; t.once('windowClose', () => closed = true);
  w = nextWindow(a); a.chat(`/stash view ${P}T`); w = await w; await sleep(500);
  check('admin live view closes the target own menu', !!w && closed, `admin window=${!!w} target closed=${closed}`);
  a.closeWindow(a.currentWindow); await sleep(500);
  w = nextWindow(t); t.chat('/stash'); w = await w;
  check('target can open their stash after the admin closed the view', !!w);
  await disconnect(t); await disconnect(a);
  process.exit(0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
