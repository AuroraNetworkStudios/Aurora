#!/bin/sh
# usage: start.sh <dir> <role>
# Keeps the previous console in console.all.log. Detached from the caller's stdin/stdout/stderr, so a caller piping
# into grep isn't held open by the server
cd "$1" && { [ -f console.log ] && cat console.log >> console.all.log; true; } && : > cmds && : > console.log && (tail -n0 -f cmds 2>/dev/null | java -Xms1G -Xmx2G -Dbench.role=$2 -jar server.jar --nogui > console.log 2>&1 &) </dev/null >/dev/null 2>&1
