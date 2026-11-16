#!/bin/bash
# Full crash sweep, run in the emulator job after shots.sh (the debug APK is installed).
# Opens every screen, scrolls each to its end and back (long lists, tall cards), in light and dark,
# then runs a random-tap test. Any crash or ANR fails the job, with its stack trace in shots/crawl.txt.
set -u
PKG=com.ayattla.app
OUT=shots/crawl.txt
mkdir -p shots; : > "$OUT"
FAIL=0

ROUTES=(
  home quran azkar azkar/1 azkar/5 azkar-all tasbih tajweed word-by-word hifz-test ruqya asma occasions
  qada sawm mosques kids kids/112 kids/78 hifz-plan downloads kids-quiz ayah-today text-card status
  prayer notifications prayer-settings qibla hijri zakat saved settings about radios hadith hadith-check topics duas guide khatma
  page/1 page/50 page/604 reader/1/1 reader/2/1 reader/2/255 reader/26/1 reader/114/1
  'more?group=Daily' 'more?group=Quran' more 'studio?s=2\&a=255\&b=255' 'studio?v=1'
)

check() {  # $1 = label
  local log; log=$(adb logcat -d -b crash,main -s AndroidRuntime:E ActivityManager:E 2>/dev/null)
  if echo "$log" | grep -qE "FATAL EXCEPTION|ANR in $PKG|too large"; then
    echo "✗ $1" | tee -a "$OUT"
    echo "$log" | grep -A40 -E "FATAL EXCEPTION|ANR in" | head -60 >> "$OUT"
    FAIL=$((FAIL + 1))
  elif [ -z "$(adb shell pidof $PKG)" ]; then
    echo "✗ $1 (the app is no longer running)" | tee -a "$OUT"
    FAIL=$((FAIL + 1))
  else
    echo "✓ $1" | tee -a "$OUT"
  fi
}

sweep() {  # $1 = theme label
  for r in "${ROUTES[@]}"; do
    adb logcat -b all -c
    adb shell am start -S -W -n $PKG/.MainActivity --es screen "$r" >/dev/null 2>&1
    sleep 3
    for i in 1 2 3 4 5 6 7 8; do adb shell input swipe 540 1800 540 300 100; sleep 0.3; done
    for i in 1 2; do adb shell input swipe 540 400 540 1900 100; sleep 0.3; done
    sleep 1
    check "$1 $r"
  done
}

adb shell cmd uimode night no; sleep 1
sweep "فاتح"
adb shell cmd uimode night yes; sleep 1
sweep "داكن"
adb shell cmd uimode night no

# Large text on a dense screen, as on big phones with a larger font size: long cards get much taller.
adb shell settings put system font_scale 1.3
adb shell wm density 520; sleep 2
sweep "خط كبير"
adb shell settings put system font_scale 1.0
adb shell wm density reset; sleep 2

# Random taps and swipes across the app (fixed seed, so a failure can be replayed).
adb logcat -b all -c
adb shell am start -S -W -n $PKG/.MainActivity >/dev/null 2>&1; sleep 4
adb shell monkey -p $PKG -s 4242 --throttle 120 --pct-syskeys 0 --pct-appswitch 0 --ignore-security-exceptions -v 4000 > shots/monkey.txt 2>&1
if grep -qE "// CRASH|// NOT RESPONDING" shots/monkey.txt; then
  echo "✗ random-tap test" | tee -a "$OUT"
  grep -A40 -E "// CRASH|// NOT RESPONDING" shots/monkey.txt | head -60 >> "$OUT"
  FAIL=$((FAIL + 1))
else
  echo "✓ random-tap test (4000 events)" >> "$OUT"
fi

echo "screens checked: $(grep -c '^✓' "$OUT") passed, $FAIL failed" | tee -a "$OUT"
[ "$FAIL" -eq 0 ]
