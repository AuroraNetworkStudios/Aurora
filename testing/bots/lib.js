// Helpers to drive the two test servers (console via cmds file, results via console.log) and mineflayer bots
const fs = require('fs');
const path = require('path');
const mineflayer = require('mineflayer');

// The servers' folder, set up by run.sh
const BENCH = process.env.AURORA_TEST_WORK || path.join(__dirname, '..', 'work');
const SERVERS = { A: { dir: path.join(BENCH, 'srv-a'), port: 25601 }, B: { dir: path.join(BENCH, 'srv-b'), port: 25602 } };

const sleep = ms => new Promise(r => setTimeout(r, ms));
let failures = 0;
const results = [];

function check(name, ok, detail = '') {
  results.push({ name, ok });
  if (!ok) failures++;
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? '  (' + detail + ')' : ''}`);
}

function logSize(server) {
  return fs.statSync(path.join(SERVERS[server].dir, 'console.log')).size;
}

function send(server, command) {
  fs.appendFileSync(path.join(SERVERS[server].dir, 'cmds'), command + '\n');
}

// Waits for a console line matching regex, written after the given byte offset
async function waitLog(server, regex, from, timeoutMs = 15000) {
  const file = path.join(SERVERS[server].dir, 'console.log');
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    const fd = fs.openSync(file, 'r');
    const size = fs.fstatSync(fd).size;
    const buf = Buffer.alloc(Math.max(0, size - from));
    fs.readSync(fd, buf, 0, buf.length, from);
    fs.closeSync(fd);
    const match = buf.toString('utf8').split('\n').map(l => l.match(regex)).find(Boolean);
    if (match) return match;
    await sleep(100);
  }
  return null;
}

async function command(server, cmd, regex, timeoutMs) {
  const from = logSize(server);
  send(server, cmd);
  return waitLog(server, regex, from, timeoutMs);
}

async function inspect(server, name) {
  const m = await command(server, `opbench inspect ${name}`, new RegExp(`BENCH inspect name=${name} (.*)$`));
  if (!m) return null;
  const line = m[1];
  const get = k => (line.match(new RegExp(`${k}=(\\S+)`)) || [])[1];
  return { online: get('online') === 'true', loaded: get('loaded') === 'true', warm: get('warm') === 'true', diamonds: get('diamonds') !== undefined ? Number(get('diamonds')) : null, lease: get('lease'), raw: line };
}

async function count(server, name, timeoutMs = 15000) {
  const m = await command(server, `opbench count ${name}`, new RegExp(`BENCH count name=${name} diamonds=(\\S+)`), timeoutMs);
  return m ? (m[1] === 'FAILED' ? 'FAILED' : Number(m[1])) : 'TIMEOUT';
}

async function stored(server, name, timeoutMs = 15000) {
  const m = await command(server, `opbench stored ${name}`, new RegExp(`BENCH stored name=${name} diamonds=(\\S+)`), timeoutMs);
  return m ? (m[1] === 'FAILED' ? 'FAILED' : Number(m[1])) : 'TIMEOUT';
}

async function add(server, name, amount, timeoutMs = 15000) {
  const m = await command(server, `opbench add ${name} ${amount}`, new RegExp(`BENCH add name=${name} amount=${amount} result=(\\S+)`), timeoutMs);
  return m ? m[1] : 'TIMEOUT';
}

// The bots never move. Movement packets translated to 26.3 are refused ("Invalid move player packet"), so none are
// sent; teleports are still confirmed.
const MOVE_PACKETS = new Set(['position', 'position_look', 'look', 'flying']);
function stayStill(bot) {
  const write = bot._client.write.bind(bot._client);
  bot._client.write = (name, params) => MOVE_PACKETS.has(name) ? undefined : write(name, params);
}

// Resolves once the bot has spawned on the server
function connect(name, server, timeoutMs = 30000) {
  return new Promise((resolve, reject) => {
    const bot = mineflayer.createBot({ host: '127.0.0.1', port: SERVERS[server].port, username: name, version: '26.1', auth: 'offline', hideErrors: true, physicsEnabled: false });
    const timer = setTimeout(() => { bot.end(); reject(new Error(`${name} spawn timeout on ${server}`)); }, timeoutMs);
    bot.once('spawn', () => { clearTimeout(timer); bot.server = server; resolve(bot); });
    bot.once('kicked', r => { clearTimeout(timer); reject(new Error(`${name} kicked from ${server}: ${JSON.stringify(r)}`)); });
    bot.on('error', () => {});
    stayStill(bot);
  });
}

function disconnect(bot) {
  return new Promise(resolve => {
    if (!bot || bot.ended) return resolve();
    bot.once('end', () => resolve());
    bot.quit();
    setTimeout(resolve, 5000);
  });
}

// Polls until the predicate holds for the inspected state, returns the last state
async function waitState(server, name, predicate, timeoutMs = 15000) {
  const deadline = Date.now() + timeoutMs;
  let state;
  while (Date.now() < deadline) {
    state = await inspect(server, name);
    if (state && predicate(state)) return state;
    await sleep(250);
  }
  return state;
}

module.exports = { BENCH, stayStill, SERVERS, sleep, check, send, logSize, waitLog, command, inspect, count, stored, add, connect, disconnect, waitState, results, failures: () => failures };
