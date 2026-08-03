# Upstream contribution plan

This repository has not opened a FUTO issue or pull request. It is a complete
reference implementation prepared so a maintainer can evaluate the scope before
any upstream contact occurs.

## Requirements from the included contributor guide

Grayjay's `CONTRIBUTION.md` asks core contributors to:

1. Fork the core repository.
2. Clone the fork.
3. Make and document the changes.
4. Commit and push the changes.
5. Open a pull request.
6. Follow the existing style and include documentation/tests where applicable.
7. Sign the FUTO Individual Contributor License Agreement for core work.

Official plugins are AGPL and use the same fork/change/commit/push/pull-request
flow. Core code remains governed by the repository's Source First license; this
reference repository does not change either license.

## Before proposing upstream

- Rebase both repositories onto the then-current upstream default branches.
- Regenerate and verify `YoutubeScript.js` after resolving plugin conflicts.
- Run plugin tests, focused unit tests, both debug variants, and the authenticated
  manual matrix in `TESTING.md`.
- Remove test comments and scrub logs/screenshots of credentials and tokens.
- Confirm every new string is represented in all locale files.
- Confirm unsupported plugins still render the original read-only/Polycentric
  paths.
- Split the change into the review slices in `IMPLEMENTATION.md` if requested.
- Sign the CLA before submitting core changes.

## Recommended pull-request narrative

An eventual proposal should explain the user problem, capability/API design,
compatibility defaults, authentication boundary, UI behavior, test evidence,
and known limitations. It should link the matching plugin revision and pin the
app's submodule to that revision.

Avoid presenting YouTube parsing as a stable API. The source plugin deliberately
owns that volatile behavior, and structured `UPSTREAM_RESPONSE_CHANGED` errors
make failures diagnosable without crashing the app.

## Suggested acceptance criteria

- Existing sources compile without implementing new methods.
- Read-only comments and Polycentric behavior do not regress.
- Native actions are never offered without source and item-level capability.
- Owned-comment actions survive a close/reopen cycle without a local ownership
  database.
- Clearing reactions sends the source's neutral action and updates selection.
- Locked composers and reply actions are visibly disabled.
- The app contains no YouTube cookies, request tokens, or endpoint construction.
- Automated and manual checks in `TESTING.md` pass.
