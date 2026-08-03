# Build and test

## Prerequisites

- Git with Git LFS
- Recursive submodules
- Android SDK 36
- A JDK accepted by the checked-in Gradle wrapper (Android Studio's bundled JDK
  is suitable)
- Node.js 20 or newer for plugin tests

After cloning:

```bash
git submodule update --init --recursive
git lfs pull
```

The FFmpeg AAR is tracked by Git LFS. A text pointer in its place will produce a
ZIP/manifest error during Android builds.

## Automated checks

Run the repository verifier:

```bash
./scripts/verify-native-platform-comments.sh
```

Or run its components directly:

```bash
npm --prefix app/src/stable/assets/sources/youtube run verify
npm --prefix app/src/unstable/assets/sources/youtube run verify

./gradlew :app:testStableDebugUnitTest \
  --tests com.futo.platformplayer.PlatformCommentMutationTests \
  --tests com.futo.platformplayer.PlatformVideoReactionTests

./gradlew :app:testUnstableDebugUnitTest \
  --tests com.futo.platformplayer.PlatformCommentMutationTests \
  --tests com.futo.platformplayer.PlatformVideoReactionTests

./gradlew :app:assembleStableDebug :app:assembleUnstableDebug
```

`npm run verify` runs the plugin's 56 mutation/parser tests, verifies that
generated modules embedded in `YoutubeScript.js` match their sources, and parses
the final script with Node.

## Manual authenticated matrix

Use a test YouTube account and remove test comments at the end.

| Surface | Required checks |
| --- | --- |
| Top-level comments | Create, reload, edit with prefilled text, delete, reopen video and confirm ownership actions remain. |
| Replies | Open replies, reply from the field and overflow menu, confirm mention insertion, edit/delete owned reply. |
| Locked content | Confirm top-level or thread composer is disabled with a lock and no request is sent. |
| Comment reactions | Like, clear like, dislike, clear dislike, switch reaction, confirm selected color/count refresh. |
| Video reactions | Toggle Polycentric and platform rows independently; verify one backend never changes the other. |
| Dislike estimate | Real zero renders `0`; disabled/unavailable estimate is gray and non-interactive while loading and after failure. |
| Destinations | Switch Polycentric/Platform repeatedly; verify composer label and submit backend always match the selected category. |
| Lifecycle | Rotate/recreate activity, leave/reopen video, sign out/in, and retest ownership and capabilities. |

## Failure diagnostics

Capture the structured plugin error category, HTTP status, response command
shape (with credentials/tokens removed), active source version, app build type,
and whether the same action succeeds on YouTube's mobile site. Never attach
cookies, authorization headers, visitor data, or action tokens to a public bug
report.
