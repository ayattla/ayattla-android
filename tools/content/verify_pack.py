#!/usr/bin/env python3
"""Structural check of the content pack against the standard Madinah mushaf (Hafs, Kufan count):
114 surahs, their ayah counts, 6,236 ayat, 604 pages in order, the 30 juz starts, the basmala.
Also reports azkar entries without a source reference (shown in the app as such, never invented).
Exit 1 on any Quran difference. Usage: verify_pack.py pack.sqlite [report.md]
"""
import json
import sqlite3
import sys

COUNTS = [7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135, 112, 78, 118, 64, 77,
          227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55,
          78, 96, 29, 22, 24, 13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42, 29, 19, 36,
          25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6]
JUZ = [(1, 1), (2, 142), (2, 253), (3, 93), (4, 24), (4, 148), (5, 82), (6, 111), (7, 88), (8, 41), (9, 93), (11, 6), (12, 53),
       (15, 1), (17, 1), (18, 75), (21, 1), (23, 1), (25, 21), (27, 56), (29, 46), (33, 31), (36, 28), (39, 32), (41, 47),
       (46, 1), (51, 31), (58, 1), (67, 1), (78, 1)]
assert len(COUNTS) == 114 and sum(COUNTS) == 6236

db = sqlite3.connect(f"file:{sys.argv[1]}?mode=ro", uri=True)
errors, lines = [], []
surahs = {sid: n for sid, n in db.execute("SELECT id, ayah_count FROM surah")}
rows = db.execute("SELECT surah, ayah, page, juz, text_uthmani FROM ayah ORDER BY surah, ayah").fetchall()
if len(surahs) != 114: errors.append(f"surahs: {len(surahs)}")
if len(rows) != 6236: errors.append(f"ayat: {len(rows)}")
per = {}
for s, a, *_ in rows: per[s] = per.get(s, 0) + 1
for i, n in enumerate(COUNTS, 1):
    if per.get(i) != n or surahs.get(i) != n: errors.append(f"surah {i}: {per.get(i)} ayat in rows, {surahs.get(i)} in surah table, expected {n}")
empty = [(s, a) for s, a, _, _, t in rows if not t or not t.strip()]
if empty: errors.append(f"empty ayat: {empty[:5]}")
pages = [p for _, _, p, _, _ in rows]
if None in pages: errors.append("ayat without page")
else:
    if pages[0] != 1 or pages[-1] != 604 or any(b < a for a, b in zip(pages, pages[1:])): errors.append("pages not 1..604 in order")
    if len(set(pages)) != 604: errors.append(f"distinct pages: {len(set(pages))}")
juz_of = {(s, a): j for s, a, _, j, _ in rows}
for j, start in enumerate(JUZ, 1):
    if juz_of.get(start) != j: errors.append(f"juz {j} should start at {start}, pack says {juz_of.get(start)}")
basmala = db.execute("SELECT count(*) FROM surah WHERE basmala = 1").fetchone()[0]
if basmala != 112: errors.append(f"basmala on {basmala} surahs, expected 112 (all but 1 and 9)")

lines.append("# تقرير فحص حزمة المحتوى")
lines.append("")
lines.append(f"- السور: {len(surahs)} (المتوقع 114)")
lines.append(f"- الآيات: {len(rows)} (المتوقع 6236)، وعدد آيات كل سورة مطابق للعد الكوفي (رواية حفص)")
lines.append(f"- الصفحات: {len(set(p for p in pages if p))} صفحة مرتبة من 1 إلى 604 (مصحف المدينة)")
lines.append(f"- بدايات الأجزاء الثلاثين: {'مطابقة' if not any('juz' in e for e in errors) else 'فيها اختلاف'}")
lines.append(f"- البسملة في رأس {basmala} سورة (كل السور عدا الفاتحة والتوبة)")
lines.append("")
az = db.execute("SELECT c.title, count(*), sum(CASE WHEN a.reference IS NULL OR trim(a.reference) = '' THEN 1 ELSE 0 END) "
                "FROM azkar a JOIN azkar_category c ON c.id = a.category GROUP BY c.id ORDER BY c.id").fetchall()
total = sum(r[1] for r in az); missing = sum(r[2] for r in az)
lines.append(f"## الأذكار: {total} ذكرا، منها {missing} بلا مرجع تفصيلي في مصدر البيانات")
lines.append("يظهر في التطبيق بجانبها «من حصن المسلم، المرجع التفصيلي غير مذكور في المصدر» ولا يُكتب مرجع من عندنا.")
lines.append("")
for t, n, m in az:
    if m: lines.append(f"- {t}: {m} من {n}")
for k in ("ruqya", "asma", "topics", "duas", "prayer_guide", "tasbih_phrases"):
    r = db.execute("SELECT value FROM meta WHERE key = ?", (k,)).fetchone()
    ok = bool(r and r[0])
    lines.append(f"- {k}: {'موجود' if ok else 'غير موجود'}")
lines.append("")
lines.append("## النتيجة: " + ("سليمة" if not errors else "فيها أخطاء"))
for e in errors: lines.append(f"- {e}")
report = "\n".join(lines)
print(report)
if len(sys.argv) > 2: open(sys.argv[2], "w", encoding="utf-8").write(report + "\n")
sys.exit(1 if errors else 0)
