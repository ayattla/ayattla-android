#!/bin/bash
# Builds the signed Play bundle (.aab) and a test .apk. Low priority so the site is unaffected.
set -e
cd "$(dirname "$0")"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export ANDROID_HOME=/opt/android-sdk
K=/root/.ayattla-android; mkdir -p $K; chmod 700 $K
if [ ! -f $K/upload.jks ]; then
  head -c 24 /dev/urandom | base64 | tr -d '/+=' > $K/pass; chmod 600 $K/pass
  keytool -genkeypair -keystore $K/upload.jks -alias upload -keyalg RSA -keysize 4096 -validity 10000 \
    -storepass "$(cat $K/pass)" -keypass "$(cat $K/pass)" -dname "CN=Ayat Tutla, O=ayattla.com, C=SA" >/dev/null 2>&1
  chmod 600 $K/upload.jks; echo "new upload key created"
fi
export AYT_KS=$K/upload.jks AYT_KS_PASS="$(cat $K/pass)"
SDK=$(ls $ANDROID_HOME/platforms | sed 's/android-//' | sort -n | tail -1)
AGP=$(grep '^AGP:' /root/android-setup.log | awk '{print $2}')
ABH=$(grep '^ABH:' /root/android-setup.log | awk '{print $2}')
echo "sdk=$SDK agp=$AGP abh=$ABH"
sed -i "s/@AGP@/$AGP/" build.gradle
sed -i "s/@SDK@/$SDK/g; s/@ABH@/$ABH/" app/build.gradle
curl -sSo /tmp/ic-any.png https://ayattla.com/tools/icon-512.png
curl -sSo /tmp/ic-mask.png https://ayattla.com/tools/icon-maskable-512.png
BG=$(python3 icons.py /tmp/ic-any.png /tmp/ic-mask.png app/src/main/res)
sed -i "s/@ICONBG@/$BG/" app/src/main/res/values/colors.xml
echo "sdk.dir=$ANDROID_HOME" > local.properties
nice -n 19 ionice -c3 /opt/gradle/current/bin/gradle --no-daemon -q bundleRelease assembleRelease
mkdir -p out
cp app/build/outputs/bundle/release/app-release.aab out/ayattla.aab
cp app/build/outputs/apk/release/app-release.apk out/ayattla.apk
BT=$(ls -d $ANDROID_HOME/build-tools/* | sort -V | tail -1)
$BT/apksigner verify out/ayattla.apk && echo "apk signature OK"
keytool -list -v -keystore $K/upload.jks -storepass "$AYT_KS_PASS" -alias upload | grep -E 'SHA256:' | awk '{print $2}' > out/upload-sha256.txt
echo "SHA256 $(cat out/upload-sha256.txt)"
ls -la out
