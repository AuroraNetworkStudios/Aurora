#!/bin/sh
# Aurora's bot tests. usage: testing/run.sh <server jar> [single] [threads] [proxy]
# Runs the given suites (all three by default) against build/libs/Aurora-*.jar (or $AURORA_JAR). See README.md.
# Only one run per machine: the ports, the MySQL container and the process names are fixed.
set -e
T=$(cd "$(dirname "$0")" && pwd)
A=$(dirname "$T")
if [ ! -f "$1" ]; then echo "usage: testing/run.sh <server jar> [single] [threads] [proxy]"; exit 2; fi
SERVER_JAR=$(cd "$(dirname "$1")" && pwd)/$(basename "$1")
shift
SUITES=${*:-single threads proxy}
W=${AURORA_TEST_WORK:-$T/work}
DL=$W/downloads
if [ -z "$AURORA_JAR" ]; then
  set -- "$A"/build/libs/Aurora-*.jar
  if [ $# -ne 1 ] || [ ! -f "$1" ]; then echo "build/libs needs exactly one Aurora jar (or set AURORA_JAR): $*"; exit 2; fi
  AURORA_JAR=$1
fi
# Absolute, the bench plugin's build resolves it from its own folder
AURORA_JAR=$(cd "$(dirname "$AURORA_JAR")" && pwd)/$(basename "$AURORA_JAR")
echo "Testing $AURORA_JAR on $(basename "$SERVER_JAR")"
export AURORA_TEST_WORK=$W
export NODE_PATH=$W/node_modules
mkdir -p "$W" "$DL"
: > "$W/bots.log"
cp "$T"/server/start.sh "$T"/server/stop.sh "$T"/server/reset.sh "$T"/server/cfg.py "$W"/
B=$W

# Edits a file in place the same way on macOS and Linux
edit() { perl -0pi -e "$1" "$2"; }
wait_done() { for i in $(seq 240); do grep -q "Done (" "$1/console.log" 2>/dev/null && return 0; sleep 1; done; echo "server in $1 didn't start"; return 1; }
download() { # <file> <url> <sha512>
  if [ ! -f "$DL/$1" ]; then
    curl -sSfL -o "$DL/$1.part" "$2"
    echo "$3  $DL/$1.part" | shasum -a 512 -c - >/dev/null || { echo "checksum mismatch for $1"; exit 1; }
    mv "$DL/$1.part" "$DL/$1"
  fi
  echo "$3  $DL/$1" | shasum -a 512 -c - >/dev/null || { echo "checksum mismatch for $1, delete it to download it again"; exit 1; }
}

echo "== MySQL (Docker)"
if ! docker exec aurora-bench-mysql mysql -uroot -pbench -e "SELECT 1" >/dev/null 2>&1; then
  docker rm -f aurora-bench-mysql >/dev/null 2>&1 || true
  docker run -d --rm --name aurora-bench-mysql -p 127.0.0.1:33061:3306 -e MYSQL_ROOT_PASSWORD=bench -e MYSQL_DATABASE=aurora mysql:8.4 >/dev/null
  for i in $(seq 120); do docker exec aurora-bench-mysql mysql -uroot -pbench -h127.0.0.1 --protocol=TCP -e "SELECT 1" aurora >/dev/null 2>&1 && break; sleep 1; done
  docker exec aurora-bench-mysql mysql -uroot -pbench -h127.0.0.1 --protocol=TCP -e "SELECT 1" aurora >/dev/null 2>&1 || { echo "MySQL didn't start"; exit 1; }
fi

echo "== downloads"
# The bots speak an older protocol, Via translates
download ViaVersion-5.12.2.jar https://cdn.modrinth.com/data/P1OZGk5p/versions/Fkfcr89K/ViaVersion-5.12.2.jar \
  c6af11f50e14bc1ba239f5a90be653cce85feee30c473a4b53336b4d5527d790369cf43843f7cb06d3ac3a34fc207e7203436737ca719070644101472453401b
download ViaBackwards-5.12.2.jar https://cdn.modrinth.com/data/NpvuJQoq/versions/WFO37e8T/ViaBackwards-5.12.2.jar \
  78bc7e5fd33ecf13aee31c62e1de46a854e066a1936f294347c6798371357497fe2cc6f23f1d42003ad3050f94ac5382db95e60ffa90d765fada0322b59b0ef9
if [ ! -f "$DL/velocity.jar" ]; then
  curl -sSfL -o "$DL/velocity.jar" https://fill-data.papermc.io/v1/objects/85be73b90f295acd8d5d45a00ad10bac791d4b72b7554172612a89565ebaaa8f/velocity-4.3.0-44.jar
fi
echo "85be73b90f295acd8d5d45a00ad10bac791d4b72b7554172612a89565ebaaa8f  $DL/velocity.jar" | shasum -a 256 -c - >/dev/null || { echo "checksum mismatch for velocity.jar"; exit 1; }
if [ ! -d "$W/node_modules/mineflayer" ]; then
  (cd "$W" && [ -f package.json ] || npm init -y >/dev/null; cd "$W" && npm install --ignore-scripts --no-audit --no-fund mineflayer@4.39.0 >/dev/null)
fi

echo "== bench plugin"
if ! sh "$A/gradlew" -q -p "$T/bench-plugin" jar -PauroraJar="$AURORA_JAR" > "$W/bench-plugin.log" 2>&1; then
  cat "$W/bench-plugin.log"; echo "the bench plugin doesn't build against $AURORA_JAR"; exit 1
fi
BENCH_JAR=$T/bench-plugin/build/libs/AuroraBench.jar

echo "== servers"
"$B/stop.sh" >/dev/null 2>&1 || true
for n in a b; do
  S=$B/srv-$n
  port=$([ $n = a ] && echo 25601 || echo 25602)
  mkdir -p "$S/plugins"
  cp "$SERVER_JAR" "$S/server.jar"
  # A different server type or version can't load the other's world
  if [ "$(cat "$S/server-kind" 2>/dev/null)" != "$(basename "$SERVER_JAR")" ]; then
    for d in world world_nether world_the_end libraries versions cache; do [ -d "$S/$d" ] && mv "$S/$d" "$W/old-$n-$d-$$"; done
    basename "$SERVER_JAR" > "$S/server-kind"
  fi
  echo "eula=true" > "$S/eula.txt"
  # Peaceful: a bot that dies loses its open menu and its inventory
  printf 'server-ip=127.0.0.1\nserver-port=%s\nonline-mode=false\nmax-players=100\nspawn-protection=0\ndifficulty=peaceful\nspawn-monsters=false\nview-distance=4\nsimulation-distance=4\n' $port > "$S/server.properties"
  cp "$AURORA_JAR" "$S/plugins/Aurora.jar"
  cp "$BENCH_JAR" "$DL/ViaVersion-5.12.2.jar" "$DL/ViaBackwards-5.12.2.jar" "$S/plugins/"
done
# The first start writes the configs, which are set up for the tests below (every time, it does no harm)
if [ ! -f "$B/srv-a/plugins/Aurora/config.yml" ] || [ ! -f "$B/srv-b/config/paper-global.yml" ]; then
  "$B/start.sh" "$B/srv-a" a; "$B/start.sh" "$B/srv-b" b
  wait_done "$B/srv-a"; wait_done "$B/srv-b"
  "$B/stop.sh" >/dev/null 2>&1
fi
for n in a b; do
  S=$B/srv-$n
  edit 's/^(  connection-throttle:).*/$1 -1/m' "$S/bukkit.yml"
  python3 - "$S/plugins/Aurora/config.yml" <<'EOF'
import sys, re
p = sys.argv[1]; s = open(p).read()
m = re.search(r'(?ms)^mysql:\n(.*?)(?=^\S)', s)
block = m.group(1)
for key, value in [('host', '"127.0.0.1"'), ('port', '33061'), ('database', '"aurora"'), ('username', '"root"'), ('password', '"bench"')]:
    block = re.sub(r'(?m)^  %s: .*$' % key, '  %s: %s' % (key, value), block)
s = s[:m.start(1)] + block + s[m.end(1):]
open(p, 'w').write(s)
EOF
done

V=$B/velocity
if echo "$SUITES" | grep -q proxy && [ ! -f "$V/velocity.toml" ]; then
  echo "== velocity"
  mkdir -p "$V" && cp "$DL/velocity.jar" "$V/"
  (cd "$V" && : > cmds && : > console.log && (tail -n0 -f cmds | java -Xmx512M -Dbench.role=velocity -jar velocity.jar > console.log 2>&1 &)) </dev/null >/dev/null 2>&1
  for i in $(seq 60); do grep -q -E "Done \(|Listening on" "$V/console.log" 2>/dev/null && break; sleep 1; done
  echo shutdown >> "$V/cmds"
  for i in $(seq 30); do pgrep -f "[b]ench.role=velocity" >/dev/null || break; sleep 1; done
  pkill -f "[t]ail -n0 -f cmds" || true
  python3 - "$V/velocity.toml" <<'EOF'
import sys, re
p = sys.argv[1]; s = open(p).read()
s = s.replace('bind = "0.0.0.0:25565"', 'bind = "127.0.0.1:25600"')
s = re.sub(r'(?m)^online-mode = true', 'online-mode = false', s)
s = s.replace('player-info-forwarding-mode = "NONE"', 'player-info-forwarding-mode = "MODERN"')
s = re.sub(r'(?m)^login-ratelimit = \d+', 'login-ratelimit = 0', s)
start = s.index('[servers]'); end = s.index('[forced-hosts]')
s = s[:start] + '[servers]\na = "127.0.0.1:25601"\nb = "127.0.0.1:25602"\ntry = [\n    "a"\n]\n\n' + s[end:]
s = re.sub(r'(?ms)^\[forced-hosts\].*?(?=^\[)', '[forced-hosts]\n\n', s)
open(p, 'w').write(s)
EOF
fi

cd "$T/bots"
FAILED=0
bot() { # runs a bot script (env VAR=... node script.js), prints its checks and counts the failed ones
  code=0
  "$@" > "$W/bot.out" 2>&1 || code=$?
  cat "$W/bot.out" >> "$W/bots.log"
  grep -E "^FAIL|^PASS|checks passed|RESULT|ERROR" "$W/bot.out" || true
  n=$(grep -cE "^FAIL|^ERROR" "$W/bot.out" || true)
  if [ "$code" -ne 0 ] && [ "$n" -eq 0 ]; then echo "FAIL  $* exited with $code: $(tail -1 "$W/bot.out")"; n=1; fi
  FAILED=$((FAILED + n))
}
errs() {
  for n in "$@"; do
    c=$(sed -E 's/\x1b\[[0-9;]*m//g' "$B/srv-$n/console.log" | grep -E 'ERROR|SEVERE|Exception|Failed' | grep -v 'bench reward failure' | grep -vc 'StacklessClosed' || true)
    echo "   errors on $n: $c"
    FAILED=$((FAILED + c))
  done
}
velocity() { # on|off for both servers
  for n in a b; do
    if [ "$1" = on ]; then
      python3 "$B/cfg.py" "$B/srv-$n/plugins/Aurora/config.yml" storage=mysql
      edit "s/(  velocity:\n    enabled: )false(\n    online-mode: )\w+(\n    secret: ).*/\${1}true\${2}false\${3}'$(cat "$V/forwarding.secret")'/" "$B/srv-$n/config/paper-global.yml"
    else
      edit 's/(  velocity:\n    enabled: )true/${1}false/' "$B/srv-$n/config/paper-global.yml"
    fi
  done
}
setcfg() { edit "$1" "$B/srv-a/plugins/Aurora/config.yml"; }

for suite in $SUITES; do
  case $suite in
  single)
    # One server: rejoins, offline edits, menus and dupes, restarts, hanging saves, crashes. Both storages.
    velocity off
    for storage in mysql yaml; do
      echo "=== single server, $storage"
      "$B/stop.sh" >/dev/null 2>&1; "$B/reset.sh" >/dev/null
      python3 "$B/cfg.py" "$B/srv-a/plugins/Aurora/config.yml" storage=$storage
      setcfg 's/^offline-player-cache: .*/offline-player-cache: true/m'
      setcfg 's/^user-auto-save-in-minutes: .*/user-auto-save-in-minutes: 10/m'
      "$B/start.sh" "$B/srv-a" a; wait_done "$B/srv-a"
      P=$(echo $storage | cut -c1 | tr a-z A-Z)
      echo "--- soak (rejoins, offline breaks, edits)"; bot env PREFIX=${P}a PLAYERS=6 SECONDS=90 RATE=25 SERVERS=A node soak2.js
      echo "--- shop soak (takes and transfers)"; bot env PREFIX=${P}b PLAYERS=6 SECONDS=90 RATE=30 SERVERS=A node shopsoak2.js
      echo "--- menus"; bot env PREFIX=${P}c node menu2.js; bot env PREFIX=${P}d node menu3.js
      echo "--- menu dupes"; bot env PREFIX=${P}e node menudupe.js
      echo "--- offline stash take x5"; for r in 1 2 3 4 5; do bot env PREFIX=${P}f$r node stash5.js; done
      echo "--- restarts"; bot env PREFIX=${P}g node yaml2.js
      if [ $storage = mysql ]; then echo "--- quit right before stop"; bot env PREFIX=${P}h node quitstop.js; fi
      echo "--- hanging saves and sync edits"; bot env STORAGE=$storage PREFIX=${P}k node slowsave.js
      errs a
      "$B/stop.sh" >/dev/null 2>&1
      echo "--- offline edit saved by autosave, then a crash"
      setcfg 's/^user-auto-save-in-minutes: .*/user-auto-save-in-minutes: 1/m'
      "$B/start.sh" "$B/srv-a" a; wait_done "$B/srv-a"
      bot env PREFIX=${P}j node autosave.js
      echo "--- storage (emoji meta, saves of idle players)"; bot env STORAGE=$storage PREFIX=${P}u node storage3.js
      errs a
      "$B/stop.sh" >/dev/null 2>&1
      setcfg 's/^user-auto-save-in-minutes: .*/user-auto-save-in-minutes: 10/m'
      echo "--- offline-player-cache off"
      setcfg 's/^offline-player-cache: .*/offline-player-cache: false/m'
      "$B/start.sh" "$B/srv-a" a; wait_done "$B/srv-a"
      bot env PREFIX=${P}i node cacheoff.js
      errs a
      "$B/stop.sh" >/dev/null 2>&1
      setcfg 's/^offline-player-cache: .*/offline-player-cache: true/m'
    done
    ;;
  threads)
    # Folia: events and rewards on the player's own thread, then dbmigrate from YAML to MySQL
    echo "=== threads, dbmigrate"
    velocity off
    "$B/stop.sh" >/dev/null 2>&1; "$B/reset.sh" >/dev/null
    python3 "$B/cfg.py" "$B/srv-a/plugins/Aurora/config.yml" storage=yaml
    "$B/start.sh" "$B/srv-a" a; wait_done "$B/srv-a"
    bot env PREFIX=Tz node threads.js
    errs a
    "$B/stop.sh" >/dev/null 2>&1
    python3 "$B/cfg.py" "$B/srv-a/plugins/Aurora/config.yml" storage=yaml
    ;;
  proxy)
    # Two servers on one MySQL behind Velocity: players moving between them keep their data
    echo "=== two servers behind Velocity"
    "$B/stop.sh" >/dev/null 2>&1; "$B/reset.sh" >/dev/null
    velocity on
    for n in a b; do edit 's/^offline-player-cache: .*/offline-player-cache: false/m' "$B/srv-$n/plugins/Aurora/config.yml"; done
    "$B/start.sh" "$B/srv-a" a; "$B/start.sh" "$B/srv-b" b; wait_done "$B/srv-a"; wait_done "$B/srv-b"
    (cd "$V" && : > cmds && : > console.log && (tail -n0 -f cmds 2>/dev/null | java -Xmx512M -Dbench.role=velocity -jar velocity.jar > console.log 2>&1 &)) </dev/null >/dev/null 2>&1
    for i in $(seq 60); do grep -q "Done (" "$V/console.log" && break; sleep 1; done
    bot env PREFIX=Ha node handover.js
    errs a b
    echo end >> "$V/cmds"; for i in $(seq 30); do pgrep -f "[b]ench.role=velocity" >/dev/null || break; sleep 1; done
    pkill -f "[b]ench.role=velocity" || true
    "$B/stop.sh" >/dev/null 2>&1
    velocity off
    for n in a b; do edit 's/^offline-player-cache: .*/offline-player-cache: true/m' "$B/srv-$n/plugins/Aurora/config.yml"; done
    ;;
  *) echo "unknown suite: $suite"; exit 2 ;;
  esac
done

echo "=== $FAILED failed checks and server errors"
[ "$FAILED" -eq 0 ]
