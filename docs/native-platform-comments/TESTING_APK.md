# Testing the GitHub Actions APK

The Actions artifact is an unofficial **development** build of the native
platform comments proof of concept. It uses the `unstableDebug` variant so it:

- installs as `Grayjay Unstable` with application ID
  `com.futo.platformplayer.d`;
- can coexist with the official stable Grayjay application;
- enables the optional plugin `Browser` package through `BuildConfig.DEBUG`;
- remains updateable across CI runs when the repository's persistent debug
  signing secret is configured.

It is intentionally not presented as an official FUTO release. A distributable
release build needs a separately managed release keystore and cannot grant an
unsigned third-party plugin FUTO's official-plugin trust.

## Download

1. Open the repository's **Releases** page.
2. Open the newest `Grayjay native comments development` prerelease.
3. Download `grayjay-native-comments-unstable-debug.apk` and its adjacent
   `.sha256` file. The same files remain available as an Actions artifact for
   30 days.
4. Optionally verify the APK before installation:

   ```sh
   shasum -a 256 -c grayjay-native-comments-unstable-debug.apk.sha256
   ```

## Install on an Android Studio virtual device

Start the virtual device in **Device Manager**, then run:

```sh
adb devices
adb install -r grayjay-native-comments-unstable-debug.apk
```

The `-r` flag preserves the existing `Grayjay Unstable` application data. The
CI version code increases on every Actions run. When the repository's
`GRAYJAY_DEBUG_KEYSTORE_BASE64` secret is configured, its persistent
development certificate also lets Android accept later artifacts as updates.

If Android reports a certificate mismatch, the installed package came from a
different signing key. Uninstalling it fixes the mismatch but deletes that
build's local app data:

```sh
adb uninstall com.futo.platformplayer.d
adb install grayjay-native-comments-unstable-debug.apk
```

## Browser security behavior

No in-app switch is required for this APK. The `debug` build type sets
`BuildConfig.DEBUG=true`, which is one of Grayjay's existing, explicit paths for
allowing the plugin `Browser` package.

For a local Android Studio build, select the `unstableDebug` build variant and
run the `app` configuration. A release build sets `BuildConfig.DEBUG=false`.
In a release build, `Browser` remains limited to a plugin carrying FUTO's
official signature or a source loaded through Grayjay's developer source ID;
ordinary Developer Settings do not globally remove that restriction.

## Recommended smoke test

1. Open `Grayjay Unstable` and sign in to the bundled YouTube source.
2. Open a YouTube video with comments enabled.
3. Verify Platform comments load and create one uniquely named test comment.
4. Reply to it, then exercise like/dislike toggling, edit, and delete.
5. Delete every test comment before finishing.
6. Switch to Polycentric and confirm its composer and reactions still target
   Polycentric rather than YouTube.
