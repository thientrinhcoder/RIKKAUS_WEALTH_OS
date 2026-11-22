#!/usr/bin/env bash
# Executes the GH-41 auth contract cases against a running API. Token values are redacted in output:
# a cookie or token is shown as <len:N sha:XXXXXXXX> so rotation is still visible.
set -u
B=${BASE:-http://localhost:8380}/api/v1/auth
O=http://localhost:8081
W=(-H "X-Rikkaus-Client: web" -H "Origin: $O")
J=(-H "Content-Type: application/json")
T=$(mktemp -d)

red() { sed -E 's/(rikkaus_refresh=)([A-Za-z0-9_-]{10,})/\1<redacted>/g; s/("(accessToken|refreshToken)":")[^"]+"/\1<redacted>"/g'; }
fp() { [ -z "$1" ] && echo "<empty>" || echo "<len:${#1} sha:$(printf %s "$1" | shasum -a 256 | cut -c1-8)>"; }
cookie_of() { grep -i '^set-cookie: rikkaus_refresh=' "$1" | sed -E 's/^[^=]*=([^;]*).*/\1/' | tr -d '\r'; }
show() { echo "\$ $1"; sed -n '1p' "$2"; grep -iE '^(set-cookie|access-control-[a-z-]+|vary):' "$2" | red; echo; red < "$3"; echo; echo; }
post() { # name, path, body, extra curl args...
  local n=$1 p=$2 b=$3; shift 3
  if [ -n "$b" ]; then curl -s -D $T/$n.h -o $T/$n.b -X POST "$B$p" "${J[@]}" -d "$b" "$@"; else curl -s -D $T/$n.h -o $T/$n.b -X POST "$B$p" "$@"; fi
}
psql_q() { docker exec rikkaus_wealth_os-postgres-1 psql -U rikkaus -d rikkaus -At -c "$1"; }

echo "## TC-01 dev sign-in, web mode, new user"
psql_q "delete from wealth.users where google_subject like 'local-dev|%';" >/dev/null
post tc01 /dev-sign-in '{"email":"an.nguyen@example.com","displayName":"An Nguyễn"}' "${W[@]}"
show "POST /dev-sign-in (web)" $T/tc01.h $T/tc01.b
C1=$(cookie_of $T/tc01.h); A1=$(jq -r .accessToken $T/tc01.b)
echo "cookie=$(fp "$C1") body.refreshToken present: $(jq 'has("refreshToken")' $T/tc01.b)"
echo "users row: $(psql_q "select google_subject, email, display_name from wealth.users where google_subject like 'local-dev|%';")"

echo; echo "## TC-02 dev sign-in again, same email"
post tc02 /dev-sign-in '{"email":"an.nguyen@example.com","displayName":"An Nguyễn"}' "${W[@]}"
echo "status $(sed -n 1p $T/tc02.h | tr -d '\r'); same user id: $( [ "$(jq -r .user.id $T/tc01.b)" = "$(jq -r .user.id $T/tc02.b)" ] && echo yes || echo no ); rows: $(psql_q "select count(*) from wealth.users where google_subject='local-dev|an.nguyen@example.com';")"

echo; echo "## TC-03 access token works"
curl -s -D $T/tc03.h -o $T/tc03.b "$B/session" -H "Authorization: Bearer $A1"
show "GET /session (bearer from TC-01)" $T/tc03.h $T/tc03.b

echo "## TC-04 rotation via cookie"
post tc04 /refresh "" "${W[@]}" -H "Cookie: rikkaus_refresh=$C1"
show "POST /refresh (web, cookie, no body)" $T/tc04.h $T/tc04.b
C4=$(cookie_of $T/tc04.h)
echo "old cookie=$(fp "$C1") new cookie=$(fp "$C4") differs: $( [ "$C1" != "$C4" ] && echo yes || echo no ); body.refreshToken present: $(jq 'has("refreshToken")' $T/tc04.b)"

echo; echo "## TC-05 reuse of rotated cookie revokes the session"
post tc05a /refresh "" "${W[@]}" -H "Cookie: rikkaus_refresh=$C1"
show "POST /refresh with OLD cookie" $T/tc05a.h $T/tc05a.b
post tc05b /refresh "" "${W[@]}" -H "Cookie: rikkaus_refresh=$C4"
show "POST /refresh with the newer cookie after reuse" $T/tc05b.h $T/tc05b.b
SID=$(psql_q "select session_id from wealth.refresh_tokens rt join wealth.users u on u.id=rt.user_id where u.google_subject='local-dev|an.nguyen@example.com' order by rt.issued_at limit 1;" 2>/dev/null)
echo "chain of first session: $(psql_q "select count(*) || ' tokens, ' || count(revoked_at) || ' revoked' from wealth.refresh_tokens where session_id='$SID';" 2>/dev/null)"

echo; echo "## TC-06 logout via cookie clears the cookie"
post tc06s /dev-sign-in '{"email":"an.nguyen@example.com","displayName":"An Nguyễn"}' "${W[@]}"; C6=$(cookie_of $T/tc06s.h)
post tc06 /logout "" "${W[@]}" -H "Cookie: rikkaus_refresh=$C6"
show "POST /logout (web, cookie)" $T/tc06.h $T/tc06.b
post tc06r /refresh "" "${W[@]}" -H "Cookie: rikkaus_refresh=$C6"
show "POST /refresh with the logged-out cookie" $T/tc06r.h $T/tc06r.b

echo "## TC-07 CSRF: cookie without web header"
post tc07s /dev-sign-in '{"email":"an.nguyen@example.com","displayName":"An Nguyễn"}' "${W[@]}"; C7=$(cookie_of $T/tc07s.h)
post tc07 /refresh "" -H "Cookie: rikkaus_refresh=$C7"
show "POST /refresh (cookie only, no X-Rikkaus-Client)" $T/tc07.h $T/tc07.b
post tc07ok /refresh "" "${W[@]}" -H "Cookie: rikkaus_refresh=$C7"
echo "follow-up proper web refresh with same cookie: $(sed -n 1p $T/tc07ok.h | tr -d '\r') (token was not consumed)"; C7b=$(cookie_of $T/tc07ok.h)

echo; echo "## TC-08 CSRF: foreign Origin"
post tc08 /refresh "" -H "X-Rikkaus-Client: web" -H "Origin: https://evil.example" -H "Cookie: rikkaus_refresh=$C7b"
show "POST /refresh (web header, Origin https://evil.example)" $T/tc08.h $T/tc08.b
post tc08ok /refresh "" "${W[@]}" -H "Cookie: rikkaus_refresh=$C7b"
echo "follow-up proper web refresh with same cookie: $(sed -n 1p $T/tc08ok.h | tr -d '\r')"; C8=$(cookie_of $T/tc08ok.h)

echo; echo "## TC-09 logout CSRF: cookie without header"
post tc09 /logout "" -H "Cookie: rikkaus_refresh=$C8"
show "POST /logout (cookie only)" $T/tc09.h $T/tc09.b
post tc09ok /refresh "" "${W[@]}" -H "Cookie: rikkaus_refresh=$C8"
echo "session still renewable: $(sed -n 1p $T/tc09ok.h | tr -d '\r')"

echo; echo "## TC-10 neither body token nor cookie"
post tc10a /refresh '{}' "${W[@]}"; show "POST /refresh {} (web, no cookie)" $T/tc10a.h $T/tc10a.b
post tc10b /logout '{}'; show "POST /logout {} (native)" $T/tc10b.h $T/tc10b.b

echo "## TC-11 CORS preflight, allowed origin, auth route"
curl -s -D $T/tc11.h -o $T/tc11.b -X OPTIONS "$B/refresh" -H "Origin: $O" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: content-type,x-rikkaus-client"
show "OPTIONS /auth/refresh (Origin $O)" $T/tc11.h $T/tc11.b

echo "## TC-12 CORS preflight, unknown origin"
curl -s -D $T/tc12.h -o $T/tc12.b -X OPTIONS "$B/refresh" -H "Origin: https://evil.example" -H "Access-Control-Request-Method: POST" -H "Access-Control-Request-Headers: content-type,x-rikkaus-client"
show "OPTIONS /auth/refresh (Origin https://evil.example)" $T/tc12.h $T/tc12.b

echo "## TC-13 CORS on a non-auth route stays credential-free"
curl -s -D $T/tc13.h -o $T/tc13.b -X OPTIONS "${B%/auth}/assets" -H "Origin: $O" -H "Access-Control-Request-Method: GET" -H "Access-Control-Request-Headers: authorization"
show "OPTIONS /api/v1/assets (Origin $O)" $T/tc13.h $T/tc13.b

echo "## TC-14 native body flow unchanged"
post tc14 /dev-sign-in '{"email":"binh.tran@example.com","displayName":"Bình Trần"}'
show "POST /dev-sign-in (native)" $T/tc14.h $T/tc14.b
R14=$(jq -r .refreshToken $T/tc14.b)
post tc14r /refresh "{\"refreshToken\":\"$R14\"}"
show "POST /refresh (native, body token)" $T/tc14r.h $T/tc14r.b
R14b=$(jq -r .refreshToken $T/tc14r.b); echo "rotated body token: $(fp "$R14") -> $(fp "$R14b")"
post tc14l /logout "{\"refreshToken\":\"$R14b\"}"
show "POST /logout (native, body token)" $T/tc14l.h $T/tc14l.b

echo "## TC-15 native reuse still revokes"
post tc15s /dev-sign-in '{"email":"binh.tran@example.com","displayName":"Bình Trần"}'; R15=$(jq -r .refreshToken $T/tc15s.b)
post tc15a /refresh "{\"refreshToken\":\"$R15\"}"; R15b=$(jq -r .refreshToken $T/tc15a.b)
post tc15b /refresh "{\"refreshToken\":\"$R15\"}"; show "POST /refresh reusing the first body token" $T/tc15b.h $T/tc15b.b
post tc15c /refresh "{\"refreshToken\":\"$R15b\"}"; show "POST /refresh with the successor after reuse" $T/tc15c.h $T/tc15c.b

echo "## TC-16 dev sign-in validation"
post tc16 /dev-sign-in '{"email":"not-an-email"}'; show "POST /dev-sign-in {email: not-an-email}" $T/tc16.h $T/tc16.b

echo "## TC-19 Google sign-in route still validates"
post tc19 /google '{}' "${W[@]}"; show "POST /google {} (web)" $T/tc19.h $T/tc19.b

rm -rf "$T"
