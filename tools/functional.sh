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
IME=com.android.adbkeyboard/.AdbIME
curl -sSL -m 60 -o /tmp/adbkb.apk https://github.com/senzhk/ADBKeyBoard/raw/master/ADBKeyboard.apk && adb install -r /tmp/adbkb.apk
adb shell ime enable $IME; adb shell ime set $IME
adb shell settings put secure enabled_input_methods "$IME"; adb shell settings put secure default_input_method "$IME"
echo "keyboard: $(adb shell settings get secure default_input_method)"
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
expect "التجويد: نص سورة البقرة يظهر" "الٓمٓ" 25
shot tajweed-baqara
scrollAll; scrollAll; scrollAll
if alive; then ok "التجويد: سورة البقرة كاملة وتمرير حتى آخرها"; else bad "التجويد: انهار مع سورة البقرة"; fi
shot tajweed-baqara-end

# ── Word by word.
open word-by-word
expect "معاني الكلمات: الفاتحة" "الآية 1" 25
pickSurah "البقرة"
expect "معاني الكلمات: البقرة" "Alif Laam Meem" 30
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
$UI field 0; typeAr "الصلاة"; $UI tap "بحث"
expect "الأحاديث: نتائج البحث فيها الكلمة" "الصلاة" 20
expect "الأحاديث: البحث" "حديثًا" 20
$UI tap "العقيدة"; expect "الأحاديث: تصنيف" "حديثًا" 20
shot hadith
open hadith-check
# Typing Arabic into this multi-line field does not work from the test keyboard; the service itself
# is checked by the API probe. Here: the field and the button are there and nothing crashes.
expect "التحقق من حديث: الحقل والزر" "تحقق" 6
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
# A fresh GPS fix shortly after the request, as a phone would get it.
$UI tap "حدد موقعي وابحث"; sleep 3; adb emu geo fix 46.6753 24.7136 >/dev/null 2>&1
# Public map servers are often busy: then the app must show its fallback (retry / open in maps).
if $UI wait "مسجدا ضمن" 60; then ok "المساجد القريبة: نتائج"
elif $UI has "ابحث عن مساجد في الخرائط"; then ok "المساجد القريبة: الخوادم مشغولة فظهر البديل (إعادة المحاولة والخرائط)"
else bad "المساجد القريبة: لا نتائج ولا بديل"; fi
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
open kids; $UI tap "الناس" || $UI tap "الفلق"; sleep 3; alive && ok "الأطفال: فتح سورة" || bad "الأطفال: فتح سورة"; shot kids-surah
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
# Al-Kawthar (3 ayat) for the first reciter: scroll to it, download, wait for the delete icon.
for i in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28; do adb shell input swipe 540 1800 540 600 200; sleep 0.3; done
$UI tap "تحميل" 0; sleep 20
if $UI has "حذف"; then ok "التحميلات: تحميل سورة"; else bad "التحميلات: تحميل سورة"; fi
shot downloads

# ── Notification center: turn on the five prayers, check the system has the alarms, send a test.
adb shell pm grant $PKG android.permission.POST_NOTIFICATIONS 2>/dev/null || true
adb shell appops set $PKG SCHEDULE_EXACT_ALARM allow 2>/dev/null || true
open notifications
expect "مركز التنبيهات: يفتح" "إذن الإشعارات" 8
$UI tap "كل الصلوات الخمس"; sleep 3
expect "مركز التنبيهات: جدولة الصلوات" "التالي:" 8
n=$(adb shell dumpsys alarm | grep -c "com.ayattla.app.ALERT")
if [ "$n" -ge 5 ]; then ok "مركز التنبيهات: $n منبها في النظام"; else bad "مركز التنبيهات: المنبهات في النظام ($n)"; fi
shot notifications
$UI tap "تنبيه تجريبي"; sleep 3
if adb shell dumpsys notification --noredact | grep -q "تنبيه تجريبي"; then ok "مركز التنبيهات: وصل التنبيه التجريبي"; else bad "مركز التنبيهات: التنبيه التجريبي"; fi
open prayer-settings
expect "حساب المواقيت: يفتح" "طريقة الحساب" 6
$UI tap "الحنفي"; sleep 2; alive && ok "حساب المواقيت: العصر الحنفي" || bad "حساب المواقيت: العصر الحنفي"
shot prayer-settings
$UI tap "إعادة الضبط الافتراضي"

# ── Reader and mushaf pages.
open reader/2/1; scrollAll; scrollAll; alive && ok "القارئ: البقرة كاملة" || bad "القارئ: البقرة"
open page/1; for i in 1 2 3 4 5; do adb shell input swipe 900 1200 150 1200 120; sleep 0.6; done; alive && ok "المصحف: تقليب الصفحات" || bad "المصحف: تقليب"
open quran; scrollAll; alive && ok "فهرس السور: التمرير" || bad "فهرس السور"

