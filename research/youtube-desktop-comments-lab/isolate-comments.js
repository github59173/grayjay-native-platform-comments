(() => {
  'use strict';

  if (window.__grayjayExactCommentsLab) return;

  const ROOT_SELECTOR = 'ytd-comments#comments';
  const HIDDEN_ATTRIBUTE = 'data-grayjay-isolation-hidden';
  const PLAYER_SELECTORS = [
    'ytd-watch-flexy #player-container-outer',
    'ytd-watch-flexy #player-container-inner',
    'ytd-watch-flexy #player-full-bleed-container',
    'ytd-watch-flexy #full-bleed-container',
    'ytd-watch-flexy ytd-player',
    'ytd-watch-flexy #player',
    '#movie_player',
    'video',
    'audio'
  ];
  const PRESERVE_SELECTORS = [
    'script',
    'style',
    'link[rel="stylesheet"]',
    'ytd-popup-container',
    'tp-yt-iron-dropdown',
    'tp-yt-paper-dialog',
    'ytd-menu-popup-renderer',
    'ytd-modal-with-title-and-button-renderer',
    'ytd-consent-bump-v2-lightbox',
    '[role="dialog"]',
    '[role="menu"]'
  ];

  const css = String.raw`
    [${HIDDEN_ATTRIBUTE}] { display: none !important; }

    ytd-watch-flexy #player-container-outer,
    ytd-watch-flexy #player-container-inner,
    ytd-watch-flexy #player-full-bleed-container,
    ytd-watch-flexy #full-bleed-container,
    ytd-watch-flexy ytd-player,
    ytd-watch-flexy #player,
    #movie_player,
    video,
    audio {
      display: none !important;
      width: 0 !important;
      height: 0 !important;
      min-width: 0 !important;
      min-height: 0 !important;
      margin: 0 !important;
      padding: 0 !important;
      visibility: hidden !important;
    }

    html[data-grayjay-comments-isolated],
    html[data-grayjay-comments-isolated] body {
      min-width: 0 !important;
      width: 100% !important;
      margin: 0 !important;
      padding: 0 !important;
      overflow-x: hidden !important;
      background: var(--yt-spec-base-background, #0f0f0f) !important;
    }

    html[data-grayjay-comments-isolated] ytd-comments#comments {
      box-sizing: border-box !important;
      display: block !important;
      width: min(100%, var(--grayjay-comments-width, 880px)) !important;
      max-width: none !important;
      min-width: 0 !important;
      margin: 0 auto !important;
      padding: 0 16px 48px !important;
      position: relative !important;
      visibility: visible !important;
    }

    html[data-grayjay-comments-isolated] ytd-comments#comments * {
      max-width: 100%;
    }

    html[data-grayjay-hide-count] ytd-comments-header-renderer #leading-section,
    html[data-grayjay-hide-count] ytd-comments-header-renderer #count,
    html[data-grayjay-hide-count] ytd-comments-header-renderer h2 {
      display: none !important;
    }

    html[data-grayjay-hide-sort] ytd-comments-header-renderer #additional-section,
    html[data-grayjay-hide-sort] ytd-comments-header-renderer #filter-menu,
    html[data-grayjay-hide-sort] ytd-comments-header-renderer yt-chip-cloud-renderer {
      display: none !important;
    }

    html[data-grayjay-hide-composer] ytd-comments-header-renderer #simple-box,
    html[data-grayjay-hide-composer] ytd-comment-simplebox-renderer {
      display: none !important;
    }

    html[data-grayjay-hide-avatars] ytd-comment-view-model #author-thumbnail,
    html[data-grayjay-hide-avatars] ytd-comment-renderer #author-thumbnail {
      display: none !important;
    }

    html[data-grayjay-hide-pinned] ytd-comment-view-model #pinned-comment-badge,
    html[data-grayjay-hide-pinned] ytd-comment-renderer #pinned-comment-badge {
      display: none !important;
    }

    html[data-grayjay-hide-badges] ytd-comment-view-model #author-comment-badge,
    html[data-grayjay-hide-badges] ytd-comment-renderer #author-comment-badge {
      display: none !important;
    }

    html[data-grayjay-hide-timestamps] ytd-comment-view-model #published-time-text,
    html[data-grayjay-hide-timestamps] ytd-comment-renderer #published-time-text {
      display: none !important;
    }

    html[data-grayjay-hide-likes] ytd-comment-engagement-bar #like-button,
    html[data-grayjay-hide-likes] ytd-comment-engagement-bar #vote-count-middle,
    html[data-grayjay-hide-likes] ytd-comment-action-buttons-renderer #like-button,
    html[data-grayjay-hide-likes] ytd-comment-action-buttons-renderer #vote-count-middle {
      display: none !important;
    }

    html[data-grayjay-hide-dislikes] ytd-comment-engagement-bar #dislike-button,
    html[data-grayjay-hide-dislikes] ytd-comment-action-buttons-renderer #dislike-button {
      display: none !important;
    }

    html[data-grayjay-hide-reply-action] ytd-comment-engagement-bar #reply-button-end,
    html[data-grayjay-hide-reply-action] ytd-comment-action-buttons-renderer #reply-button-end {
      display: none !important;
    }

    html[data-grayjay-hide-reply-expanders] ytd-comment-replies-renderer {
      display: none !important;
    }

    html[data-grayjay-hide-hearts] ytd-comment-engagement-bar #creator-heart,
    html[data-grayjay-hide-hearts] ytd-comment-action-buttons-renderer #creator-heart {
      display: none !important;
    }

    html[data-grayjay-hide-menus] ytd-comment-view-model #action-menu,
    html[data-grayjay-hide-menus] ytd-comment-renderer #action-menu {
      display: none !important;
    }

    html[data-grayjay-hide-connectors] ytd-comment-replies-renderer #expander-contents::before,
    html[data-grayjay-hide-connectors] ytd-comment-replies-renderer #expander-contents::after,
    html[data-grayjay-hide-connectors] ytd-comment-thread-renderer #replies::before {
      display: none !important;
    }

    html[data-grayjay-compact] ytd-comment-thread-renderer {
      margin-bottom: 8px !important;
    }

    html[data-grayjay-compact] ytd-comment-view-model #body,
    html[data-grayjay-compact] ytd-comment-renderer #body {
      padding-top: 4px !important;
      padding-bottom: 4px !important;
    }
  `;

  const style = document.createElement('style');
  style.id = 'grayjay-exact-comments-style';
  style.textContent = css;
  (document.head || document.documentElement).appendChild(style);

  let isolated = true;
  let scheduled = false;
  let rootWasFound = false;

  const shouldPreserve = (element) => {
    if (!(element instanceof Element)) return true;
    return PRESERVE_SELECTORS.some((selector) => {
      try {
        return element.matches(selector) || Boolean(element.querySelector(selector));
      } catch (_) {
        return false;
      }
    });
  };

  const clearIsolationMarkers = () => {
    document.querySelectorAll(`[${HIDDEN_ATTRIBUTE}]`).forEach((element) => {
      element.removeAttribute(HIDDEN_ATTRIBUTE);
    });
    document.documentElement.removeAttribute('data-grayjay-comments-isolated');
  };

  const removeVideoPlayer = () => {
    document.querySelectorAll('video, audio').forEach((media) => {
      try {
        media.muted = true;
        media.pause();
        media.removeAttribute('src');
        media.querySelectorAll('source').forEach((source) => source.removeAttribute('src'));
        media.load();
      } catch (_) {}
    });

    PLAYER_SELECTORS.forEach((selector) => {
      document.querySelectorAll(selector).forEach((element) => {
        if (!element.closest(ROOT_SELECTOR)) element.remove();
      });
    });
  };

  const isolate = () => {
    scheduled = false;
    removeVideoPlayer();

    const root = document.querySelector(ROOT_SELECTOR);
    if (!root) return false;

    rootWasFound = true;
    root.setAttribute('data-grayjay-comments-root', 'ready');
    root.scrollIntoView({ block: 'start', inline: 'nearest' });

    clearIsolationMarkers();
    if (!isolated) return true;

    const path = new Set();
    let cursor = root;
    while (cursor && cursor instanceof Element) {
      path.add(cursor);
      cursor = cursor.parentElement;
    }

    cursor = root.parentElement;
    while (cursor && cursor instanceof Element) {
      Array.from(cursor.children).forEach((child) => {
        if (!path.has(child) && !shouldPreserve(child)) {
          child.setAttribute(HIDDEN_ATTRIBUTE, '');
        }
      });
      cursor = cursor.parentElement;
    }

    document.documentElement.setAttribute('data-grayjay-comments-isolated', '');
    return true;
  };

  const scheduleIsolation = () => {
    if (scheduled) return;
    scheduled = true;
    window.requestAnimationFrame(isolate);
  };

  const setHidden = (key, hidden) => {
    const attribute = `data-grayjay-${key}`;
    if (hidden) document.documentElement.setAttribute(attribute, '');
    else document.documentElement.removeAttribute(attribute);
  };

  const setWidth = (width) => {
    const safeWidth = Math.max(360, Math.min(1200, Number(width) || 880));
    document.documentElement.style.setProperty('--grayjay-comments-width', `${safeWidth}px`);
  };

  const setIsolated = (value) => {
    isolated = Boolean(value);
    scheduleIsolation();
  };

  const reset = () => {
    Array.from(document.documentElement.attributes).forEach((attribute) => {
      if (attribute.name.startsWith('data-grayjay-hide-') || attribute.name === 'data-grayjay-compact') {
        document.documentElement.removeAttribute(attribute.name);
      }
    });
    setWidth(880);
    isolated = true;
    scheduleIsolation();
  };

  const status = () => ({
    rootFound: Boolean(document.querySelector(ROOT_SELECTOR)),
    rootWasFound,
    isolated,
    threads: document.querySelectorAll('ytd-comment-thread-renderer').length,
    replies: document.querySelectorAll('ytd-comment-replies-renderer ytd-comment-view-model').length,
    players: document.querySelectorAll(PLAYER_SELECTORS.join(',')).length,
    url: location.href
  });

  window.__grayjayExactCommentsLab = {
    rootSelector: ROOT_SELECTOR,
    isolate,
    reset,
    setHidden,
    setIsolated,
    setWidth,
    status
  };

  const observer = new MutationObserver(scheduleIsolation);
  observer.observe(document.documentElement, { childList: true, subtree: true });

  document.addEventListener('DOMContentLoaded', scheduleIsolation, { once: true });
  window.addEventListener('yt-navigate-finish', scheduleIsolation);
  window.addEventListener('load', scheduleIsolation, { once: true });
  window.setInterval(() => {
    removeVideoPlayer();
    if (!rootWasFound || !document.querySelector(`${ROOT_SELECTOR}[data-grayjay-comments-root="ready"]`)) {
      scheduleIsolation();
    }
  }, 1000);

  scheduleIsolation();
})();
