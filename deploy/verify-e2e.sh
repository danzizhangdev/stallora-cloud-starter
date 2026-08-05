#!/usr/bin/env bash
# End-to-end probe against a running stack. Usage: verify-e2e.sh <base-url>
set -euo pipefail
BASE="${1:?usage: verify-e2e.sh <base-url>}"
pass() { echo "  ✅ $1"; }
fail() { echo "  ❌ $1"; exit 1; }

echo "[1/5] unauthenticated request is rejected"
code=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/api/sample/notes/1")
[ "$code" = "401" ] && pass "401 without token" || fail "expected 401, got $code"

echo "[2/5] token issuance"
TOKEN=$(curl -s -X POST "$BASE/api/auth/token" -H 'Content-Type: application/json' \
        -d '{"username":"alice","password":"password-alice"}' | jq -r '.data.token')
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && pass "token issued" || fail "no token in response"

echo "[3/5] authenticated write through gateway carries trace id"
RESP_HEADERS=$(mktemp); RESP_BODY=$(mktemp)
curl -s -D "$RESP_HEADERS" -o "$RESP_BODY" -X POST "$BASE/api/sample/notes" \
     -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d '{"text":"cold start probe"}'
grep -qi '^x-trace-id:' "$RESP_HEADERS" && pass "X-Trace-Id header present" || fail "no X-Trace-Id header"
NOTE_ID=$(jq -r '.data.id' "$RESP_BODY")
[ "$NOTE_ID" != "null" ] && pass "note $NOTE_ID created" || fail "note creation failed: $(cat "$RESP_BODY")"

echo "[4/5] outbox round-trip (write -> relay -> kafka -> idempotent consumer)"
for i in $(seq 1 30); do
  COUNT=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/sample/notes/$NOTE_ID/events" | jq -r '.data.count')
  [ "$COUNT" = "1" ] && break
  sleep 2
done
[ "$COUNT" = "1" ] && pass "event consumed exactly once" || fail "outbox event not consumed within 60s (count=$COUNT)"

echo "[5/5] circuit-breaker demo endpoint responds"
STATUS=$(curl -s -H "Authorization: Bearer $TOKEN" "$BASE/api/sample/key-info" | jq -r '.code')
[ "$STATUS" = "OK" ] && pass "key-info reachable" || fail "key-info failed"

echo "E2E PASS"
