(() => {
  'use strict';

  if (window.__grayjayYouTubeCommentsSurface) return;

  const rootSelector = 'ytd-comments#comments';
  const channelNavigationScheme = 'grayjay-comments';
  const hiddenAttribute = 'data-grayjay-comments-hidden';
  const playerSelectors = [
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
  const preserveSelectors = [
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

  const style = document.createElement('style');
  style.id = 'grayjay-youtube-comments-style';
  style.textContent = `
    [${hiddenAttribute}],
    ${playerSelectors.join(',')} {
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
    html[data-grayjay-comments-isolated] body,
    html[data-grayjay-comments-isolated] ytd-app,
    html[data-grayjay-comments-isolated] ytd-page-manager,
    html[data-grayjay-comments-isolated] ytd-watch-flexy,
    html[data-grayjay-comments-isolated] #columns,
    html[data-grayjay-comments-isolated] #primary,
    html[data-grayjay-comments-isolated] #primary-inner,
    html[data-grayjay-comments-isolated] #below {
      box-sizing: border-box !important;
      min-width: 0 !important;
      max-width: none !important;
      width: 100% !important;
      margin: 0 !important;
      padding: 0 !important;
      overflow-x: hidden !important;
      background: __GRAYJAY_BACKGROUND__ !important;
      color: __GRAYJAY_FOREGROUND__ !important;
    }

    html[data-grayjay-comments-isolated] {
      --yt-spec-base-background: __GRAYJAY_BACKGROUND__ !important;
      --yt-spec-brand-background-solid: __GRAYJAY_BACKGROUND__ !important;
      --yt-spec-general-background-a: __GRAYJAY_BACKGROUND__ !important;
      color-scheme: dark !important;
    }

    html[data-grayjay-comments-isolated] ytd-comments#comments {
      box-sizing: border-box !important;
      display: block !important;
      min-width: 0 !important;
      max-width: none !important;
      width: 100% !important;
      margin: 0 !important;
      padding: 0 12px 48px !important;
      position: relative !important;
      visibility: visible !important;
      background: __GRAYJAY_BACKGROUND__ !important;
      color: __GRAYJAY_FOREGROUND__ !important;
    }

    html[data-grayjay-comments-isolated] ytd-comments#comments > #sections,
    html[data-grayjay-comments-isolated] ytd-comments#comments ytd-item-section-renderer,
    html[data-grayjay-comments-isolated] ytd-comments#comments ytd-comments-header-renderer {
      background: __GRAYJAY_BACKGROUND__ !important;
    }

    html[data-grayjay-comments-isolated] ytd-comments#comments * {
      max-width: 100% !important;
    }

    html[data-grayjay-hide-count] ytd-comments-header-renderer #leading-section,
    html[data-grayjay-hide-count] ytd-comments-header-renderer #count,
    html[data-grayjay-hide-count] ytd-comments-header-renderer h2,
    html[data-grayjay-hide-sort] ytd-comments-header-renderer #additional-section,
    html[data-grayjay-hide-sort] ytd-comments-header-renderer #filter-menu,
    html[data-grayjay-hide-sort] ytd-comments-header-renderer yt-chip-cloud-renderer {
      display: none !important;
    }

    /* The desktop renderer reserves a full title row even after both of its
       visible controls are hidden. Collapse that empty row, but leave the
       official composer immediately below it intact. */
    html[data-grayjay-hide-count][data-grayjay-hide-sort]
      ytd-comments-header-renderer #title {
      display: none !important;
      min-height: 0 !important;
      height: 0 !important;
      margin: 0 !important;
      padding: 0 !important;
      overflow: hidden !important;
    }

    html[data-grayjay-comments-isolated]
      ytd-comments-header-renderer {
      margin-top: 8px !important;
      padding-top: 0 !important;
    }

    html[data-grayjay-comments-isolated]
      ytd-comments-header-renderer #simple-box,
    html[data-grayjay-comments-isolated]
      ytd-comments-header-renderer ytd-comment-simplebox-renderer {
      margin-top: 0 !important;
      padding-top: 0 !important;
    }
  `;
  (document.head || document.documentElement).appendChild(style);

  document.documentElement.setAttribute('data-grayjay-hide-count', '');
  document.documentElement.setAttribute('data-grayjay-hide-sort', '');

  let scheduled = false;
  let rootFound = false;

  const removePlayer = () => {
    document.querySelectorAll('video, audio').forEach((media) => {
      try {
        media.muted = true;
        media.pause();
        media.removeAttribute('src');
        media.querySelectorAll('source').forEach((source) => source.removeAttribute('src'));
        media.load();
      } catch (_) {}
    });
    playerSelectors.forEach((selector) => {
      document.querySelectorAll(selector).forEach((element) => {
        if (!element.closest(rootSelector)) element.remove();
      });
    });
  };

  const shouldPreserve = (element) => {
    if (!(element instanceof Element)) return true;
    return preserveSelectors.some((selector) => {
      try {
        return element.matches(selector) || Boolean(element.querySelector(selector));
      } catch (_) {
        return false;
      }
    });
  };

  const isYouTubeChannelUrl = (candidate) => {
    try {
      const url = new URL(candidate, window.location.href);
      const host = url.hostname.toLowerCase().replace(/\.$/, '');
      if (url.protocol !== 'https:' ||
          (host !== 'youtube.com' && !host.endsWith('.youtube.com'))) return null;
      const segments = url.pathname.split('/').filter(Boolean).map(decodeURIComponent);
      const first = segments[0] || '';
      if ((first.startsWith('@') && first.length > 1) ||
          (['channel', 'c', 'user'].includes(first) && Boolean(segments[1]))) {
        return url.href;
      }
    } catch (_) {}
    return null;
  };

  const routeChannelClick = (event) => {
    const path = typeof event.composedPath === 'function' ? event.composedPath() : [];
    const anchor = path.find((node) => node instanceof HTMLAnchorElement && node.href) ||
      (event.target instanceof Element ? event.target.closest('a[href]') : null);
    if (!anchor) return;

    const root = document.querySelector(rootSelector);
    const isInsideComments = root && (root.contains(anchor) || path.includes(root));
    const channelUrl = isInsideComments ? isYouTubeChannelUrl(anchor.href) : null;
    if (!channelUrl) return;

    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();
    window.location.assign(
      `${channelNavigationScheme}://channel?url=${encodeURIComponent(channelUrl)}`
    );
  };

  const isolate = () => {
    scheduled = false;
    removePlayer();

    const root = document.querySelector(rootSelector);
    if (!root) return false;

    const firstIsolation = !document.documentElement.hasAttribute('data-grayjay-comments-isolated');
    rootFound = true;
    root.setAttribute('data-grayjay-comments-root', 'ready');
    if (firstIsolation) root.scrollIntoView({ block: 'start', inline: 'nearest' });

    document.querySelectorAll(`[${hiddenAttribute}]`).forEach((element) => {
      element.removeAttribute(hiddenAttribute);
    });

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
          child.setAttribute(hiddenAttribute, '');
        }
      });
      cursor = cursor.parentElement;
    }

    document.documentElement.setAttribute('data-grayjay-comments-isolated', '');
    if (firstIsolation) requestAnimationFrame(() => window.scrollTo(0, 0));
    return true;
  };

  const schedule = () => {
    if (scheduled) return;
    scheduled = true;
    requestAnimationFrame(isolate);
  };

  window.__grayjayYouTubeCommentsSurface = {
    isolate,
    status: () => ({
      ready: Boolean(document.querySelector(`${rootSelector}[data-grayjay-comments-root="ready"]`)),
      rootFound,
      threads: document.querySelectorAll('ytd-comment-thread-renderer').length,
      players: document.querySelectorAll(playerSelectors.join(',')).length
    })
  };

  const observer = new MutationObserver(schedule);
  observer.observe(document.documentElement, { childList: true, subtree: true });
  document.addEventListener('click', routeChannelClick, true);
  document.addEventListener('DOMContentLoaded', schedule, { once: true });
  window.addEventListener('load', schedule, { once: true });
  window.addEventListener('yt-navigate-finish', schedule);
  setInterval(() => {
    removePlayer();
    if (!rootFound || !document.querySelector(rootSelector)) schedule();
  }, 1000);
  schedule();
})();
