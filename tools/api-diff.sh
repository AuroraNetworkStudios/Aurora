#!/bin/sh
# Shows what changed in Aurora's public API between two jars, for release notes and to catch accidental breaks.
# usage: tools/api-diff.sh <old Aurora jar> <new Aurora jar>
# Lines starting with - are gone or changed in the new jar (breaking for plugins using them), + are new.
set -e
for jar in "$1" "$2"; do [ -f "$jar" ] || { echo "usage: tools/api-diff.sh <old Aurora jar> <new Aurora jar> ($jar not found)"; exit 2; }; done
api() {
  classes=$(unzip -Z1 "$1" | grep '^gg/auroramc/aurora/api/.*\.class$' | grep -v '\$[0-9]*\.class$' | sed 's/\.class$//; s#/#.#g')
  # Each member on one line with its class in front, so the diff says where it is
  javap -protected -cp "$1" $classes 2>/dev/null | awk '
    /^Compiled from/ { next }
    /^[a-z].*[{]$/ { cls = $0; sub(/ *[{]$/, "", cls); api = (cls ~ /^(public|protected) /); if (api) print cls; next }
    /^}/ { next }
    api { sub(/^ +/, ""); print cls " :: " $0 }
  ' | sort
}
old=$(mktemp); new=$(mktemp)
trap 'rm -f "$old" "$new"' EXIT
api "$1" > "$old"
api "$2" > "$new"
diff "$old" "$new" | grep '^[<>]' | sed 's/^< /- /; s/^> /+ /' || true
echo "$(grep -c . "$old") API lines before, $(grep -c . "$new") now"
