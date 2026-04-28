#!/usr/bin/env bash
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8081}"
ADMIN_REALM="${ADMIN_REALM:-master}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin}"
REALM_NAME="${REALM_NAME:-FSA}"
CLIENT_ID="${CLIENT_ID:-fsa-client}"
PREFERRED_CLIENT_SECRET="${CLIENT_SECRET:-fsa-client-secret}"
GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-}"
GOOGLE_CLIENT_SECRET="${GOOGLE_CLIENT_SECRET:-}"
FORCE_RECREATE_REALM="${FORCE_RECREATE_REALM:-true}"

# username:password:role:firstName:lastName
USERS=(
  "admin@posam.sk:admin123:ADMIN:Workshop:Admin"
  "user@posam.sk:user123:USER:Workshop:User"
)

ROLES=("ADMIN" "USER")

log() {
  printf '[keycloak-setup] %s\n' "$*"
}

require_tools() {
  local missing=0
  for cmd in curl jq; do
    if ! command -v "$cmd" >/dev/null 2>&1; then
      printf 'Missing required command: %s\n' "$cmd" >&2
      missing=1
    fi
  done
  if [[ "$missing" -ne 0 ]]; then
    exit 1
  fi
}

http_code() {
  local method="$1"
  local url="$2"
  local data="${3:-}"

  if [[ -n "$data" ]]; then
    curl -sS -o /dev/null -w '%{http_code}' -X "$method" "$url" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H 'Content-Type: application/json' \
      --data "$data"
  else
    curl -sS -o /dev/null -w '%{http_code}' -X "$method" "$url" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H 'Content-Type: application/json'
  fi
}

api_get() {
  local path="$1"
  curl -fsS "${KEYCLOAK_URL}${path}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H 'Content-Type: application/json'
}

api_post() {
  local path="$1"
  local data="$2"
  curl -fsS -X POST "${KEYCLOAK_URL}${path}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H 'Content-Type: application/json' \
    --data "$data" >/dev/null
}

api_put() {
  local path="$1"
  local data="$2"
  curl -fsS -X PUT "${KEYCLOAK_URL}${path}" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H 'Content-Type: application/json' \
    --data "$data" >/dev/null
}

api_delete() {
  local path="$1"
  local data="${2:-}"
  if [[ -n "$data" ]]; then
    curl -fsS -X DELETE "${KEYCLOAK_URL}${path}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H 'Content-Type: application/json' \
      --data "$data" >/dev/null
  else
    curl -fsS -X DELETE "${KEYCLOAK_URL}${path}" \
      -H "Authorization: Bearer ${ACCESS_TOKEN}" \
      -H 'Content-Type: application/json' >/dev/null
  fi
}

get_admin_token() {
  local token
  token="$(curl -fsS -X POST "${KEYCLOAK_URL}/realms/${ADMIN_REALM}/protocol/openid-connect/token" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    --data-urlencode 'client_id=admin-cli' \
    --data-urlencode 'grant_type=password' \
    --data-urlencode "username=${ADMIN_USER}" \
    --data-urlencode "password=${ADMIN_PASSWORD}" | jq -r '.access_token')"

  if [[ -z "$token" || "$token" == "null" ]]; then
    printf 'Unable to obtain admin access token. Check KEYCLOAK_URL/ADMIN_USER/ADMIN_PASSWORD.\n' >&2
    exit 1
  fi

  ACCESS_TOKEN="$token"
}

ensure_realm() {
  local realm_url="${KEYCLOAK_URL}/admin/realms/${REALM_NAME}"
  local status
  status="$(http_code GET "$realm_url")"

  if [[ "$status" == "200" && "$FORCE_RECREATE_REALM" == "true" ]]; then
    log "Realm ${REALM_NAME} exists, deleting and recreating."
    api_delete "/admin/realms/${REALM_NAME}"
    status="404"
  fi

  if [[ "$status" == "404" ]]; then
    log "Creating realm ${REALM_NAME}."
    api_post "/admin/realms" "$(jq -n --arg realm "$REALM_NAME" '{realm:$realm, enabled:true}')"
  elif [[ "$status" == "200" ]]; then
    log "Realm ${REALM_NAME} already exists, reusing."
  else
    printf 'Unexpected realm status code: %s\n' "$status" >&2
    exit 1
  fi
}

