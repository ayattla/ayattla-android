#!/bin/bash
# Fetches the approved sources and builds what the app ships but git does not hold:
#   app/src/main/assets/content-v1.sqlite  (Quran, azkar, reciters, tasbih; same tool as the iOS app)
#   app/src/main/res/font/*.ttf            (Amiri Quran, Amiri, IBM Plex Sans Arabic; OFL 1.1)
# Religious text is copied byte for byte; the Quran text is compared with Tanzil.
set -euo pipefail
cd "$(dirname "$0")/.."
SRC=build/content-src
FONT=app/src/main/res/font
LIC=app/src/main/assets/licenses
mkdir -p "$SRC" app/src/main/assets "$FONT" "$LIC"
get() { curl -sSfL --retry 3 -m 60 "$1" -o "$2"; }

echo "== sources"
get https://api.alquran.cloud/v1/quran/quran-uthmani "$SRC/quran-uthmani.json"
rm -rf "$SRC/azkar-db"; git clone -q --depth 1 https://github.com/osamayy/azkar-db "$SRC/azkar-db"
AZ=$(git -C "$SRC/azkar-db" rev-parse HEAD)
get https://ayattla.com/api/meta "$SRC/meta.json"
get https://ayattla.com/tools/tasbih "$SRC/tasbih.html"
get https://ayattla.com/tools/prayer-guide "$SRC/prayer-guide.html"
get https://ayattla.com/api/duas "$SRC/duas.json"
get https://ayattla.com/api/ruqya "$SRC/ruqya.json"
get https://ayattla.com/tools/asma-ul-husna "$SRC/asma.html"
# Ayat by topic: the list page, then one page per topic it links to (same as the iOS app).
get https://ayattla.com/ayat/ "$SRC/ayat.html"
rm -rf "$SRC/ayat"; mkdir -p "$SRC/ayat"
for slug in $(sed -n '/<nav class="tools"/,/<\/nav>/p' "$SRC/ayat.html" | grep -o 'href="/ayat/[a-z0-9_-]\+"' | sed 's|href="/ayat/||;s|"||' | sort -u); do
  get "https://ayattla.com/ayat/$slug" "$SRC/ayat/$slug.html"
done
echo "topics: $(ls "$SRC/ayat" | wc -l)"
# WhatsApp statuses by occasion: the list page, then each occasion page it links to.
get https://ayattla.com/status/ "$SRC/status.html"
rm -rf "$SRC/status"; mkdir -p "$SRC/status"
for slug in $(grep -o 'href="/status/[a-z0-9-]\+"' "$SRC/status.html" | sed 's|href="/status/||;s|"||' | sort -u); do
  [ "$slug" = editor ] && continue
  get "https://ayattla.com/status/$slug" "$SRC/status/$slug.html" || echo "status $slug failed"
done
echo "status pages: $(ls "$SRC/status" | wc -l)"
get "https://tanzil.net/pub/download/index.php?marks=true&sajdah=true&rub=true&tatweel=false&quranType=uthmani&outType=txt-2&agree=true" "$SRC/tanzil.txt" || echo "tanzil download failed"

echo "== pack"
rm -f app/src/main/assets/content-v1.sqlite
python3 tools/content/normalize_azkar.py "$SRC/azkar-db/azkar.json" --source "osamayy/azkar-db@$AZ" --out "$SRC/azkar.json"
python3 tools/content/build_content_pack.py --version 1 --quran "$SRC/quran-uthmani.json" --azkar "$SRC/azkar.json" \
  --reciters "$SRC/meta.json" --tasbih-html "$SRC/tasbih.html" \
  --guide-html "$SRC/prayer-guide.html" --duas "$SRC/duas.json" --ruqya "$SRC/ruqya.json" --asma-html "$SRC/asma.html" --topics-html "$SRC/ayat.html" --topics-dir "$SRC/ayat" --status-html "$SRC/status.html" --status-dir "$SRC/status" --out app/src/main/assets/content-v1.sqlite
python3 - <<'PY'
import sqlite3
db = sqlite3.connect("app/src/main/assets/content-v1.sqlite")
q = lambda s: db.execute(s).fetchone()[0]
n = q("select count(*) from ayah")
print("surahs", q("select count(*) from surah"), "ayat", n, "reciters", q("select count(*) from reciter"),
      "azkar", q("select count(*) from azkar"), "categories", q("select count(*) from azkar_category"))
import json
for k in ("topics", "status", "duas", "prayer_guide"):
    r = db.execute("select value from meta where key=?", (k,)).fetchone()
    print(k, "missing" if not r else len(json.loads(r[0]) if k in ("topics", "status") else r[0]))
assert n == 6236, n
PY
if [ -s "$SRC/tanzil.txt" ]; then python3 tools/content/compare_tanzil.py app/src/main/assets/content-v1.sqlite "$SRC/tanzil.txt"; fi
mkdir -p build && python3 tools/content/verify_pack.py app/src/main/assets/content-v1.sqlite build/content-report.md

echo "== fonts (OFL 1.1)"
GF=https://github.com/google/fonts/raw/main/ofl
get $GF/amiriquran/AmiriQuran-Regular.ttf $FONT/amiri_quran.ttf
get $GF/amiri/Amiri-Regular.ttf $FONT/amiri.ttf
for w in Regular Medium SemiBold Bold; do
  get $GF/ibmplexsansarabic/IBMPlexSansArabic-$w.ttf "$FONT/plex_$(echo $w | tr 'A-Z' 'a-z').ttf"
done
get $GF/amiriquran/OFL.txt $LIC/AmiriQuran-OFL.txt
get $GF/amiri/OFL.txt $LIC/Amiri-OFL.txt
get $GF/ibmplexsansarabic/OFL.txt $LIC/IBMPlexSansArabic-OFL.txt
ls -l app/src/main/assets/content-v1.sqlite $FONT
