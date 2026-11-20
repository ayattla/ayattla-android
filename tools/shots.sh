#!/bin/bash
# Runs inside the emulator job: installs the debug APK and captures each main screen.
set -u
# 1) The minified release build: launch it and record any crash.
REL=$(ls rel/*.apk 2>/dev/null | head -1)
if [ -n "$REL" ]; then
  adb install -r "$REL" && adb logcat -c
  adb shell am start -W -n com.ayattla.app/.MainActivity >/dev/null; sleep 12
  adb shell am start -W -n com.ayattla.app/.MainActivity >/dev/null; sleep 4
  mkdir -p shots; adb exec-out screencap -p > shots/00-release.png
  adb logcat -d -s AndroidRuntime:E System.err:W > shots/release-crash.txt
  echo "release crash lines: $(grep -c 'FATAL' shots/release-crash.txt)"
  adb uninstall com.ayattla.app >/dev/null
fi
APK=$(ls apk/*.apk | head -1)
adb install -r "$APK"
adb shell pm grant com.ayattla.app android.permission.POST_NOTIFICATIONS 2>/dev/null || true
mkdir -p shots
adb shell settings put global hide_error_dialogs 1 || true
sleep 20
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null || true
shot() {
  adb shell am start -S -W -n com.ayattla.app/.MainActivity --es screen "$2" >/dev/null
  sleep "${3:-5}"
  adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  sleep 1
  adb exec-out screencap -p > "shots/$1.png"
  echo "$1 $(stat -c %s shots/$1.png) bytes"
}
shot 00-intro-a intro 0.2
shot 00-intro-b intro 2
shot 01-home home 9
shot 02-quran quran
shot 03-reader-baqara-255 reader/2/255
shot 04-reader-yasin reader/36/1
shot 05-azkar azkar
shot 06-azkar-morning azkar/1
shot 07-studio 'studio?s=2\&a=255\&b=255' 7
shot 08-more more
shot 09-tasbih tasbih
shot 10-prayer prayer
shot 11-qibla qibla
shot 12-hijri hijri
shot 13-zakat zakat
shot 14-settings settings
shot 15-about about
shot 15b-radios radios 8
shot 18-hadith hadith 9
shot 19-topics topics
shot 20-duas duas
shot 21-guide guide
shot 22-khatma khatma
shot 23-mushaf-page page/50
shot 24-mushaf-page-1 page/1
shot 25-studio-video 'studio?v=1' 12
shot 26-tajweed tajweed 10
shot 27-word-by-word word-by-word 10
shot 28-hifz hifz-test
shot 29-ruqya ruqya
shot 30-asma asma
shot 31-occasions occasions
shot 32-qada qada
shot 33-sawm sawm
shot 34-mosques mosques
shot 35-kids kids
shot 36-kids-surah kids/112
shot 37-ayah-today ayah-today 8
shot 38-status status
shot 38b-status-jumuah status/jumuah
shot 38c-status-marriage status/marriage
shot 39-more-all 'more?group=Daily'
# Real clip on the device: Al-Kawthar, 3 ayat, 9:16.
adb logcat -c
adb shell am start -S -n com.ayattla.app/.MainActivity --es selftest clip --es screen studio >/dev/null
for i in $(seq 40); do sleep 5; adb logcat -d -s AyatSelfTest:* | grep -q 'CLIP' && break; done
adb logcat -d -s AyatSelfTest:* | tee shots/selftest.txt
# Same clip over a real background video from the site's library.
adb logcat -c
adb shell am start -S -n com.ayattla.app/.MainActivity --es selftest clipvideo --es screen studio >/dev/null
for i in $(seq 60); do sleep 5; adb logcat -d -s AyatSelfTest:* | grep -q 'CLIP' && break; done
adb logcat -d -s AyatSelfTest:* | tee -a shots/selftest.txt
adb shell cmd uimode night yes; sleep 2
shot 16-home-dark home 7
shot 17-reader-dark reader/1/1
adb logcat -d -s AndroidRuntime:E > shots/crashes.txt || true
wc -l shots/crashes.txt