ensure_realm_role() {
  local role_name="$1"
  local role_url="${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/roles/${role_name}"
  local status
  status="$(http_code GET "$role_url")"

  if [[ "$status" == "404" ]]; then
    log "Creating role ${role_name}."
    api_post "/admin/realms/${REALM_NAME}/roles" "$(jq -n --arg name "$role_name" '{name:$name}')"
  elif [[ "$status" == "200" ]]; then
    log "Role ${role_name} already exists."
  else
    printf 'Unexpected role status code for %s: %s\n' "$role_name" "$status" >&2
    exit 1
  fi
}

get_client_uuid() {
  api_get "/admin/realms/${REALM_NAME}/clients?clientId=${CLIENT_ID}" | jq -r '.[0].id // empty'
}

ensure_client() {
  local client_uuid
  client_uuid="$(get_client_uuid)"

  local payload
  payload="$(jq -n \
    --arg clientId "$CLIENT_ID" \
    --arg secret "$PREFERRED_CLIENT_SECRET" \
    '{
      clientId: $clientId,
      name: $clientId,
      enabled: true,
      protocol: "openid-connect",
      publicClient: false,
      clientAuthenticatorType: "client-secret",
      secret: $secret,
      standardFlowEnabled: true,
      directAccessGrantsEnabled: true,
      serviceAccountsEnabled: false,
      implicitFlowEnabled: false,
      redirectUris: ["*"],
      webOrigins: ["*"],
      attributes: {
        "post.logout.redirect.uris": "*"
      }
    }')"

  if [[ -z "$client_uuid" ]]; then
    log "Creating client ${CLIENT_ID} (confidential)."
    api_post "/admin/realms/${REALM_NAME}/clients" "$payload"
    client_uuid="$(get_client_uuid)"
  else
    log "Updating existing client ${CLIENT_ID}."
    api_put "/admin/realms/${REALM_NAME}/clients/${client_uuid}" "$payload"
  fi

  if [[ -z "$client_uuid" ]]; then
    printf 'Client %s was not found after create/update.\n' "$CLIENT_ID" >&2
    exit 1
  fi

  CLIENT_UUID="$client_uuid"
  CLIENT_SECRET_VALUE="$(api_get "/admin/realms/${REALM_NAME}/clients/${CLIENT_UUID}/client-secret" | jq -r '.value')"
}

ensure_google_identity_provider() {
  if [[ -z "$GOOGLE_CLIENT_ID" || -z "$GOOGLE_CLIENT_SECRET" ]]; then
    log "Skipping Google identity provider setup because GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET were not provided."
    return
  fi

  local idp_url="${KEYCLOAK_URL}/admin/realms/${REALM_NAME}/identity-provider/instances/google"
  local status
  status="$(http_code GET "$idp_url")"

  local payload
  payload="$(jq -n \
    --arg clientId "$GOOGLE_CLIENT_ID" \
    --arg clientSecret "$GOOGLE_CLIENT_SECRET" \
    '{
      alias: "google",
      providerId: "google",
      enabled: true,
      trustEmail: true,
      storeToken: false,
      addReadTokenRoleOnCreate: false,
      firstBrokerLoginFlowAlias: "first broker login",
      config: {
        syncMode: "IMPORT",
        clientId: $clientId,
        clientSecret: $clientSecret,
        useJwksUrl: "true"
      }
    }')"

  if [[ "$status" == "404" ]]; then
    log "Creating Google identity provider."
    api_post "/admin/realms/${REALM_NAME}/identity-provider/instances" "$payload"
  elif [[ "$status" == "200" ]]; then
    log "Updating Google identity provider."
    api_put "/admin/realms/${REALM_NAME}/identity-provider/instances/google" "$payload"
  else
    printf 'Unexpected Google identity provider status code: %s\n' "$status" >&2
    exit 1
  fi
}

get_user_id() {
  local username="$1"
  local encoded_username
  encoded_username="$(jq -rn --arg v "$username" '$v|@uri')"
  api_get "/admin/realms/${REALM_NAME}/users?username=${encoded_username}&exact=true" | jq -r '.[0].id // empty'
}

