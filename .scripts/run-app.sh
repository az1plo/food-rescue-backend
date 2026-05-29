#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

if [[ -x "./mvnw" ]]; then
  MVN_CMD=("./mvnw")
elif command -v mvn >/dev/null 2>&1; then
  MVN_CMD=("mvn")
else
  echo "[run-app] Maven wrapper or mvn command is required." >&2
  exit 1
fi

# Build sibling modules into local Maven repo so springboot module can resolve them
# even when started standalone from its own pom.xml.
"${MVN_CMD[@]}" -DskipTests -pl application/springboot -am install

cd "${ROOT_DIR}/application/springboot"
if [[ -x "../../mvnw" ]]; then
  exec ../../mvnw spring-boot:run "$@"
fi

exec mvn spring-boot:run "$@"
