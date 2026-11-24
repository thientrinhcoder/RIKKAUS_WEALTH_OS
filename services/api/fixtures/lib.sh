#!/usr/bin/env bash
# Shared helpers for the local fixture scripts. Sourced by reset.sh, load-baseline.sh and
# load-long-lists.sh; never run on its own.
#
# Every record is created through the public API, so it passes the same validation and ownership
# checks a client's request does. The only route used that a client does not use is the local-only
# development sign-in, which exists solely under the `local` Spring profile.
#
# The access token is written to a private header file inside a temporary directory that is removed
# when the script exits, whether it succeeds or fails. It is never printed, never passed on a command
# line (where `ps` would show it) and never stored anywhere else.

API_BASE_URL="${API_BASE_URL:-http://localhost:8107}"
API="${API_BASE_URL%/}/api/v1"

fixture_init() {
  local tool
  for tool in curl jq; do
    command -v "$tool" > /dev/null || { echo "This script needs $tool on the PATH." >&2; exit 1; }
  done
  FIXTURE_TMP="$(mktemp -d)"
  chmod 700 "$FIXTURE_TMP"
  trap 'rm -rf "$FIXTURE_TMP"' EXIT
}

# fixture_sign_in EMAIL DISPLAY_NAME — opens a real session for a development account. The account is
# created on first use and reused afterwards.
fixture_sign_in() {
  local body code
  body="$(jq -n --arg email "$1" --arg name "$2" '{email: $email, displayName: $name}')"
  code="$(curl -sS -o "$FIXTURE_TMP/session.json" -w '%{http_code}' -X POST "$API/auth/dev-sign-in" \
    -H 'Content-Type: application/json' --data-binary "$body")" || code=000
  if [[ "$code" != 200 ]]; then
    echo "Development sign-in at $API/auth/dev-sign-in answered HTTP $code." >&2
    echo "Start the API with --spring.profiles.active=local and point API_BASE_URL at it." >&2
    exit 1
  fi
  jq -r '"Authorization: Bearer " + .accessToken' "$FIXTURE_TMP/session.json" > "$FIXTURE_TMP/auth.header"
  FIXTURE_USER_ID="$(jq -r .user.id "$FIXTURE_TMP/session.json")"
  rm -f "$FIXTURE_TMP/session.json"
}

# api METHOD PATH [JSON] — prints the response body. Any status outside 2xx stops the script and prints
# the problem document, which never contains a credential.
api() {
  local method="$1" path="$2" data="${3:-}" code
  local args=(-sS -o "$FIXTURE_TMP/body" -w '%{http_code}' -X "$method"
    -H "@$FIXTURE_TMP/auth.header" -H 'Accept: application/json')
  if [[ -n "$data" ]]; then
    args+=(-H 'Content-Type: application/json' --data-binary "$data")
  fi
  code="$(curl "${args[@]}" "$API$path")" || code=000
  if [[ "$code" != 2* ]]; then
    echo "$method $path answered HTTP $code:" >&2
    cat "$FIXTURE_TMP/body" >&2
    echo >&2
    exit 1
  fi
  cat "$FIXTURE_TMP/body"
}

# day N — the civil date N days from today in Asia/Ho_Chi_Minh, the zone every user-entered date is
# judged in. Works with both GNU and BSD date.
day() {
  if date -d today > /dev/null 2>&1; then
    TZ=Asia/Ho_Chi_Minh date -d "$1 days" +%F
  else
    TZ=Asia/Ho_Chi_Minh date -v"$(printf '%+d' "$1")"d +%F
  fi
}

# delete_all_records — removes every asset, liability, cash-flow entry and goal of the signed-in user.
# Linked records are confirmed with confirm=true, which the API documents for exactly this case.
delete_all_records() {
  local route confirm ids id
  for route in cash-flow-entries goals liabilities assets; do
    confirm=""
    if [[ "$route" == liabilities || "$route" == assets ]]; then confirm="?confirm=true"; fi
    ids="$(api GET "/$route" | jq -r '.[].id')"
    for id in $ids; do
      api DELETE "/$route/$id$confirm" > /dev/null
    done
  done
}

# count PATH — the number of elements of a list route.
count() {
  api GET "$1" | jq length
}
