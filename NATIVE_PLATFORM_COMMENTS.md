# Native platform comments and reactions

This repository is a complete, reviewable reference implementation for adding
native source comments and video reactions to Grayjay. It starts from the
current Grayjay Android source and keeps platform-specific request construction
in the source plugin.

The matching YouTube implementation lives in
[grayjay-plugin-youtube-native-comments](https://github.com/github59173/grayjay-plugin-youtube-native-comments)
and is pinned as both the stable and unstable YouTube submodule.

## What is implemented

- Create, reply to, edit, and delete native platform comments.
- Like, dislike, and clear reactions on comments when the source supports them.
- Derive ownership and available actions from fresh platform comment data, so
  edit/delete still work after reopening a video.
- Expose locked or otherwise non-commentable threads in the composer and menus.
- Keep Polycentric and native-platform comment destinations independent.
- Add source-colored, stacked Polycentric/platform video reaction rows.
- Use the official platform like count and an optional external estimate for a
  removed platform dislike count; unavailable estimates remain disabled.
- Preserve plugin authentication and transport boundaries: the app invokes
  source capabilities, while the plugin owns platform endpoints, tokens, and
  response parsing.

## Repository baseline

| Component | Upstream | Pinned revision |
| --- | --- | --- |
| Grayjay Android | `futo-org/grayjay-android` | `993a9bd850f022f952460b1dfc0744b98e0c23b4` |
| YouTube plugin | `futo-org/grayjay-plugin-youtube` | `36ae88e34905545d5eaa8c8152fd09a48461d756` |

The untouched app baseline is tagged `upstream-grayjay-993a9bd`. The plugin
repository carries its own upstream tag and history.

## Start here

- [Architecture](docs/native-platform-comments/ARCHITECTURE.md)
- [Implementation breakdown](docs/native-platform-comments/IMPLEMENTATION.md)
- [Build and test](docs/native-platform-comments/TESTING.md)
- [Upstream contribution plan](docs/native-platform-comments/UPSTREAMING.md)
- [Security and privacy](docs/native-platform-comments/SECURITY.md)
- [Source provenance](docs/native-platform-comments/PROVENANCE.md)

## Quick verification

Prerequisites are Git LFS, recursive submodules, Android SDK 36, and a JDK
compatible with the checked-in Gradle wrapper.

```bash
git clone --recurse-submodules https://github.com/github59173/grayjay-native-platform-comments.git
cd grayjay-native-platform-comments
git lfs pull
./scripts/verify-native-platform-comments.sh
```

The script verifies both embedded YouTube copies, checks that they are pinned to
one revision, and runs the focused Android unit tests. See the testing guide for
full debug APK builds and the manual authenticated test matrix.

## Project status

This is a development/reference build, not a release channel. No pull request or
issue has been opened against FUTO. The repository is arranged to make review
and future upstreaming explicit, but any contribution would still require the
maintainers' approval and the FUTO CLA described in `CONTRIBUTION.md`.
