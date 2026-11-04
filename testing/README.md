# Bot tests

Real servers with bots (mineflayer) joining, leaving, rejoining, switching servers and clicking menus while the
player data is changed from every side, then checking that nothing was lost, duplicated or saved over. Run them before
every release on Paper and on Folia; unit tests can't catch the races these find.

They aren't part of the build or CI.

## Running

```sh
testing/run.sh <server jar> [single] [threads] [proxy]
```

- `<server jar>`: a Paper or Folia server jar, for example `paper-26.2-135.jar` or `folia-26.3-6.jar` from
  [papermc.io](https://papermc.io/downloads). Run it once per server type you release for.
- The Aurora jar under test is `build/libs/Aurora-*.jar` (build it first: `./gradlew build`), or `AURORA_JAR=<path>`.
- Without suite names, all three run. That takes about 30 minutes.

It needs Docker (for MySQL), Node with npm, Java 25, Python 3, curl and perl. The first run downloads ViaVersion,
ViaBackwards and Velocity (checked against fixed checksums) and mineflayer 4.39.0 into `testing/work/`, and builds the
helper plugin in `bench-plugin/` against the Aurora jar.

The last line says how many checks failed plus how many errors the servers logged; the exit code is 0 only if both are
zero. Each check prints `PASS` or `FAIL` with what it saw; a bot script that crashes counts as failed.

- Only one run per machine at a time: the ports, the MySQL container (`aurora-bench-mysql`, left running afterwards,
  remove it with `docker rm -f aurora-bench-mysql`) and the server process names are fixed.
- `AURORA_TEST_WORK=<folder>` puts the servers and downloads somewhere else than `testing/work/`.
- The servers listen on 127.0.0.1 only (they run in offline mode and the bots have op). They need Paper or Folia 26.2
  or newer.

## The suites

- **single**: one server, once on MySQL and once on YAML:
  - soak: 6 bots for 90 seconds rejoining, leaving for a while and being changed while offline, ~4000 changes, then
    every player's stored data is compared with what they should have;
  - shop soak: takes and transfers between players, answered at once (`editUserNow`), checked the same way;
  - menus (the stash, offline views, live views, refreshes) and menu dupes (number keys, drops, drags, clicks under lag);
  - emoji player meta and saves of idle players, server restarts, a quit right before a stop, saves that hang in the
    database, an offline change saved by the autosave and then a crash, and `offline-player-cache` turned off.
- **threads**: player events and rewards happen on the player's own thread (this is what fails on Folia when they
  don't), then `aurora dbmigrate` from YAML to MySQL.
- **proxy**: two servers on one MySQL behind Velocity, players moving between them while their data changes.

## Layout

- `run.sh` sets up `work/` (MySQL in Docker on port 33061, servers A and B on 25601/25602, Velocity on 25600) and runs
  the suites.
- `bots/`: the bot scripts the suites run, `lib.js` has the shared helpers (console commands through each server's `cmds` file,
  results from its `console.log`).
- `bench-plugin/`: the `opbench` console command the bots use to change and inspect player data on the server.
- `server/`: start, stop and reset the servers.

To look at a failure: every bot's full output is in `work/bots.log`, the servers' consoles in `work/srv-a/` and
`work/srv-b/` (`console.log` for the last start, `console.all.log` for the ones before), and a single suite can be run
on its own, for example `testing/run.sh paper.jar threads`.
