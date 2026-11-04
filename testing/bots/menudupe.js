// Menu dupe and cost checks for the fix/3.0-correctness branch: costs that can't be paid, click types that
// must not press buttons, dragging into a menu, click spam while ticks are slow, and worn armor as a cost
const fs = require('fs');
const path = require('path');
const { SERVERS, sleep, check, send, command, connect, disconnect } = require('./lib');
const P = process.env.PREFIX || 'Md';
const name = `${P}Bot`;

const GUI = `title: "Dupe test"
rows: 3
items:
  buy:
    material: diamond
    slot: 10
    name: "Buy"
    on-click:
      - "[take-items] diamond/5"
      - "[give-item] emerald/1"
  pay:
    material: gold_ingot
    slot: 12
    name: "Pay"
    click-requirements:
      - requirement: "[has-items] diamond/1"
    on-click:
      - "[console] clear {player} diamond 1"
      - "[console] give {player} emerald 1"
  helmet:
    material: diamond_helmet
    slot: 14
    name: "Helmet"
    on-click:
      - "[take-items] diamond_helmet/1"
      - "[give-item] emerald/1"
`;

async function countOf(item) {
  const m = await command('A', `clear ${name} ${item} 0`, new RegExp(`(Found (\\d+) matching item|No items were found on player ${name})`));
  if (!m) return 'TIMEOUT';
  return m[2] !== undefined ? Number(m[2]) : 0;
}

const nextWindow = (bot, ms = 5000) => new Promise(r => { const t = setTimeout(() => r(null), ms); bot.once('windowOpen', w => { clearTimeout(t); r(w); }); });

async function open(bot) {
  const w = nextWindow(bot);
  send('A', `aurora gui open ${name} dupetest`);
  const window = await w;
  await sleep(300);
  return window;
}

// A raw click packet, so modes mineflayer doesn't simulate (drag) can be sent too
function rawClick(bot, slot, mouseButton, mode) {
  bot._client.write('window_click', {
    windowId: bot.currentWindow.id, stateId: bot.currentWindow.stateId ?? 0, slot, mouseButton, mode,
    changedSlots: [], cursorItem: null
  });
}

(async () => {
  fs.writeFileSync(path.join(SERVERS.A.dir, 'plugins/Aurora/gui/menus/dupetest.yml'), GUI);
  await command('A', 'aurora gui reload', /Successfully reloaded|guis/);

  const bot = await connect(name, 'A');
  await sleep(1500);
  send('A', `clear ${name}`);
  send('A', `give ${name} diamond 3`);
  await sleep(800);

  // A cost that can't be paid stops the list
  await open(bot);
  await bot.clickWindow(10, 0, 0).catch(() => {});
  await sleep(800);
  check('take-items without enough items gives nothing', await countOf('emerald') === 0 && await countOf('diamond') === 3);

  send('A', `give ${name} diamond 5`);
  await sleep(500);
  await bot.clickWindow(10, 0, 0).catch(() => {});
  await sleep(800);
  check('take-items with enough items takes them and gives once', await countOf('emerald') === 1 && await countOf('diamond') === 3);

  // Click types that must not press buttons
  rawClick(bot, 10, 0, 2); // number key 1
  await sleep(400);
  rawClick(bot, 10, 0, 4); // drop (Q)
  await sleep(400);
  rawClick(bot, 10, 2, 3); // middle click
  await sleep(400);
  rawClick(bot, 10, 40, 2); // offhand swap (F)
  await sleep(800);
  check('number key, drop, middle click and offhand swap press nothing', await countOf('emerald') === 1 && await countOf('diamond') === 3);

  // Dragging the diamonds over empty menu slots: they must stay with the player
  const diamondSlot = bot.currentWindow.slots.findIndex((s, i) => i >= 27 && s && s.name === 'diamond');
  await bot.clickWindow(diamondSlot, 0, 0).catch(() => {}); // pick the stack up
  await sleep(300);
  rawClick(bot, -999, 0, 5); // start left drag
  rawClick(bot, 0, 1, 5);
  rawClick(bot, 1, 1, 5);
  rawClick(bot, 2, 1, 5);
  rawClick(bot, -999, 2, 5); // end
  await sleep(500);
  bot.closeWindow(bot.currentWindow);
  await sleep(800);
  check('dragging items over empty menu slots keeps them', await countOf('diamond') === 3);

  // Click spam while ticks are slow: the console commands of the first click run a tick later
  send('A', `clear ${name}`);
  send('A', `give ${name} diamond 1`);
  send('A', 'tick rate 4');
  await sleep(1000);
  await open(bot);
  for (let i = 0; i < 12; i++) {
    rawClick(bot, 12, 0, 0);
    await sleep(90);
  }
  await sleep(2500);
  send('A', 'tick rate 20');
  await sleep(800);
  const emeralds = await countOf('emerald');
  check('12 clicks under lag pay one diamond for one emerald', emeralds === 1 && await countOf('diamond') === 0, `emeralds=${emeralds}`);

  // Worn armor is never taken as a cost
  bot.closeWindow(bot.currentWindow);
  send('A', `clear ${name}`);
  send('A', `item replace entity ${name} armor.head with diamond_helmet`);
  await sleep(800);
  await open(bot);
  await bot.clickWindow(14, 0, 0).catch(() => {});
  await sleep(800);
  check('a worn helmet is not taken by take-items', await countOf('emerald') === 0 && await countOf('diamond_helmet') === 1);

  bot.closeWindow(bot.currentWindow);
  await sleep(500);
  // Menu items never end up with the player
  check('no menu items left in the inventory', await countOf('gold_ingot') === 0 && await countOf('diamond') === 0);
  await disconnect(bot);
  process.exit(0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
