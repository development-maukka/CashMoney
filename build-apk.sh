#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT to your Android SDK directory}"
tools="$ANDROID_SDK_ROOT/build-tools/35.0.0"
platform="$ANDROID_SDK_ROOT/platforms/android-35/android.jar"
mkdir -p build/classes build/dex
cp app/src/main/AndroidManifest.xml build/AndroidManifest.xml
"$tools/aapt2" compile --dir app/src/main/res -o build/resources.zip
"$tools/aapt2" link -o build/unsigned.apk -I "$platform" --manifest build/AndroidManifest.xml --java build build/resources.zip --min-sdk-version 30 --target-sdk-version 35 --version-code 17 --version-name 0.9.0
find app/src/main/java build/fi -name '*.java' > build/sources.txt
if command -v javac >/dev/null; then
    javac -encoding UTF-8 -source 8 -target 8 -bootclasspath "$platform:$tools/core-lambda-stubs.jar" -d build/classes @build/sources.txt
else
    : "${ECJ_JAR:?Install a JDK or set ECJ_JAR to Eclipse compiler jar}"
    java -jar "$ECJ_JAR" -encoding UTF-8 -source 8 -target 8 -bootclasspath "$platform:$tools/core-lambda-stubs.jar" -d build/classes @build/sources.txt
fi
(cd build/classes && zip -q -r ../classes.jar .)
"$tools/d8" --lib "$platform" --min-api 30 --output build/dex build/classes.jar
(cd build/dex && zip -q -u ../unsigned.apk classes.dex)
"$tools/zipalign" -f -P 16 4 build/unsigned.apk build/aligned.apk
# Reuse the original private signing key; never generate a replacement automatically.
keystore="${CASHMONEY_KEYSTORE:-cashmoney-dev.jks}"
[ -f "$keystore" ] || { echo "Set CASHMONEY_KEYSTORE to the original private key." >&2; exit 1; }
"$tools/apksigner" sign --ks "$keystore" --ks-key-alias cashmoney --ks-pass pass:android --key-pass pass:android --out build/CashMoney-0.9.0.apk build/aligned.apk
"$tools/apksigner" verify --verbose build/CashMoney-0.9.0.apk
