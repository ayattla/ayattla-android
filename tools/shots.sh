#!/bin/bash
# Runs inside the emulator job: installs the debug APK and captures each main screen.
set -u
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
adb shell cmd uimode night yes; sleep 2
shot 16-home-dark home 7
shot 17-reader-dark reader/1/1
adb logcat -d -s AndroidRuntime:E > shots/crashes.txt || true
wc -l shots/crashes.txt
