#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
MAX_WAIT_SECONDS="${MAX_WAIT_SECONDS:-90}"
BOOTSTRAP_SCRIPT="${ROOT_DIR}/.scripts/keycloak/bootstrap-fsa-realm.sh"

log() {
  printf '[workshop-setup] %s\n' "$*"
}

require_cmd() {
  local cmd="$1"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    printf 'Missing required command: %s\n' "$cmd" >&2
    exit 1
  fi
}

wait_for_keycloak() {
  local elapsed=0
  while true; do
    if curl -fsS "${KEYCLOAK_URL}/realms/master" >/dev/null 2>&1; then
      return 0
    fi

    if [[ "${elapsed}" -ge "${MAX_WAIT_SECONDS}" ]]; then
      printf 'Keycloak did not become ready within %ss at %s\n' "${MAX_WAIT_SECONDS}" "${KEYCLOAK_URL}" >&2
      return 1
    fi

    sleep 2
    elapsed=$((elapsed + 2))
  done
}

main() {
  require_cmd docker
  require_cmd curl

  if [[ ! -f "${BOOTSTRAP_SCRIPT}" ]]; then
    printf 'Bootstrap script not found: %s\n' "${BOOTSTRAP_SCRIPT}" >&2
    exit 1
  fi

  log "Starting Keycloak container via docker compose."
  (cd "${ROOT_DIR}" && docker compose up -d my-keycloak)

  log "Waiting for Keycloak to be ready at ${KEYCLOAK_URL}."
  wait_for_keycloak

  log "Bootstrapping realm/client/users."
  KEYCLOAK_URL="${KEYCLOAK_URL}" bash "${BOOTSTRAP_SCRIPT}"

  cat <<SUMMARY

Workshop setup completed.

Keycloak:
- URL: ${KEYCLOAK_URL}
- Realm: FSA
- Client: fsa-client

Demo users:
- admin@posam.sk / admin123 / ADMIN
- teacher@posam.sk / teacher123 / TEACHER
- student@posam.sk / student123 / STUDENT

Next:
1) Start app with keycloak profile: ./mvnw -pl application/springboot spring-boot:run -Dspring-boot.run.profiles=keycloak
2) Run API tests: npm --prefix tests run test:api
SUMMARY
}

main "$@"
