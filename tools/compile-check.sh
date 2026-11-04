#!/bin/sh
# Compiles plugins against a local Aurora jar, to see what an API change breaks before releasing it. The plugins'
# own checkouts and builds aren't touched: each is copied (its committed HEAD) to a temporary folder first.
# usage: tools/compile-check.sh <plugin checkout>...
# AURORA_JAR defaults to build/libs/Aurora-*.jar; GRADLE_ARGS are passed to Gradle (like --offline).
set -e
A=$(cd "$(dirname "$0")/.." && pwd)
AURORA_JAR=${AURORA_JAR:-$(ls "$A"/build/libs/Aurora-*.jar | head -1)}
TMP=$(mktemp -d)
cat > "$TMP/local-aurora.init.gradle" <<'INIT'
// Replaces the Aurora dependency with the jar in AURORA_JAR
allprojects {
    afterEvaluate {
        configurations.configureEach { exclude group: 'gg.auroramc', module: 'Aurora' }
        dependencies { compileOnly files(System.getenv('AURORA_JAR')) }
    }
}
INIT
status=0
for dir in "$@"; do
  name=$(basename "$dir")
  mkdir -p "$TMP/$name"
  git -C "$dir" archive HEAD | tar -x -C "$TMP/$name"
  if (cd "$TMP/$name" && AURORA_JAR="$AURORA_JAR" sh ./gradlew -q compileJava --init-script "$TMP/local-aurora.init.gradle" $GRADLE_ARGS > "$TMP/$name.log" 2>&1); then
    echo "OK    $name"
  else
    echo "FAIL  $name, see $TMP/$name.log"
    grep -E "error:" "$TMP/$name.log" | head -20
    status=1
  fi
done
# Kept when something failed, for the logs
[ $status -eq 0 ] && rm -rf "$TMP"
exit $status
