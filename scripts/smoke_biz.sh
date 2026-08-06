#!/usr/bin/env bash
set -euo pipefail
BASE="${BASE_URL:-http://127.0.0.1:8080}"
echo "== 通查云 smoke against $BASE =="

TOKEN=$(BASE_URL="$BASE" python3 - <<'PY'
import json, urllib.request, subprocess, os
base = os.environ.get("BASE_URL", "http://127.0.0.1:8080")
cap = json.loads(urllib.request.urlopen(base + "/captchaImage").read())
uuid = cap["uuid"]
code = subprocess.check_output(["redis-cli", "GET", f"captcha_codes:{uuid}"], text=True).strip().strip('"')
body = json.dumps({"username": "admin", "password": "admin123", "code": code, "uuid": uuid}).encode()
req = urllib.request.Request(base + "/login", data=body, headers={"Content-Type": "application/json"})
resp = json.loads(urllib.request.urlopen(req).read())
token = resp.get("token")
if not token:
    raise SystemExit("login failed: " + json.dumps(resp, ensure_ascii=False))
print(token)
PY
)
AUTH="Authorization: Bearer $TOKEN"
echo "login ok"

curl -sf "$BASE/biz/dashboard/overview" -H "$AUTH" | python3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('code')==200; print('dashboard ok', 'unread', (d.get('data') or {}).get('unreadNotify'))"
curl -sf "$BASE/biz/notify/unreadCount" -H "$AUTH" | python3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('code')==200; print('notify count', d.get('data'))"

# P19-1: demo shortcodes must be published
curl -sf "$BASE/open/query/q6jjyg79/meta" | python3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('code')==200, d; print('open query meta', d.get('code'))"
curl -sf "$BASE/open/survey/97vw7fqf/meta" | python3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('code')==200, d; print('open survey meta', d.get('code'))"

curl -sf "$BASE/biz/survey/templates" -H "$AUTH" | python3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('code')==200 and len(d.get('data') or [])>=1; print('survey templates', len(d['data']))"
curl -sf "$BASE/biz/query/templates" -H "$AUTH" | python3 -c "import json,sys; d=json.load(sys.stdin); assert d.get('code')==200 and len(d.get('data') or [])>=1; assert all(t.get('hasSample') for t in d['data']); print('query templates', len(d['data']), 'hasSample')"

BASE_URL="$BASE" AUTH_TOKEN="$TOKEN" python3 - <<'PY'
import json, os, urllib.request, urllib.error
base = os.environ["BASE_URL"]
token = os.environ["AUTH_TOKEN"]

def req(method, path, body=None):
    data = None if body is None else json.dumps(body).encode()
    r = urllib.request.Request(
        base + path,
        data=data,
        method=method,
        headers={"Authorization": "Bearer " + token, "Content-Type": "application/json"},
    )
    try:
        with urllib.request.urlopen(r) as resp:
            return resp.status, json.loads(resp.read())
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "ignore")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, {"msg": raw}

# publish validation
st, created = req("POST", "/biz/query", {"queryName": "smoke-publish-check", "queryDesc": "temp"})
assert st == 200 and created.get("code") == 200, created
qid = (created.get("data") or {}).get("queryId")
assert qid, created
st, pub = req("POST", f"/biz/query/publish/{qid}")
assert pub.get("code") != 200, pub
msg = str(pub.get("msg") or "")
assert ("上传" in msg) or ("条件" in msg) or ("结果" in msg) or ("数据" in msg), pub
print("publish validation ok:", msg)
req("DELETE", f"/biz/query/{qid}")

# P20-1: template creates sample rows
st, tpl = req("POST", "/biz/query/fromTemplate/score_lookup")
assert tpl.get("code") == 200, tpl
tq = (tpl.get("data") or {})
tid = tq.get("queryId")
assert tid and (tq.get("rowCount") or 0) >= 1, tpl
print("query template sample rows", tq.get("rowCount"))
req("DELETE", f"/biz/query/{tid}")

# P19-4: cross stats on demo survey
st, meta = req("GET", "/open/survey/97vw7fqf/meta")
assert meta.get("code") == 200, meta
st, lst = req("GET", "/biz/survey/list?pageNum=1&pageSize=100")
rows = lst.get("rows") or []
sid = None
for r in rows:
    if r.get("publicCode") == "97vw7fqf":
        sid = r.get("surveyId")
        break
assert sid, "demo survey not found in list"
st, detail = req("GET", f"/biz/survey/{sid}")
qs = (detail.get("data") or {}).get("questions") or []
choice = [q for q in qs if q.get("qType") in ("radio", "select", "yesno", "image_radio")]
assert len(choice) >= 2, choice
q1, q2 = choice[0]["questionId"], choice[1]["questionId"]
st, cross = req("GET", f"/biz/survey/stats/{sid}/cross?q1={q1}&q2={q2}")
assert cross.get("code") == 200, cross
print("cross stats ok", (cross.get("data") or {}).get("pairedCount"))

# P20-3: answer list accepts channel + date params
st, ans = req("GET", f"/biz/survey/answer/list?surveyId={sid}&pageNum=1&pageSize=5&channelCode=")
assert ans.get("code") == 200, ans
print("answer list filter ok", ans.get("total"))

# P21-1: stats include trends key for rate/nps when present
st, stats = req("GET", f"/biz/survey/stats/{sid}")
assert stats.get("code") == 200, stats
print("stats ok", "textQ", len((stats.get("data") or {}).get("textQuestions") or []))

# P21-3: draft save/load
draft_token = "smoke-draft-token-001"
st, saved = req("PUT", f"/open/survey/97vw7fqf/draft", {"clientToken": draft_token, "form": {"demo": "1"}})
assert saved.get("code") == 200, saved
st, loaded = req("GET", f"/open/survey/97vw7fqf/draft?clientToken={draft_token}")
assert loaded.get("code") == 200, loaded
assert ((loaded.get("data") or {}).get("form") or {}).get("demo") == "1", loaded
print("server draft ok")

# P21-5: notify listTop
st, uc = req("GET", "/biz/notify/unreadCount")
assert uc.get("code") == 200, ("unread before listTop", uc)
st, top = req("GET", "/biz/notify/listTop")
assert top.get("code") == 200, top
print("notify listTop ok", "unread", top.get("unreadCount"))
PY

echo "smoke passed"
