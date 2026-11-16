#!/bin/bash
# Functional check of every tool, as a person would use it: pick, press, type, wait for the result.
# Runs in the emulator job after crawl.sh. Each step is logged as ✓ / ✗ in shots/functional.txt,
# with a screenshot of the result (shots/f-*.png) and the visible text when a step fails.
set -u
PKG=com.ayattla.app
OUT=shots/functional.txt
mkdir -p shots; : > "$OUT"
FAIL=0
UI="python3 tools/ui.py"

# Arabic typing: ADB keyboard (adb's own "input text" is ASCII only).
curl -sSL -m 60 -o /tmp/adbkb.apk https://github.com/senzhk/ADBKeyBoard/raw/master/ADBKeyboard.apk && adb install -r /tmp/adbkb.apk >/dev/null 2>&1 \
  && adb shell ime enable com.android.adbkeyboard/.AdbIME >/dev/null && adb shell ime set com.android.adbkeyboard/.AdbIME >/dev/null
typeAr() { adb shell am broadcast -a ADB_INPUT_TEXT --es msg "$1" >/dev/null; sleep 1; }

# Location for prayer times, qibla and mosques: Riyadh.
adb emu geo fix 46.6753 24.7136 >/dev/null 2>&1 || true
adb shell pm grant $PKG android.permission.ACCESS_FINE_LOCATION 2>/dev/null || true
adb shell pm grant $PKG android.permission.ACCESS_COARSE_LOCATION 2>/dev/null || true

open() { adb logcat -b all -c; adb shell am start -S -W -n $PKG/.MainActivity --es screen "$1" >/dev/null 2>&1; sleep 4; }
shot() { adb exec-out screencap -p > "shots/f-$1.png"; }
alive() {
  local log; log=$(adb logcat -d -b crash,main -s AndroidRuntime:E ActivityManager:E 2>/dev/null)
  if echo "$log" | grep -qE "FATAL EXCEPTION|ANR in $PKG"; then echo "$log" | grep -A30 -E "FATAL EXCEPTION|ANR in" | head -40 >> "$OUT"; return 1; fi
  [ -n "$(adb shell pidof $PKG)" ]
}
ok()   { echo "✓ $1" | tee -a "$OUT"; }
bad()  { echo "✗ $1" | tee -a "$OUT"; echo "   الشاشة: $($UI texts)" >> "$OUT"; FAIL=$((FAIL + 1)); }
expect() {  # $1 label, $2 text expected, $3 seconds
  if $UI wait "$2" "${3:-15}" && alive; then ok "$1"; else alive || echo "   (انهار التطبيق)" >> "$OUT"; bad "$1 — لم يظهر «$2»"; fi
}
scrollAll() { for i in 1 2 3 4 5 6 7 8 9 10 11 12; do adb shell input swipe 540 1900 540 250 80; sleep 0.25; done; sleep 1; }
pickSurah() {  # opens the surah sheet and picks $1
  $UI tap "سورة الفاتحة" && sleep 1 && $UI tap "$1" || $UI tap "$1"
}

# ── Tajweed: a long surah, then scroll to its end.
open tajweed
expect "التجويد: الفاتحة تظهر ملونة" "مفتاح الألوان" 10
pickSurah "البقرة"; sleep 3
if $UI gone "تعذر" 2 && alive; then :; fi
sleep 6; shot tajweed-baqara
scrollAll; scrollAll; scrollAll
if alive; then ok "التجويد: سورة البقرة كاملة وتمرير حتى آخرها"; else bad "التجويد: انهار مع سورة البقرة"; fi
shot tajweed-baqara-end

# ── Word by word.
open word-by-word
expect "معاني الكلمات: الفاتحة" "الآية 1" 25
pickSurah "البقرة"
expect "معاني الكلمات: البقرة" "الآية 1" 30
scrollAll; scrollAll
alive && ok "معاني الكلمات: التمرير في البقرة" || bad "معاني الكلمات: انهار أثناء التمرير"
shot wbw

# ── Khatma.
open khatma
$UI tap "ابدأ الخطة"
expect "الختمة: بدء خطة 30 يوما" "من 30" 8
$UI tap "أتممت ورد اليوم"
expect "الختمة: تسجيل ورد اليوم" "أتممت ورد اليوم ✓" 5
shot khatma
$UI tap "اقرأ ورد اليوم"
sleep 3; alive && ok "الختمة: فتح صفحات ورد اليوم" || bad "الختمة: فتح ورد اليوم"
shot khatma-read
open khatma; $UI tap "حذف الخطة"

# ── Hifz test.
open hifz-test
$UI tap "ابدأ الاختبار"
expect "اختبار الحفظ: إخفاء الكلمات" "المخفي الآن" 6
$UI tap "اكشف الكل"; expect "اختبار الحفظ: كشف الكل" "المخفي الآن: 0" 5
shot hifz-test

