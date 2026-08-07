package com.futo.platformplayer.views.comments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.OverScroller
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.futo.platformplayer.R
import com.futo.platformplayer.Settings
import com.futo.platformplayer.SettingsDev
import com.futo.platformplayer.api.media.platforms.js.JSClient
import com.futo.platformplayer.logging.Logger
import com.futo.platformplayer.states.StateApp
import com.futo.platformplayer.states.StatePlatform
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger

@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
class YouTubeCommentsWebView(context: Context) : FrameLayout(context) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val themeBackgroundColor = ContextCompat.getColor(context, R.color.black)
    private val themeForegroundColor = ContextCompat.getColor(context, R.color.white)
    private val loadingView: View
    private var webView: NestedCommentsWebView? = null
    private var documentStartScript: ScriptHandler? = null
    private var canonicalUrl: String? = null
    private var expectedVideoId: String? = null
    private var fallbackDelivered = false
    private var destroyed = false
    private var pageReady = false
    private var readinessStartedAt = 0L
    private var loginRequestDelivered = false
    private val hydratedCookies = mutableListOf<Pair<String, String>>()

    var onFallback: ((String) -> Unit)? = null
    var onLoginRequired: (() -> Unit)? = null
    var onChannelUrl: ((String) -> Unit)? = null
    var onExternalUrl: ((String) -> Unit)? = null

    init {
        setBackgroundColor(themeBackgroundColor)
        loadingView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(themeBackgroundColor)
            setPadding(24, 48, 24, 48)
            addView(ProgressBar(context))
            addView(TextView(context).apply {
                text = context.getString(R.string.youtube_web_comments_loading)
                setTextColor(themeForegroundColor)
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 16, 0, 0)
            })
        }
        addView(loadingView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    fun load(videoUrl: String): Boolean {
        if (destroyed || StateApp.instance.privateMode) return false
        val nextCanonicalUrl = YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl(videoUrl) ?: return false
        val nextVideoId = YouTubeCommentsWebPolicy.extractVideoId(videoUrl) ?: return false
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return false

        if (canonicalUrl == nextCanonicalUrl && webView != null && !fallbackDelivered)
            return true

        canonicalUrl = nextCanonicalUrl
        expectedVideoId = nextVideoId
        fallbackDelivered = false
        pageReady = false
        loginRequestDelivered = false
        loadingView.visibility = View.VISIBLE

        if (!createWebView()) {
            Logger.w(TAG, "Official YouTube comments fallback: unsupported-webview")
            return false
        }
        hydratePluginCookiesAndLoad(videoUrl)
        return true
    }

    fun reloadWithPluginAuth() {
        val sourceUrl = canonicalUrl ?: return
        fallbackDelivered = false
        pageReady = false
        loginRequestDelivered = false
        loadingView.visibility = View.VISIBLE
        hydratePluginCookiesAndLoad(sourceUrl)
    }

    fun destroySurface() {
        if (destroyed) return
        destroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        documentStartScript?.let { runCatching { it.remove() } }
        documentStartScript = null
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        webView = null
        clearHydratedSessionCookiesIfRequested()
        removeAllViews()
    }

    private fun createWebView(): Boolean {
        documentStartScript?.let { runCatching { it.remove() } }
        documentStartScript = null
        webView?.let {
            removeView(it)
            it.stopLoading()
            it.destroy()
        }

        return try {
            val commentsWebView = NestedCommentsWebView(context)
            webView = commentsWebView
            addView(commentsWebView, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

            commentsWebView.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = false
                allowContentAccess = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                setSupportZoom(false)
                builtInZoomControls = false
                displayZoomControls = false
                useWideViewPort = false
                loadWithOverviewMode = false
                userAgentString = YouTubeCommentsWebPolicy.DESKTOP_USER_AGENT
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                commentsWebView.settings.safeBrowsingEnabled = true

            CookieManager.getInstance().apply {
                setAcceptCookie(true)
                setAcceptThirdPartyCookies(commentsWebView, false)
            }

            val isolationScript = context.assets.open("scripts/youtube_comments_surface.js")
                .bufferedReader()
                .use { it.readText() }
                .replace("__GRAYJAY_BACKGROUND__", themeBackgroundColor.toCssColor())
                .replace("__GRAYJAY_FOREGROUND__", themeForegroundColor.toCssColor())
            documentStartScript = WebViewCompat.addDocumentStartJavaScript(
                commentsWebView,
                isolationScript,
                setOf("https://www.youtube.com")
            )

            if (WebViewFeature.isFeatureSupported(WebViewFeature.START_SAFE_BROWSING))
                WebViewCompat.startSafeBrowsing(context) { }

            commentsWebView.webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    request?.deny()
                }

                override fun onCreateWindow(
                    view: WebView?,
                    isDialog: Boolean,
                    isUserGesture: Boolean,
                    resultMsg: android.os.Message?
                ): Boolean = false
            }
            commentsWebView.setDownloadListener { _, _, _, _, _ -> }
            commentsWebView.webViewClient = createClient()
            true
        } catch (_: Throwable) {
            documentStartScript?.let { runCatching { it.remove() } }
            documentStartScript = null
            webView?.let { failedWebView ->
                removeView(failedWebView)
                runCatching { failedWebView.stopLoading() }
                runCatching { failedWebView.destroy() }
            }
            webView = null
            false
        }
    }

    private fun createClient(): WebViewClient {
        return object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (YouTubeCommentsWebPolicy.isBlockedMediaHost(request?.url?.host)) {
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        ByteArrayInputStream(ByteArray(0))
                    )
                }
                return null
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                if (request == null) return true
                val url = request.url?.toString() ?: return true

                YouTubeCommentsWebPolicy.channelUrlFromSurfaceNavigation(url)?.let { channelUrl ->
                    routeChannelUrl(channelUrl)
                    return true
                }
                if (YouTubeCommentsWebPolicy.isSurfaceNavigation(url)) return true
                if (!request.isForMainFrame) return false

                val host = request.url?.host

                if (YouTubeCommentsWebPolicy.isGoogleSignInHost(host)) {
                    deliverLoginRequest()
                    return true
                }

                YouTubeCommentsWebPolicy.canonicalYouTubeChannelUrl(url)?.let { channelUrl ->
                    routeChannelUrl(channelUrl)
                    return true
                }

                val videoId = expectedVideoId
                if (videoId != null && YouTubeCommentsWebPolicy.isSameVideoNavigation(url, videoId))
                    return false

                routeExternalUrl(url)
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                scheduleReadinessPoll()
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                if (request?.isForMainFrame == true)
                    failOnce("http-${errorResponse?.statusCode ?: 0}")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                if (request?.isForMainFrame == true)
                    failOnce("navigation-error")
            }

            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                failOnce("renderer-crash")
                return true
            }

            @RequiresApi(Build.VERSION_CODES.O_MR1)
            override fun onSafeBrowsingHit(
                view: WebView?,
                request: WebResourceRequest?,
                threatType: Int,
                callback: SafeBrowsingResponse?
            ) {
                callback?.backToSafety(true)
                failOnce("safe-browsing")
            }
        }
    }

    private fun hydratePluginCookiesAndLoad(sourceUrl: String) {
        val targetWebView = webView ?: return
        val targetUrl = canonicalUrl ?: return
        val client = StatePlatform.instance.getContentClientOrNull(sourceUrl) as? JSClient
        val auth = client
            ?.takeIf { it.config.id == YouTubeCommentsWebPolicy.YOUTUBE_SOURCE_ID }
            ?.descriptor
            ?.getAuth()
        val cookieMap = auth?.cookieMap.orEmpty()
        val cookieManager = CookieManager.getInstance()
        val cookieLines = mutableListOf<Triple<String, String, String>>()

        for ((domain, cookies) in cookieMap) {
            if (!YouTubeCommentsWebPolicy.isAllowedCookieDomain(domain)) continue
            val normalizedDomain = "." + domain.trim().trimStart('.').lowercase()
            val origin = "https://" + normalizedDomain.trimStart('.')
            for ((name, value) in cookies) {
                if (!COOKIE_NAME.matches(name) || value.contains(';') || value.contains('\r') || value.contains('\n'))
                    continue
                val line = "$name=$value; Domain=$normalizedDomain; Path=/; Secure; SameSite=None"
                cookieLines.add(Triple(origin, line, name))
                if (Settings.instance.plugins.shouldClearWebviewCookies())
                    hydratedCookies.add(Pair(normalizedDomain, name))
            }
        }

        if (cookieLines.isEmpty()) {
            startReadinessPolling()
            targetWebView.loadUrl(targetUrl)
            return
        }

        val remaining = AtomicInteger(cookieLines.size)
        cookieLines.forEach { (origin, line, _) ->
            cookieManager.setCookie(origin, line) {
                if (remaining.decrementAndGet() == 0) {
                    if (!Settings.instance.plugins.shouldClearWebviewCookies())
                        cookieManager.flush()
                    mainHandler.post {
                        if (!destroyed && webView === targetWebView) {
                            startReadinessPolling()
                            targetWebView.loadUrl(targetUrl)
                        }
                    }
                }
            }
        }
    }

    private fun startReadinessPolling() {
        readinessStartedAt = System.currentTimeMillis()
        scheduleReadinessPoll()
    }

    private fun scheduleReadinessPoll() {
        mainHandler.removeCallbacks(readinessPoll)
        mainHandler.post(readinessPoll)
    }

    private val readinessPoll = object : Runnable {
        override fun run() {
            if (destroyed || fallbackDelivered || pageReady) return
            val targetWebView = webView ?: return
            targetWebView.evaluateJavascript(
                "Boolean(window.__grayjayYouTubeCommentsSurface?.status?.().ready)"
            ) { result ->
                if (destroyed || fallbackDelivered || pageReady) return@evaluateJavascript
                if (result == "true") {
                    pageReady = true
                    loadingView.visibility = View.GONE
                } else if (System.currentTimeMillis() - readinessStartedAt >= READINESS_TIMEOUT_MS) {
                    failOnce("root-timeout")
                } else {
                    mainHandler.postDelayed(this, READINESS_POLL_MS)
                }
            }
        }
    }

    private fun deliverLoginRequest() {
        if (loginRequestDelivered) return
        loginRequestDelivered = true
        onLoginRequired?.invoke()
    }

    private fun routeExternalUrl(url: String) {
        val handler = onExternalUrl
        if (handler != null) {
            handler(url)
            return
        }
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            })
        }
    }

    private fun routeChannelUrl(url: String) {
        onChannelUrl?.invoke(url)
    }

    private fun failOnce(category: String) {
        if (fallbackDelivered || destroyed) return
        fallbackDelivered = true
        mainHandler.removeCallbacksAndMessages(null)
        Logger.w(TAG, "Official YouTube comments fallback: $category")
        onFallback?.invoke(category)
    }

    private fun clearHydratedSessionCookiesIfRequested() {
        if (!Settings.instance.plugins.shouldClearWebviewCookies() || hydratedCookies.isEmpty())
            return
        val cookieManager = CookieManager.getInstance()
        hydratedCookies.distinct().forEach { (domain, name) ->
            val origin = "https://" + domain.trimStart('.')
            cookieManager.setCookie(
                origin,
                "$name=; Domain=$domain; Path=/; Max-Age=0; Secure; SameSite=None",
                null
            )
        }
        hydratedCookies.clear()
    }

    private class NestedCommentsWebView(context: Context) : WebView(context) {
        private val locationBeforeScroll = IntArray(2)
        private val locationAfterScroll = IntArray(2)
        private val minimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
        private val maximumFlingVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val coordinatedFling = OverScroller(context)
        private var scrollHost: RecyclerView? = null
        private var velocityTracker: VelocityTracker? = null
        private var lastScreenTouchY = 0f
        private var downScreenTouchY = 0f
        private var hasDragged = false
        private var lastFlingY = 0

        private val flingRunner = object : Runnable {
            override fun run() {
                if (!coordinatedFling.computeScrollOffset()) return

                val currentY = coordinatedFling.currY
                val requestedScrollY = currentY - lastFlingY
                lastFlingY = currentY

                if (requestedScrollY != 0) {
                    val consumedScrollY = consumeCoordinatedScroll(requestedScrollY)
                    if (consumedScrollY != requestedScrollY) {
                        coordinatedFling.forceFinished(true)
                        return
                    }
                }

                if (!coordinatedFling.isFinished)
                    postOnAnimation(this)
            }
        }

        init {
            isVerticalScrollBarEnabled = false
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(ContextCompat.getColor(context, R.color.black))
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val webEvent = MotionEvent.obtain(event)
            var flingVelocityY: Int? = null
            var cancelWebGesture = false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopCoordinatedFling()
                    scrollHost = findRecyclerViewParent()?.also { it.stopScroll() }
                    velocityTracker?.recycle()
                    velocityTracker = VelocityTracker.obtain().also { it.addScreenSpaceMovement(event) }
                    lastScreenTouchY = event.rawY
                    downScreenTouchY = event.rawY
                    hasDragged = false
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    velocityTracker?.addScreenSpaceMovement(event)
                    if (!hasDragged && kotlin.math.abs(event.rawY - downScreenTouchY) >= touchSlop)
                        hasDragged = true
                    val requestedScrollY = (lastScreenTouchY - event.rawY).toInt()
                    val host = scrollHost
                    var consumedByHost = 0
                    if (host != null && shouldHostConsume(requestedScrollY, host)) {
                        getLocationOnScreen(locationBeforeScroll)
                        host.scrollBy(0, requestedScrollY)
                        getLocationOnScreen(locationAfterScroll)
                        consumedByHost = locationBeforeScroll[1] - locationAfterScroll[1]
                    }
                    lastScreenTouchY = event.rawY
                    webEvent.offsetLocation(0f, consumedByHost.toFloat())
                }
                MotionEvent.ACTION_UP -> {
                    velocityTracker?.apply {
                        addScreenSpaceMovement(event)
                        computeCurrentVelocity(1000, maximumFlingVelocity.toFloat())
                        val scrollVelocityY = -yVelocity.toInt()
                        if (hasDragged && kotlin.math.abs(scrollVelocityY) >= minimumFlingVelocity)
                            flingVelocityY = scrollVelocityY
                        recycle()
                    }
                    velocityTracker = null
                    cancelWebGesture = hasDragged
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
                MotionEvent.ACTION_CANCEL -> {
                    velocityTracker?.recycle()
                    velocityTracker = null
                    hasDragged = false
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }

            return try {
                if (cancelWebGesture)
                    webEvent.action = MotionEvent.ACTION_CANCEL
                val handled = super.onTouchEvent(webEvent)
                flingVelocityY?.let(::startCoordinatedFling)
                handled || flingVelocityY != null
            } finally {
                webEvent.recycle()
            }
        }

        override fun onDetachedFromWindow() {
            stopCoordinatedFling()
            velocityTracker?.recycle()
            velocityTracker = null
            super.onDetachedFromWindow()
        }

        private fun startCoordinatedFling(velocityY: Int) {
            stopCoordinatedFling()
            lastFlingY = 0
            coordinatedFling.fling(
                0,
                0,
                0,
                velocityY,
                0,
                0,
                -COORDINATED_FLING_LIMIT,
                COORDINATED_FLING_LIMIT
            )
            postOnAnimation(flingRunner)
        }

        private fun stopCoordinatedFling() {
            removeCallbacks(flingRunner)
            if (!coordinatedFling.isFinished)
                coordinatedFling.forceFinished(true)
        }

        private fun consumeCoordinatedScroll(scrollDeltaY: Int): Int {
            return TwoSurfaceScrollCoordinator.consume(
                scrollDeltaY,
                consumeOuter = ::scrollHostBy,
                consumeInner = ::scrollWebViewBy
            )
        }

        private fun scrollHostBy(scrollDeltaY: Int): Int {
            val host = scrollHost ?: return 0
            if (!host.canScrollVertically(if (scrollDeltaY > 0) 1 else -1)) return 0

            getLocationOnScreen(locationBeforeScroll)
            host.scrollBy(0, scrollDeltaY)
            getLocationOnScreen(locationAfterScroll)
            return locationBeforeScroll[1] - locationAfterScroll[1]
        }

        private fun scrollWebViewBy(scrollDeltaY: Int): Int {
            if (!canScrollVertically(if (scrollDeltaY > 0) 1 else -1)) return 0

            val scrollYBefore = scrollY
            super.scrollBy(0, scrollDeltaY)
            return scrollY - scrollYBefore
        }

        private fun shouldHostConsume(scrollDeltaY: Int, host: RecyclerView): Boolean {
            return when {
                scrollDeltaY > 0 -> host.canScrollVertically(1)
                scrollDeltaY < 0 -> !canScrollVertically(-1) && host.canScrollVertically(-1)
                else -> false
            }
        }

        private fun findRecyclerViewParent(): RecyclerView? {
            var ancestor = parent
            while (ancestor != null) {
                if (ancestor is RecyclerView) return ancestor
                ancestor = ancestor.parent
            }
            return null
        }

        private fun VelocityTracker.addScreenSpaceMovement(event: MotionEvent) {
            val screenEvent = MotionEvent.obtain(event)
            try {
                screenEvent.offsetLocation(event.rawX - event.x, event.rawY - event.y)
                addMovement(screenEvent)
            } finally {
                screenEvent.recycle()
            }
        }

        companion object {
            private const val COORDINATED_FLING_LIMIT = 1_000_000
        }
    }

    companion object {
        private const val TAG = "YouTubeCommentsWeb"
        private const val READINESS_TIMEOUT_MS = 15_000L
        private const val READINESS_POLL_MS = 250L
        private val COOKIE_NAME = Regex("^[A-Za-z0-9!#$%&'*+.^_`|~-]+$")

        private fun Int.toCssColor(): String = String.format("#%06X", this and 0xFFFFFF)

        fun isEligible(videoUrl: String?, pluginId: String?): Boolean {
            return SettingsDev.instance.experimentalSettings.youtubeWebComments &&
                !StateApp.instance.privateMode &&
                pluginId == YouTubeCommentsWebPolicy.YOUTUBE_SOURCE_ID &&
                YouTubeCommentsWebPolicy.canonicalDesktopWatchUrl(videoUrl) != null &&
                WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        }
    }
}
