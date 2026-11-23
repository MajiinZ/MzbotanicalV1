# MZ Botanicals Android storefront preview

This installable test app bundles `webStorefront/dist-review`. It is separate from `composeApp` and does not request internet permission or use Firebase, real sign-in, payments, or shipping. Its package is `org.mz.botanicals.storefrontpreview`, so it can coexist with the native app. Minimum Android version: 8.0 (API 26) with an up-to-date Android System WebView.

From the `webStorefront` directory, install dependencies with `npm ci` and build using Node 24, JDK 17, Android SDK platform 35 and build-tools 35.0.0:

```sh
bash android-preview/build.sh /absolute/path/mz-botanicals-storefront-preview.apk
```

The build uses a fresh test signing key. Uninstall an older preview before installing a newly signed build. This APK is for sideloaded testing, not production distribution.