set_user_password() {
  local user_id="$1"
  local password="$2"

  api_put "/admin/realms/${REALM_NAME}/users/${user_id}/reset-password" \
    "$(jq -n --arg value "$password" '{type:"password", temporary:false, value:$value}')"
}

set_user_single_role() {
  local user_id="$1"
  local role_name="$2"

  local current_roles
  current_roles="$(api_get "/admin/realms/${REALM_NAME}/users/${user_id}/role-mappings/realm")"
  if [[ "$(jq 'length' <<<"$current_roles")" -gt 0 ]]; then
    api_delete "/admin/realms/${REALM_NAME}/users/${user_id}/role-mappings/realm" "$current_roles"
  fi

  local role_repr
  role_repr="$(api_get "/admin/realms/${REALM_NAME}/roles/${role_name}")"
  api_post "/admin/realms/${REALM_NAME}/users/${user_id}/role-mappings/realm" "[$role_repr]"
}

resolve_email() {
  local username="$1"
  if [[ "$username" == *"@"* ]]; then
    printf '%s' "$username"
  else
    printf '%s@fsa.local' "$username"
  fi
}

ensure_user() {
  local username="$1"
  local password="$2"
  local role="$3"
  local first_name="$4"
  local last_name="$5"
  local email
  email="$(resolve_email "$username")"

  local user_id
  user_id="$(get_user_id "$username")"

  if [[ -z "$user_id" ]]; then
    log "Creating user ${username}."
    api_post "/admin/realms/${REALM_NAME}/users" \
      "$(jq -n \
        --arg username "$username" \
        --arg email "$email" \
        --arg firstName "$first_name" \
        --arg lastName "$last_name" \
        '{username:$username, email:$email, firstName:$firstName, lastName:$lastName, enabled:true, emailVerified:true, requiredActions:[]}')"
    user_id="$(get_user_id "$username")"
  else
    log "User ${username} already exists, updating profile/password/role mapping."
  fi

  if [[ -z "$user_id" ]]; then
    printf 'Failed to resolve user id for %s\n' "$username" >&2
    exit 1
  fi

  api_put "/admin/realms/${REALM_NAME}/users/${user_id}" \
    "$(jq -n \
      --arg username "$username" \
      --arg email "$email" \
      --arg firstName "$first_name" \
      --arg lastName "$last_name" \
      '{username:$username, email:$email, firstName:$firstName, lastName:$lastName, enabled:true, emailVerified:true, requiredActions:[]}')"

  set_user_password "$user_id" "$password"
  set_user_single_role "$user_id" "$role"
}

print_summary() {
  cat <<SUMMARY

Setup completed.

Realm: ${REALM_NAME}
Client ID: ${CLIENT_ID}
Client secret: ${CLIENT_SECRET_VALUE}

Users:
- admin@posam.sk / admin123 / ADMIN
- user@posam.sk / user123 / USER

Useful env overrides:
- KEYCLOAK_URL (default: http://localhost:8081)
- ADMIN_USER (default: admin)
- ADMIN_PASSWORD (default: admin)
- REALM_NAME (default: FSA)
- CLIENT_ID (default: fsa-client)
- CLIENT_SECRET (default: fsa-client-secret, Keycloak may keep existing one for existing client)
- GOOGLE_CLIENT_ID / GOOGLE_CLIENT_SECRET (optional, configures Google broker login)
- FORCE_RECREATE_REALM=true|false (default: true)

Google redirect URI to register in Google Cloud Console:
- ${KEYCLOAK_URL}/realms/${REALM_NAME}/broker/google/endpoint
SUMMARY
}

main() {
  require_tools
  get_admin_token
  ensure_realm

  for role in "${ROLES[@]}"; do
    ensure_realm_role "$role"
  done

  ensure_client
  ensure_google_identity_provider

  for entry in "${USERS[@]}"; do
    IFS=':' read -r username password role first_name last_name <<<"$entry"
    ensure_user "$username" "$password" "$role" "$first_name" "$last_name"
  done

  print_summary
}

main "$@"
