#!/bin/bash
# Build Otchłań Launcher APK: ./build_apk.sh <versionName> <versionCode>
# Sign with the game keystore: KEYSTORE=path KS_PASS=... (same key as the game, kept private)
set -e
cd "$(dirname "$0")"
SDK=${SDK:-/home/claude/sdk}; BT=$SDK/build-tools/35.0.0; AJ=$SDK/platforms/android-34/android.jar
KS=${KEYSTORE:-otchlan.keystore}
VER=${1:-1.0}; CODE=${2:-1}
rm -rf build && mkdir -p build/res build/classes build/apk
sed -i "s/android:versionCode=\"[0-9]*\"/android:versionCode=\"$CODE\"/; s/android:versionName=\"[^\"]*\"/android:versionName=\"$VER\"/" AndroidManifest.xml
$BT/aapt2 compile --dir res -o build/res/
$BT/aapt2 link -I $AJ --manifest AndroidManifest.xml -o build/base.apk -A assets build/res/*.flat --auto-add-overlay
javac -source 8 -target 8 -bootclasspath $AJ -cp $AJ -d build/classes src/ua/dima/otchlan/launcher/*.java 2>&1 | grep -v -e warning -e JAVA_TOOL -e _JAVA_OPTIONS || true
$BT/d8 --min-api 24 --lib $AJ --output build/apk build/classes/ua/dima/otchlan/launcher/*.class
cp build/base.apk build/unsigned.apk
(cd build/apk && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f -p 4 build/unsigned.apk build/aligned.apk
$BT/apksigner sign --ks $KS --ks-pass env:KS_PASS --key-pass env:KS_PASS --out build/OtchlanLauncher.apk build/aligned.apk
$BT/apksigner verify build/OtchlanLauncher.apk && ls -la build/OtchlanLauncher.apk
