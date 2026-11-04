#!/bin/sh
# Fresh state for a run, with the servers stopped: empties the MySQL database and the YAML user data
B=$(cd "$(dirname "$0")" && pwd)
docker exec aurora-bench-mysql mysql -uroot -pbench -e "DROP DATABASE IF EXISTS aurora; CREATE DATABASE aurora;" 2>/dev/null
# Vanilla player data too, or bots keep their inventories from the last run
for n in a b; do rm -rf "$B/srv-$n/plugins/Aurora/userdata" "$B/srv-$n/world/players" "$B/srv-$n/world/playerdata"; done
echo "reset done"
