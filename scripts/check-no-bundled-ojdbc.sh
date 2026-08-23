#!/usr/bin/env sh
set -eu

JAR=${1:-autotest-dist/target/autotest-tool-1.0.0.jar}

if [ ! -f "$JAR" ]; then
  echo "Release JAR not found: $JAR" >&2
  exit 1
fi

if git ls-files | grep -Ei '(^|/)ojdbc[^/]*\.jar$' >/dev/null; then
  echo "ERROR: Oracle JDBC JAR is tracked by Git" >&2
  exit 1
fi

if git ls-files 'lib/jdbc/*.jar' | grep -q .; then
  echo "ERROR: External JDBC driver JAR is tracked under lib/jdbc" >&2
  exit 1
fi

if jar tf "$JAR" | grep -Eq '^(oracle/|META-INF/maven/com\.oracle\.database\.jdbc/)'; then
  echo "ERROR: Oracle JDBC content is bundled in $JAR" >&2
  exit 1
fi

echo "OK: no bundled Oracle JDBC detected"
