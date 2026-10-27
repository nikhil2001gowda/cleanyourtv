#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

SDK_ROOT="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BUILD_TOOLS="$SDK_ROOT/build-tools/37.0.0"
AAPT2="$BUILD_TOOLS/aapt2"
D8="$BUILD_TOOLS/d8"
ZIPALIGN="$BUILD_TOOLS/zipalign"
APKSIGNER="$BUILD_TOOLS/apksigner"
ANDROID_JAR="$SDK_ROOT/platforms/android-34/android.jar"
KEYSTORE="$HOME/.android/debug.keystore"
KEYSTORE_PASS="android"
KEY_ALIAS="androiddebugkey"

BUILD_DIR="$DIR/build"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/classes" "$BUILD_DIR/dex"

echo "==> aapt2 compile..."
"$AAPT2" compile --dir "$DIR/res" -o "$BUILD_DIR/res-compiled.zip"

echo "==> aapt2 link..."
"$AAPT2" link \
    -o "$BUILD_DIR/unsigned.apk" \
    -I "$ANDROID_JAR" \
    --manifest "$DIR/AndroidManifest.xml" \
    -R "$BUILD_DIR/res-compiled.zip" \
    --java "$BUILD_DIR/gen" \
    --auto-add-overlay \
    --min-sdk-version 21 \
    --target-sdk-version 34

echo "==> javac..."
find "$DIR/src" "$BUILD_DIR/gen" -name "*.java" > "$BUILD_DIR/sources.txt"
javac -encoding UTF-8 -source 8 -target 8 \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD_DIR/classes" \
    @"$BUILD_DIR/sources.txt"

echo "==> Packaging classes into jar..."
jar cf "$BUILD_DIR/classes.jar" -C "$BUILD_DIR/classes" .

echo "==> d8..."
"$D8" --lib "$ANDROID_JAR" --output "$BUILD_DIR/dex" "$BUILD_DIR/classes.jar"

echo "==> Packaging dex into APK..."
cp "$BUILD_DIR/unsigned.apk" "$BUILD_DIR/unsigned-with-dex.apk"
(cd "$BUILD_DIR/dex" && jar uf "$BUILD_DIR/unsigned-with-dex.apk" classes.dex)

echo "==> zipalign..."
"$ZIPALIGN" -f -p 4 "$BUILD_DIR/unsigned-with-dex.apk" "$BUILD_DIR/aligned.apk"

echo "==> apksigner..."
"$APKSIGNER" sign \
    --ks "$KEYSTORE" \
    --ks-pass "pass:$KEYSTORE_PASS" \
    --ks-key-alias "$KEY_ALIAS" \
    --key-pass "pass:$KEYSTORE_PASS" \
    --out "$DIR/home-on-fire.apk" \
    "$BUILD_DIR/aligned.apk"

echo "==> Built successfully: $DIR/home-on-fire.apk"
