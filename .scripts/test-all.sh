#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE_URL="${BASE_URL:-http://localhost:8080}"
APP_READY_URL="${APP_READY_URL:-${BASE_URL}/actuator/health/readiness}"
APP_LOG="${APP_LOG:-${ROOT_DIR}/app-test.log}"

cd "${ROOT_DIR}"

echo "[test-all] Running Maven tests..."
./mvnw test

echo "[test-all] Starting application for API tests..."
RUN_ARGS=(-DskipTests)
if [[ -n "${SPRING_BOOT_RUN_JVM_ARGS:-}" ]]; then
  RUN_ARGS+=("-Dspring-boot.run.jvmArguments=${SPRING_BOOT_RUN_JVM_ARGS}")
fi

./.scripts/run-app.sh "${RUN_ARGS[@]}" > "${APP_LOG}" 2>&1 &
APP_PID=$!

cleanup() {
  kill "${APP_PID}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

is_app_ready() {
  local response
  local body
  local status_code

  response="$(curl -sS -w $'\n%{http_code}' "${APP_READY_URL}" || true)"
  status_code="${response##*$'\n'}"
  body="${response%$'\n'*}"

  [[ "${status_code}" == "200" ]] && [[ "${body}" == *"\"status\":\"UP\""* ]]
}

echo "[test-all] Waiting for application readiness endpoint at ${APP_READY_URL} ..."
for _ in $(seq 1 60); do
  if is_app_ready; then
    break
  fi
  if ! kill -0 "${APP_PID}" >/dev/null 2>&1; then
    echo "[test-all] Application exited unexpectedly. Startup log:"
    cat "${APP_LOG}"
    exit 1
  fi
  sleep 2
done

if ! is_app_ready; then
  echo "[test-all] Application did not become ready in time. Startup log:"
  cat "${APP_LOG}"
  exit 1
fi

echo "[test-all] Running Playwright API tests..."
npm --prefix tests ci
BASE_URL="${BASE_URL}" npm --prefix tests run test:api

echo "[test-all] All tests passed."
