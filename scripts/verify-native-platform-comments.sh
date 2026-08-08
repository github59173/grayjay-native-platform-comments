#!/usr/bin/env bash
set -euo pipefail

repository_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
stable_plugin="$repository_root/app/src/stable/assets/sources/youtube"
unstable_plugin="$repository_root/app/src/unstable/assets/sources/youtube"

stable_revision="$(git -C "$stable_plugin" rev-parse HEAD)"
unstable_revision="$(git -C "$unstable_plugin" rev-parse HEAD)"
if [[ "$stable_revision" != "$unstable_revision" ]]; then
  echo "Stable and unstable YouTube plugins must pin the same revision." >&2
  exit 1
fi

node --test "$repository_root/scripts/youtube-comments-surface.test.mjs"

npm --prefix "$stable_plugin" run verify
npm --prefix "$unstable_plugin" run verify

"$repository_root/gradlew" :app:testStableDebugUnitTest \
  --tests com.futo.platformplayer.PlatformCommentMutationTests \
  --tests com.futo.platformplayer.PlatformVideoReactionTests

"$repository_root/gradlew" :app:testUnstableDebugUnitTest \
  --tests com.futo.platformplayer.PlatformCommentMutationTests \
  --tests com.futo.platformplayer.PlatformVideoReactionTests
