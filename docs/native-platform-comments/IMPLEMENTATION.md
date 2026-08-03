# Implementation breakdown

This document maps the feature into reviewable layers. A production upstream
submission should preserve these boundaries even if maintainers request smaller
pull requests.

## 1. Public source contract

| Area | Principal files | Responsibility |
| --- | --- | --- |
| Plugin declarations | `app/src/main/assets/devportal/plugin.d.ts`, `app/src/main/assets/scripts/source.js` | Document and expose optional plugin entry points. |
| Client interface | `IPlatformClient.kt`, `PlatformClientCapabilities.kt` | Add source-neutral capability and mutation methods with compatible defaults. |
| Comment models | `PlatformCommentMutation.kt`, `PlatformCommentingState.kt`, `IPlatformComment.kt` | Describe actions, ownership, locks, results, and safe metadata. |
| Video models | `PlatformVideoReaction.kt` | Describe platform reaction state/results and error categories. |

## 2. JavaScript bridge

`JSClient.kt` discovers plugin functions, invokes them through the authenticated
source runtime, and converts structured results. `JSComment.kt`,
`JSCommentPager.kt`, `JSCommentMutationResult.kt`, and
`JSVideoReactionResult.kt` form the serialization boundary. `Extensions_V8.kt`
and `UIDialogs.kt` translate runtime failures into stable app behavior.

No YouTube endpoint or token format is implemented in Kotlin. Those details are
in the companion plugin.

## 3. State and mutation coordination

`StatePlatform.kt` is the app-facing service for native comment and video
reaction actions. `CommentMutationCoordinator.kt` prevents duplicate mutations,
checks action invariants, and centralizes result/error handling.

The implementation refreshes only the affected comment surface after an
acknowledged mutation. It does not insert an optimistic comment before YouTube
confirms the request.

## 4. Comment UI

| UI | Behavior added |
| --- | --- |
| `CommentsList.kt` / `CommentsFragment.kt` | Destination-aware composer, native action routing, refresh. |
| `RepliesOverlay.kt` / `CommentsModalBottomSheet.kt` | Reply composer, locked-thread state, replies refresh. |
| `CommentDialog.kt` / `dialog_comment.xml` | Create/reply/edit modes; existing text prefill; destination label. |
| `CommentViewHolder.kt` / `list_comment.xml` | Source-colored reactions and ownership-aware overflow actions. |
| Article/post detail fragments | Preserve destination-specific behavior outside video detail. |

Copy remains a menu action. Edit/Delete appear only for a comment the active
platform account owns. Reply inserts the target handle when appropriate.

## 5. Video UI

`PillRatingLikesDislikes.kt`, `rating_likesdislikes.xml`, and
`VideoDetailView.kt` render two compact independent rows inside one control:
Polycentric on top and the native platform below. The separator and selection
colors communicate the backend without extra `PC` or `YT` labels. Generic
source accent colors make the design reusable by other plugins.

## 6. Resources and localization

New strings are present in every existing locale resource. Layout resources add
the comment lock state and the shared video-action corner treatment. The source
accent fallback lives in normal app colors rather than YouTube-specific UI code.

## 7. Companion YouTube plugin

The pinned companion repository implements:

- MWEB/WEB comment create, reply, edit, and delete commands;
- like, dislike, and neutral/clear comment reactions;
- video reaction state and mutations;
- ownership/action metadata extraction;
- locked-thread/commenting-state extraction;
- module embedding checks so the shipped `YoutubeScript.js` cannot drift from
  its reviewed source modules.

## Suggested upstream review slices

1. Contract models, capability defaults, JS declarations, and unit tests.
2. JS bridge and structured error handling.
3. Comment state orchestration and comment/reply UI.
4. Generic source accent color and comment reaction styling.
5. Independent platform video-reaction row.
6. YouTube plugin implementation and its JavaScript tests.

Keeping these commits independently buildable makes regressions and API review
substantially easier than one mixed UI/endpoint patch.
