#!/usr/bin/env python3
"""Update version.json for a new game or launcher build.

Game:     python3 tools/publish.py game Otchlan.apk 1.3 4 "Zmiana 1" "Zmiana 2" ...
Launcher: python3 tools/publish.py launcher OtchlanLauncher.apk 1.1 2
News:     python3 tools/publish.py news "Tytuł" "Tekst"

Then: git add -A && git commit -m "..." && git push
The launcher reads version.json from the main branch, so the push is the release.
"""
import hashlib, json, os, shutil, sys, datetime

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VJ = os.path.join(ROOT, "version.json")
today = datetime.date.today().strftime("%d.%m.%Y")

def sha(p):
    return hashlib.sha256(open(p, "rb").read()).hexdigest()

data = json.load(open(VJ, encoding="utf-8"))
kind = sys.argv[1]

if kind == "game":
    src, name, code, items = sys.argv[2], sys.argv[3], int(sys.argv[4]), sys.argv[5:]
    assert code > data["game"]["versionCode"], "versionCode must grow"
    rel = f"releases/Otchlan-{name}.apk"
    shutil.copy(src, os.path.join(ROOT, rel))
    p = os.path.join(ROOT, rel)
    data["game"] = {"package": "ua.dima.otchlan", "versionName": name, "versionCode": code,
                    "apk": rel, "sha256": sha(p), "size": os.path.getsize(p), "date": today}
    data["changelog"].insert(0, {"version": name, "date": today, "items": items})
elif kind == "launcher":
    src, name, code = sys.argv[2], sys.argv[3], int(sys.argv[4])
    assert code > data["launcher"]["versionCode"], "versionCode must grow"
    rel = f"launcher/OtchlanLauncher-{name}.apk"
    shutil.copy(src, os.path.join(ROOT, rel))
    shutil.copy(src, os.path.join(ROOT, "OtchlanLauncher.apk"))
    p = os.path.join(ROOT, rel)
    data["launcher"] = {"versionName": name, "versionCode": code, "apk": rel,
                        "sha256": sha(p), "size": os.path.getsize(p)}
elif kind == "news":
    data["news"].insert(0, {"date": today, "title": sys.argv[2], "text": sys.argv[3]})
    data["news"] = data["news"][:6]
else:
    sys.exit(__doc__)

json.dump(data, open(VJ, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
print(json.dumps(data["game"], ensure_ascii=False))
