#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
sdk=${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK with platform 35 and build-tools 35.0.0}
tools="$sdk/build-tools/35.0.0"
platform="$sdk/platforms/android-35/android.jar"
output=${1:?Pass an absolute output path for the APK}
test "${output:0:1}" = / && test "${output##*.}" = apk
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT

(cd "$here/.." && npm run build:review)
review="$here/../dist-review"
mkdir -p "$tmp/res/drawable-nodpi" "$tmp/classes" "$tmp/dex" "$tmp/assets"/{assets,brand}
cp "$review/index.html" "$tmp/assets/"
cp "$review/assets/"{app.js,app.css} "$tmp/assets/assets/"
cp "$review/brand/"{exo2-regular.otf,mz-brand.png,mz-welcome.png} "$tmp/assets/brand/"
cp "$review/brand/mz-brand.png" "$tmp/res/drawable-nodpi/preview_icon.png"

"$tools/aapt2" compile --dir "$tmp/res" -o "$tmp/res.zip"
"$tools/aapt2" link -I "$platform" --manifest "$here/AndroidManifest.xml" \
  -A "$tmp/assets" "$tmp/res.zip" -o "$tmp/resources.apk"
javac --release 8 -classpath "$platform" -d "$tmp/classes" "$here/PreviewActivity.java"
jar cf "$tmp/code.jar" -C "$tmp/classes" .
"$tools/d8" --min-api 26 --lib "$platform" --output "$tmp/dex" "$tmp/code.jar"
cp "$tmp/resources.apk" "$tmp/unsigned.apk"
(cd "$tmp/dex" && zip -q -X "$tmp/unsigned.apk" classes.dex)
"$tools/zipalign" -p -f 4 "$tmp/unsigned.apk" "$tmp/aligned.apk"
keytool -genkeypair -keystore "$tmp/review-signing.p12" -storetype PKCS12 \
  -storepass android -keypass android -alias preview -keyalg RSA -keysize 2048 \
  -validity 3650 -dname 'CN=MZ Botanicals Offline Preview' >/dev/null 2>&1
"$tools/apksigner" sign --ks "$tmp/review-signing.p12" --ks-pass pass:android \
  --out "$output" "$tmp/aligned.apk"
"$tools/apksigner" verify "$output"
rm -f "$output.idsig"
