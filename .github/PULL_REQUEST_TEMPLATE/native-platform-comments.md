## Summary

<!-- Explain the user-visible behavior and why it belongs in core. -->

## Companion plugin revision

<!-- Link the exact plugin commit that implements the source side. -->

## Compatibility

- [ ] Existing source plugins continue to work through default unsupported methods.
- [ ] Polycentric-only and read-only comment paths were retested.
- [ ] No platform endpoint/authentication logic was added to Kotlin app code.

## Verification

- [ ] Plugin `npm run verify` passes.
- [ ] Stable and unstable focused unit tests pass.
- [ ] Stable and unstable debug APKs assemble.
- [ ] Authenticated manual matrix in `docs/native-platform-comments/TESTING.md` passes.
- [ ] Test comments were deleted.

## Security/privacy

- [ ] Logs and fixtures contain no cookies, headers, action tokens, or account data.
- [ ] Unknown capabilities/response shapes fail closed.

## Contributor requirements

- [ ] I followed `CONTRIBUTION.md` and the existing code style.
- [ ] Documentation and tests cover the change.
- [ ] I have completed the FUTO CLA required for core contributions.
