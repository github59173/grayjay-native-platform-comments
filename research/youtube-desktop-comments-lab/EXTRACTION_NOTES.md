# Exact YouTube desktop comments lab

This research lab loads YouTube's complete desktop watch page for the Rickroll
test video:

```text
https://www.youtube.com/watch?v=dQw4w9WgXcQ&app=desktop
```

It does **not** parse YouTube JSON, proxy Innertube requests, copy comment data,
or reconstruct YouTube's comment renderers. A persistent macOS `WKWebView`
loads YouTube directly, so `ytd-comments#comments`, its custom elements,
continuations, menus, dialogs, authentication state, and event handlers remain
attached to YouTube's own page runtime.

## Extraction boundary

The smallest complete live surface observed on the desktop watch page is:

```css
ytd-comments#comments
```

Its observed ancestor path is:

```text
ytd-comments#comments
div.box
div#below
div#primary-inner
div#primary
div#columns
ytd-watch-flexy[role=main]
ytd-page-manager#page-manager
```

The comments element is not portable HTML. Removing it from this ancestor tree
also removes it from the YouTube custom-element runtime and continuation state.
The lab therefore leaves the element in place and marks unrelated sibling
branches as hidden.

## Files

- `ExactCommentsLab.swift` creates the native resizable WebView and inspection
  controls.
- `isolate-comments.js` runs at document start, removes the video player and
  media nodes, locates the official comments root, hides unrelated page
  branches, preserves popup/dialog containers, and reapplies those rules after
  YouTube SPA mutations.
- `run-lab.sh` builds the disposable macOS binary in the temporary directory
  and launches it.

There is intentionally no local HTTP server, comment parser, fixture markup, or
custom comment renderer.

The WebView also blocks `googlevideo.com` media resources before the watch page
loads. Player containers, `<video>`, and `<audio>` nodes are removed whenever
YouTube's SPA attempts to recreate them; no duplicate playback surface is kept.

## Run

From this directory:

```bash
./run-lab.sh
```

The window opens the official desktop comments root at 880 px. Resize the
window or use the width slider. The checkboxes apply CSS only; unchecked state
is YouTube's untouched UI. Right-click a YouTube element and choose **Inspect
Element** to inspect the live DOM.

## Controls and selectors

| Control | Official desktop selector seam |
| --- | --- |
| Comment count | `ytd-comments-header-renderer #leading-section`, `#count` |
| Sort/filter | `#additional-section`, `#filter-menu`, `yt-chip-cloud-renderer` |
| Composer | `ytd-comment-simplebox-renderer` |
| Avatars | `ytd-comment-view-model #author-thumbnail` |
| Pinned label | `#pinned-comment-badge` |
| Author badges | `#author-comment-badge` |
| Timestamps | `#published-time-text` |
| Likes/count | `ytd-comment-engagement-bar #like-button`, `#vote-count-middle` |
| Dislikes | `ytd-comment-engagement-bar #dislike-button` |
| Reply action | `ytd-comment-engagement-bar #reply-button-end` |
| Reply expander | `ytd-comment-replies-renderer` |
| Creator heart | `ytd-comment-engagement-bar #creator-heart` |
| Action menu | `ytd-comment-view-model #action-menu` |

These selectors are a small, versioned maintenance seam. YouTube may change
them without notice.

## What “exact backend” means

YouTube's backend source code is private server-side software and is not
present in page source. The exact behavior available to a client is obtained by
letting YouTube's own desktop runtime make its own continuation and mutation
requests. This lab does that directly; it does not attempt to duplicate those
requests.

The WebView uses its own persistent website-data store. It does not read or
copy browser cookies. Whether posting, liking, editing, or deleting is available
depends on the WebView's YouTube sign-in state and Google's embedded sign-in
policy. Production Grayjay integration must use Grayjay's approved source-login
and cookie lifecycle rather than exporting credentials into this lab.

The Android experiment derived from this seam now lives in
`app/src/main/java/com/futo/platformplayer/views/comments/YouTubeCommentsWebView.kt`
and `app/src/main/assets/scripts/youtube_comments_surface.js`. Its default UI
policy hides the comment count and sort/filter controls while retaining
YouTube's composer and all comment/reply interactions.
