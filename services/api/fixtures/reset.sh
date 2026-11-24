#!/usr/bin/env bash
# Removes every asset, liability, cash-flow entry and goal of one development account, through the API.
#
#   ./reset.sh [email]                      default email: an.nguyen@example.com
#   API_BASE_URL=http://localhost:8107 ./reset.sh long.list@example.com
#
# The API has no route that deletes the declared exchange rate, onboarding progress or feedback, so
# those stay. Use a new email for a user who has never declared a rate.
set -euo pipefail
# shellcheck source=lib.sh
source "$(cd "$(dirname "$0")" && pwd)/lib.sh"

EMAIL="${1:-an.nguyen@example.com}"

fixture_init
fixture_sign_in "$EMAIL" "${FIXTURE_DISPLAY_NAME:-An Nguyễn}"
delete_all_records

printf 'Reset %s at %s: assets %s, liabilities %s, cash-flow entries %s, goals %s.\n' "$EMAIL" "$API_BASE_URL" \
  "$(count /assets)" "$(count /liabilities)" "$(count /cash-flow-entries)" "$(count /goals)"
echo "Kept (no delete route): the declared exchange rate, onboarding progress and feedback."
