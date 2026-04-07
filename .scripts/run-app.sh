#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT_DIR}"

# Build sibling modules into local Maven repo so springboot module can resolve them
# even when started standalone from its own pom.xml.
./mvnw -DskipTests -pl application/springboot -am install

cd "${ROOT_DIR}/application/springboot"
exec ../../mvnw spring-boot:run "$@"
