# Architecture

## Design boundary

The app owns generic capability contracts, UI state, orchestration, and error
presentation. A source plugin owns every platform-specific endpoint, request
token, authentication detail, and response parser.

```text
Video/comment UI
       |
StatePlatform + CommentMutationCoordinator
       |
IPlatformClient capability contract
       |
JSClient serialization boundary
       |
Source plugin (authenticated platform requests)
       |
Platform web API
```

This follows Grayjay's existing source model: logged-in cookies and plugin HTTP
clients stay inside the source runtime rather than being copied into app code.

## Capability negotiation

`PlatformClientCapabilities` advertises comment creation, reply, edit, delete,
comment reactions, comment-state discovery, and video-reaction support. The UI
only enables an action when both the source capability and the per-item state
permit it. Older plugins remain compatible because all new methods have safe
unsupported defaults.

`PlatformCommentingState` describes whether a video or thread accepts comments
and provides a user-facing reason when it does not. `PlatformCommentMutation`
and `PlatformVideoReactionResult` return structured errors instead of leaking
source exceptions into views.

## Comment flow

1. The selected comment category determines the destination.
2. The app asks the active source for comment/thread state.
3. The dialog is populated with the current comment for edits or a mention for
   replies selected from another user's menu.
4. `CommentMutationCoordinator` serializes mutation work and rejects invalid or
   unsupported requests before crossing the JS boundary.
5. `JSClient` serializes comment metadata without nullable map values and calls
   the matching plugin function.
6. On success, the visible pager is refreshed and menu state is recomputed from
   the newly loaded comment.

Ownership is derived from source-provided comment metadata for the active
account. It is not tied to an app-local database of comments created during the
current session.

## Comment menus and locks

Every comment keeps Copy in its overflow menu. Reply is exposed for replyable
third-party comments. Edit and Delete are exposed only when the platform marks
the comment as owned and supplies the corresponding operation metadata.

The top-level and reply composers render disabled lock states when YouTube says
the associated surface cannot accept a comment. A missing replies preview is
not treated by itself as proof that a thread is locked; the plugin parses the
available command/state metadata.

## Video reactions

The existing Polycentric reaction state and the platform reaction state are
independent rows inside one compact control. Mutating one row never posts the
same action to the other backend.

Platform like counts and reaction state come from the source. YouTube dislike
counts may be supplemented by Return YouTube Dislike through Grayjay's existing
data path. A real zero is displayed as `0`; unknown or disabled data is shown as
unavailable and cannot be tapped.

## Source colors

`SourcePluginConfig.accentColor` lets a source provide a presentation color.
`PlatformAccentColor` validates and resolves it with a neutral fallback. The
same source color is used for platform video reactions and native comment
reaction selection, allowing future plugins to opt in without hard-coded
YouTube checks.

## Compatibility

- New interface methods default to unsupported/no-op values.
- New serialized fields are optional or have defaults.
- Polycentric-only and read-only sources retain their existing behavior.
- Platform-specific behavior is selected through capability flags, never source
  name string matching.
