# Source provenance

## Grayjay Android

- Upstream repository: `https://github.com/futo-org/grayjay-android.git`
- Baseline revision: `993a9bd850f022f952460b1dfc0744b98e0c23b4`
- Local baseline tag: `upstream-grayjay-993a9bd`
- Baseline date inspected: 2026-08-02

## YouTube source plugin

- Upstream repository: `https://github.com/futo-org/grayjay-plugin-youtube.git`
- Baseline revision: `36ae88e34905545d5eaa8c8152fd09a48461d756`
- Companion repository:
  `https://github.com/github59173/grayjay-plugin-youtube-native-comments`
- Feature revision pinned by this app:
  `13e0377cd82acf98a16d2c38fb6d0338be42f103` in both YouTube submodule entries.

## Submodule URL normalization

The app's upstream `.gitmodules` uses relative GitLab URLs. A GitHub fork resolves
those relative paths under the fork owner and cannot clone them. This reference
repository uses the canonical absolute FUTO GitLab URLs for unchanged upstream
submodules and the public companion GitHub URL for the modified YouTube plugin.
No code was copied into those unchanged dependencies.

## Licensing

The original repository license files are retained unmodified. Grayjay core and
the official plugin have different licenses and contribution requirements; see
`LICENSE.md`, `CONTRIBUTION.md`, and the companion plugin's license. This
provenance record is informational and is not a relicensing statement.
