#!/bin/zsh
set -euo pipefail

lab_dir="${0:A:h}"
lab_binary="${TMPDIR:-/tmp}/grayjay-exact-youtube-comments-lab"
module_cache="${TMPDIR:-/tmp}/grayjay-swift-module-cache"
macos_sdk="/Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk"

xcrun swiftc \
  -swift-version 5 \
  -sdk "$macos_sdk" \
  -module-cache-path "$module_cache" \
  -framework AppKit \
  -framework WebKit \
  "$lab_dir/ExactCommentsLab.swift" \
  -o "$lab_binary"

exec "$lab_binary" "$lab_dir/isolate-comments.js"
