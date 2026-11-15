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
# Real clip on the device: Al-Kawthar, 3 ayat, 9:16.
adb logcat -c
adb shell am start -S -n com.ayattla.app/.MainActivity --es selftest clip --es screen studio >/dev/null
for i in $(seq 40); do sleep 5; adb logcat -d -s AyatSelfTest:* | grep -q 'CLIP' && break; done
adb logcat -d -s AyatSelfTest:* | tee shots/selftest.txt
adb shell cmd uimode night yes; sleep 2
shot 16-home-dark home 7
shot 17-reader-dark reader/1/1
adb logcat -d -s AndroidRuntime:E > shots/crashes.txt || true
wc -l shots/crashes.txt
