#!/bin/bash
# Backup builder on the Hostinger server: same steps as GitHub Actions, plus the store signing.
# Low priority so the site is unaffected. Signing key: /root/.ayattla-android (never in git).
set -e
cd "$(dirname "$0")"
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export ANDROID_HOME=/opt/android-sdk
K=/root/.ayattla-android
export AYT_VERSION_CODE=${AYT_VERSION_CODE:-$(( $(date -u +%s) / 60 ))}
export AYT_KS=$K/upload.jks AYT_KS_PASS="$(cat $K/pass)"
echo "sdk.dir=$ANDROID_HOME" > local.properties
nice -n 19 ionice -c3 bash tools/prepare.sh
nice -n 19 ionice -c3 /opt/gradle/current/bin/gradle --no-daemon -q testDebugUnitTest bundleRelease assembleRelease
mkdir -p out
cp app/build/outputs/bundle/release/app-release.aab out/ayattla.aab
cp app/build/outputs/apk/release/app-release.apk out/ayattla.apk
BT=$(ls -d $ANDROID_HOME/build-tools/* | sort -V | tail -1)
$BT/apksigner verify out/ayattla.apk && echo "apk signature OK"
ls -la out
