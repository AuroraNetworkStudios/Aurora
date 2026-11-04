#!/bin/sh
S=$(dirname "$0"); for n in a b; do echo stop >> $S/srv-$n/cmds; done
for i in $(seq 1 60); do pgrep -f "[b]ench.role" >/dev/null || break; perl -e 'select(undef,undef,undef,1)'; done
pkill -f "[t]ail -n0 -f cmds"; true
