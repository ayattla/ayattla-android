#!/bin/bash
# Installs the minified release APK, opens it twice, opens a few screens by tapping nothing, and fails on any crash.
APK=$(ls out/apk/release/*.apk | head -1)
adb install -r "$APK" || exit 1
adb logcat -c
adb shell am start -W -n com.ayattla.app/.MainActivity
sleep 15
adb shell am force-stop com.ayattla.app
adb shell am start -W -n com.ayattla.app/.MainActivity
sleep 8
adb logcat -d -s AndroidRuntime:E > crash.txt
if grep -q FATAL crash.txt; then
  echo "::error::Crash on API $(adb shell getprop ro.build.version.sdk)"
  sed -n '1,60p' crash.txt
  exit 1
fi
adb shell dumpsys activity activities | grep -m1 -E 'mResumedActivity|topResumedActivity'
echo "OK on API $(adb shell getprop ro.build.version.sdk)"