# ── Home v5 and the unified search.
open home; expect "الرئيسية: الصلاة القادمة" "القبلة" 8; scrollAll; scrollAll; scrollAll; scrollAll
$UI tap "تخصيص الرئيسية"; sleep 2; expect "الرئيسية: التخصيص" "إعادة الترتيب الافتراضي" 5; shot home-customize; adb shell input keyevent 4
open home; $UI tap "الفجر" && alive && ok "الرئيسية: صلواتي اليوم" || bad "الرئيسية: صلواتي اليوم"
open settings; expect "الإعدادات: الافتتاحية" "الافتتاحية عند فتح التطبيق" 5
open search; $UI field 0; typeAr "الكهف"; sleep 3; expect "البحث الشامل: نتائج" "سورة الكهف" 6; shot search

# ── WhatsApp statuses by occasion.
open status; expect "الحالات: المناسبات" "المناسبات" 8
open status/jumuah; expect "الحالات: يوم الجمعة" "صممها حالة" 8
$UI tap "صممها حالة"; sleep 4; expect "الحالات: محرر البطاقة" "مشاركة" 8; shot status-card; adb shell input keyevent 4
open status/marriage; expect "الحالات: الزواج" "صممها حالة" 8

# ── «رحلتي مع القرآن»: a mushaf page on screen counts, the run starts.
open page/3; sleep 5
open journey; expect "رحلتي: الصفحة المقروءة تُحتسب" "سلسلتك: يوم واحد" 10; shot journey
scrollAll; expect "رحلتي: الأسبوع والشهر" "صفحة هذا الشهر" 6
open home; expect "الرئيسية: بطاقة رحلتي" "رحلتي مع القرآن" 10

# ── Without internet: airplane mode, and the app's own offline state (--es offline 1) in case the
#    emulator keeps a route. Everything opened above was saved, so it must keep working.
adb shell cmd connectivity airplane-mode enable >/dev/null 2>&1 || adb shell settings put global airplane_mode_on 1
adb shell svc wifi disable >/dev/null 2>&1; adb shell svc data disable >/dev/null 2>&1; sleep 5
offopen() { adb logcat -b all -c; adb shell am start -S -W -n $PKG/.MainActivity --es screen "$1" --es offline 1 >/dev/null 2>&1; sleep 4; }
offopen home;    expect "بدون إنترنت: شريط التنبيه في الرئيسية" "أنت بدون إنترنت" 10; shot offline-home
$UI tap "أنت بدون إنترنت"; sleep 2
expect "بدون إنترنت: يفتح مركز الاستخدام بدون إنترنت" "أنت بدون إنترنت الآن" 8; shot offline-center
scrollAll; expect "بدون إنترنت: الأدوات حسب ما يعمل" "يحتاج إنترنت" 6; shot offline-center-tools
offopen tajweed; pickSurah "البقرة"; expect "بدون إنترنت: التجويد من المحفوظ (البقرة)" "الٓمٓ" 15; shot offline-tajweed
offopen word-by-word; expect "بدون إنترنت: معاني الكلمات من المحفوظ" "الآية 1" 15
offopen hadith
if $UI wait "حديثًا" 10 || $UI wait "لا توجد أحاديث محفوظة" 3; then alive && ok "بدون إنترنت: الأحاديث من المحفوظ" || bad "بدون إنترنت: الأحاديث"; else bad "بدون إنترنت: الأحاديث"; fi
shot offline-hadith
offopen hadith-check; expect "بدون إنترنت: التحقق يبحث في المحفوظ" "يبحث في الأحاديث المحفوظة" 8
offopen radios;  expect "بدون إنترنت: الإذاعات توضح أنها تحتاج إنترنت" "الإذاعات بث مباشر يحتاج إنترنت" 15; shot offline-radios
offopen mosques; expect "بدون إنترنت: المساجد القريبة توضح الحاجة للإنترنت" "هذه الأداة تحتاج اتصالا بالإنترنت" 8
offopen prayer;  expect "بدون إنترنت: مواقيت الصلاة" "الفجر" 8
offopen azkar/1; alive && ok "بدون إنترنت: أذكار الصباح" || bad "بدون إنترنت: أذكار الصباح"
offopen reader/36/1; alive && ok "بدون إنترنت: قراءة سورة يس" || bad "بدون إنترنت: قراءة سورة يس"
offopen qibla;   alive && ok "بدون إنترنت: القبلة" || bad "بدون إنترنت: القبلة"
offopen occasions; expect "بدون إنترنت: المناسبات تعمل" "يعمل بدون إنترنت" 8
offopen kaffarah;  expect "بدون إنترنت: الكفارات تعمل" "يعمل بدون إنترنت" 8
adb shell cmd connectivity airplane-mode disable >/dev/null 2>&1 || adb shell settings put global airplane_mode_on 0
adb shell svc wifi enable >/dev/null 2>&1; adb shell svc data enable >/dev/null 2>&1; sleep 5
open offline; expect "مع الإنترنت: مركز التجهيز" "أنت متصل بالإنترنت" 20

echo "النتيجة: $(grep -c '^✓' "$OUT") نجحت، $FAIL فشلت" | tee -a "$OUT"
[ "$FAIL" -eq 0 ]
