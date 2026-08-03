# Security and privacy notes

Native platform mutations act with the user's logged-in source session. The
implementation therefore treats plugin/app boundaries and action metadata as
security-sensitive.

- Authentication remains in Grayjay's existing source runtime and HTTP clients.
- The app does not persist YouTube cookies, authorization headers, visitor data,
  or mutation tokens in its own feature database.
- The plugin uses action metadata returned for the current account and item;
  Edit/Delete are not enabled from a display-name match alone.
- UI ownership hints are not authorization. YouTube remains the authority and
  must accept every mutation.
- Structured failures are user-safe and logs must redact headers, cookies,
  tokens, full authenticated response bodies, and account identifiers.
- Mutation metadata is treated as ephemeral. A refreshed comment replaces stale
  action metadata rather than merging token maps indefinitely.
- Unknown capability or response shapes fail closed: controls are disabled and
  no guessed mutation is sent.
- The optional dislike-estimate backend supplies a display count only; it is not
  used as authority for the user's YouTube reaction.

Do not report security-sensitive findings in public issues. Follow FUTO's
preferred private reporting channel if this work is ever proposed upstream.