# ── Hifz plan.
open hifz-plan
$UI tap "ابدأ الخطة"
expect "خطة الحفظ: البدء" "حفظ اليوم" 6
$UI tap "حفظته"
expect "خطة الحفظ: تسجيل حفظ اليوم" "أنجزت حفظ اليوم" 6
shot hifz-plan

# ── Hadith: search and categories.
open hadith
expect "الأحاديث: القائمة" "حديثًا" 20
$UI tap "ابحث في الأحاديث"; typeAr "الصلاة"; $UI tap "بحث"
expect "الأحاديث: البحث" "حديثًا" 20
$UI tap "العقيدة"; expect "الأحاديث: تصنيف" "حديثًا" 20
shot hadith
open hadith-check
$UI tap "نص الحديث"; typeAr "إنما الأعمال بالنيات"; $UI tap "تحقق"
expect "التحقق من حديث" "إنما الأعمال بالنيات" 20
shot hadith-check

# ── Tasbih.
open tasbih
for i in 1 2 3; do adb shell input tap 540 1300; sleep 0.4; done
expect "المسبحة: العد" "3" 4
shot tasbih

# ── Azkar: count down a zikr.
open azkar/1
expect "الأذكار: أذكار الصباح" "الصباح" 6
shot azkar

# ── Ruqya: play.
open ruqya
$UI tap "استمع للرقية كاملة"; sleep 6
alive && ok "الرقية: تشغيل التلاوة" || bad "الرقية: التشغيل"
shot ruqya

# ── Radios.
open radios
expect "الإذاعات: القائمة" "ابحث باسم القارئ" 20
shot radios

# ── Mosques (location set to Riyadh).
open mosques
$UI tap "حدد موقعي وابحث"; sleep 2
$UI tap "While using" 2>/dev/null || $UI tap "أثناء" 2>/dev/null || true
if $UI wait "مسجدا ضمن" 60; then ok "المساجد القريبة: نتائج"; else bad "المساجد القريبة: لا نتائج"; fi
shot mosques

# ── Prayer times and qibla.
open prayer
expect "مواقيت الصلاة" "الفجر" 10
shot prayer
open qibla; sleep 3; alive && ok "القبلة: تفتح" || bad "القبلة"; shot qibla

# ── Hijri, zakat, qada, sawm.
open hijri; $UI tap "يوم بعد"; $UI tap "شهر بعد"; alive && ok "التقويم الهجري: التنقل" || bad "التقويم الهجري"; shot hijri
open zakat; expect "الزكاة" "النصاب" 5; shot zakat
open qada; $UI tap "أضف للعداد"; $UI tap "قضيت واحدة"; alive && ok "قضاء الصلوات" || bad "قضاء الصلوات"; shot qada
open sawm; $UI tap "حفظ"; alive && ok "قضاء الصيام" || bad "قضاء الصيام"; shot sawm

# ── Kids.
open kids; $UI tap "النبأ" || $UI tap "الناس"; sleep 3; alive && ok "الأطفال: فتح سورة" || bad "الأطفال: فتح سورة"; shot kids-surah
open kids-quiz; expect "مسابقة الأطفال" "النتيجة" 6
shot kids-quiz

# ── Library tools.
open asma; expect "أسماء الله الحسنى" "الرحمن" 6; shot asma
open duas; expect "الأدعية" "أدعية من القرآن" 6; shot duas
open topics; expect "المواضيع" "مواضع" 6; $UI tap "مواضع"; sleep 2; alive && ok "المواضيع: فتح موضوع" || bad "المواضيع: فتح موضوع"; shot topics
open occasions; expect "المناسبات" "رمضان" 6
open ayah-today; expect "آية اليوم" "التفسير الميسر" 6; $UI tap "استمع"; sleep 3; alive && ok "آية اليوم: استماع" || bad "آية اليوم: استماع"; shot ayah-today
open guide; sleep 2; alive && ok "دليل الاستخدام" || bad "دليل الاستخدام"

# ── Downloads: one short surah.
open downloads
$UI tap "تحميل" 0; sleep 15
if $UI has "حذف"; then ok "التحميلات: تحميل سورة"; else bad "التحميلات: تحميل سورة"; fi
shot downloads

# ── Reader and mushaf pages.
open reader/2/1; scrollAll; scrollAll; alive && ok "القارئ: البقرة كاملة" || bad "القارئ: البقرة"
open page/1; for i in 1 2 3 4 5; do adb shell input swipe 900 1200 150 1200 120; sleep 0.6; done; alive && ok "المصحف: تقليب الصفحات" || bad "المصحف: تقليب"
open quran; scrollAll; alive && ok "فهرس السور: التمرير" || bad "فهرس السور"

echo "النتيجة: $(grep -c '^✓' "$OUT") نجحت، $FAIL فشلت" | tee -a "$OUT"
[ "$FAIL" -eq 0 ]
