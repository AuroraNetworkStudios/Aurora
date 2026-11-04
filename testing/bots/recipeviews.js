// AuroraCrafting's recipe views, with the test recipes in testing/crafting/blueprints: each view shows its result,
// clicking an ingredient that has its own recipe shows that one and its back button comes back, and the previous/next
// buttons go to the recipe's neighbours in its group.
const { connect, disconnect, sleep, check, results, failures } = require('./lib');
const P = process.env.PREFIX || 'Rv';

// The test recipes: their result, and their group in the order the recipes are written
const groups = {
  tvct: [['tv_ct1', 'lodestone'], ['tv_ct2', 'target'], ['tv_ct3', 'lectern']],
  tvfu: [['tv_fu1', 'quartz'], ['tv_fu2', 'flint'], ['tv_fu3', 'brick']],
  tvbl: [['tv_bl1', 'quartz'], ['tv_bl2', 'flint'], ['tv_bl3', 'brick']],
  tvsm: [['tv_sm1', 'quartz'], ['tv_sm2', 'flint'], ['tv_sm3', 'brick']],
  tvca: [['tv_ca1', 'quartz'], ['tv_ca2', 'flint'], ['tv_ca3', 'brick']],
  tvcl: [['tv_cl1', 'prismarine_crystals'], ['tv_cl2', 'magma_block'], ['tv_cl3', 'snow_block']],
  tvst: [['tv_st1', 'diamond_sword'], ['tv_st2', 'diamond_axe'], ['tv_st3', 'diamond_hoe']],
  tvbr: [['tv_br1', 'honey_bottle'], ['tv_br2', 'experience_bottle'], ['tv_br3', 'dragon_breath']],
  tva: [['tv_a1', 'beacon'], ['tv_a2', 'conduit'], ['tv_a3', 'end_crystal']],
};
// Every one of these has amethyst shards as an ingredient, whose own recipe is tv_cut (calcite in the stonecutter)
const withAmethyst = ['tv_ct1', 'tv_ct2', 'tv_fu1', 'tv_bl1', 'tv_sm1', 'tv_ca1', 'tv_cl1', 'tv_st1', 'tv_br1', 'tv_br2', 'tv_a1', 'tv_a2'];

const nextWindow = (bot, ms) => new Promise(r => {
  const t = setTimeout(() => { bot.removeListener('windowOpen', on); r(null); }, ms);
  const on = w => { clearTimeout(t); r(w); };
  bot.once('windowOpen', on);
});
const text = v => v == null ? '' : (typeof v === 'string' ? v : JSON.stringify(v));
const top = w => w ? w.slots.slice(0, w.inventoryStart).map((s, i) => s && { slot: i, name: s.name, count: s.count, label: text(s.customName) }).filter(Boolean) : [];
const shows = (w, item) => top(w).some(s => s.name === item);
const button = (w, word) => top(w).find(s => s.label.toLowerCase().includes(word));

async function open(bot, id) {
  if (bot.currentWindow) { bot.closeWindow(bot.currentWindow); await sleep(300); }
  const w = nextWindow(bot, 4000);
  bot.chat(`/recipes ${id}`);
  const win = await w;
  await sleep(300);
  return win;
}
async function click(bot, slot) {
  const w = nextWindow(bot, 2000);
  await bot.clickWindow(slot, 0, 0).catch(() => {});
  const win = await w;
  if (win) await sleep(300);
  return win;
}

(async () => {
  const bot = await connect(`${P}b`, 'A');
  await sleep(4000);
  const failed = { result: [], ingredient: [], back: [], next: [], previous: [] };
  for (const recipes of Object.values(groups)) {
    for (let i = 0; i < recipes.length; i++) {
      const [id, result] = recipes[i];
      const view = await open(bot, id);
      if (!shows(view, result)) { failed.result.push(id); continue; }

      const next = recipes[(i + 1) % recipes.length], previous = recipes[(i + recipes.length - 1) % recipes.length];
      const nextButton = button(view, 'next');
      if (!nextButton || !shows(await click(bot, nextButton.slot), next[1])) failed.next.push(id);
      const again = await open(bot, id);
      const prevButton = button(again, 'previous');
      if (!prevButton || !shows(await click(bot, prevButton.slot), previous[1])) failed.previous.push(id);

      if (withAmethyst.includes(id)) {
        const view2 = await open(bot, id);
        const amethyst = top(view2).find(s => s.name === 'amethyst_shard');
        const ingredientView = amethyst && await click(bot, amethyst.slot);
        if (!ingredientView || !shows(ingredientView, 'calcite')) { failed.ingredient.push(id); continue; }
        const back = button(ingredientView, 'back');
        if (!back || !shows(await click(bot, back.slot), result)) failed.back.push(id);
      }
    }
  }
  // Amounts: what the recipe gives, and what it takes (vanilla furnaces take one of the input)
  const amount = async (id, item) => (top(await open(bot, id)).find(s => s.name === item) || {}).count;
  const cut = await amount('tv_cut', 'amethyst_shard'), cauldron = await amount('tv_cl2', 'cobblestone'), furnace = await amount('tv_fu2', 'tuff');
  check('amounts: a result of 2, a cauldron input of 3, a furnace input of 1', cut === 2 && cauldron === 3 && furnace === 1,
    `result=${cut} cauldron=${cauldron} furnace=${furnace}`);

  const all = Object.values(groups).flat().length;
  check(`every recipe view shows its result (${all} recipes)`, failed.result.length === 0, failed.result.join(' '));
  check(`an ingredient with its own recipe shows that recipe (${withAmethyst.length} views)`, failed.ingredient.length === 0, failed.ingredient.join(' '));
  check('the back button of an ingredient\'s recipe comes back', failed.back.length === 0, failed.back.join(' '));
  check('next shows the next recipe of the group', failed.next.length === 0, failed.next.join(' '));
  check('previous shows the previous recipe of the group', failed.previous.length === 0, failed.previous.join(' '));
  console.log(`${results.length - failures()}/${results.length} checks passed`);
  await disconnect(bot);
  await sleep(1500);
  process.exit(failures() ? 1 : 0);
})().catch(e => { console.log('ERROR', e); process.exit(2); });
