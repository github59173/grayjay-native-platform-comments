import AppKit
import Foundation
import WebKit

private let watchURL = URL(string: "https://www.youtube.com/watch?v=dQw4w9WgXcQ&app=desktop")!
private let desktopUserAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

final class ExactCommentsLabController: NSObject, NSApplicationDelegate, WKNavigationDelegate, WKUIDelegate, NSWindowDelegate {
    private var window: NSWindow!
    private var webView: WKWebView!
    private var statusLabel: NSTextField!
    private var widthLabel: NSTextField!
    private var widthSlider: NSSlider!
    private var statusTimer: Timer?
    private var lastReportedStatus = ""
    private let isolationSource: String

    init(isolationSource: String) {
        self.isolationSource = isolationSource
        super.init()
    }

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.regular)
        installApplicationMenu()
        createWindow()
        configureMediaBlockingAndLoad()
        NSApp.activate(ignoringOtherApps: true)
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        true
    }

    func applicationWillTerminate(_ notification: Notification) {
        statusTimer?.invalidate()
        webView?.stopLoading()
    }

    private func installApplicationMenu() {
        let mainMenu = NSMenu()
        let appItem = NSMenuItem()
        mainMenu.addItem(appItem)

        let appMenu = NSMenu()
        appMenu.addItem(withTitle: "About Exact YouTube Comments Lab", action: #selector(NSApplication.orderFrontStandardAboutPanel(_:)), keyEquivalent: "")
        appMenu.addItem(.separator())
        appMenu.addItem(withTitle: "Quit Exact YouTube Comments Lab", action: #selector(NSApplication.terminate(_:)), keyEquivalent: "q")
        appItem.submenu = appMenu

        let viewItem = NSMenuItem()
        mainMenu.addItem(viewItem)
        let viewMenu = NSMenu(title: "View")
        viewMenu.addItem(withTitle: "Reload YouTube", action: #selector(reloadPage), keyEquivalent: "r").target = self
        viewItem.submenu = viewMenu
        NSApp.mainMenu = mainMenu
    }

    private func createWindow() {
        let configuration = WKWebViewConfiguration()
        configuration.websiteDataStore = .default()
        configuration.defaultWebpagePreferences.allowsContentJavaScript = true
        configuration.preferences.javaScriptCanOpenWindowsAutomatically = false
        configuration.preferences.setValue(true, forKey: "developerExtrasEnabled")
        configuration.mediaTypesRequiringUserActionForPlayback = [.audio, .video]
        configuration.userContentController.addUserScript(
            WKUserScript(source: isolationSource, injectionTime: .atDocumentStart, forMainFrameOnly: true)
        )

        webView = WKWebView(frame: .zero, configuration: configuration)
        webView.translatesAutoresizingMaskIntoConstraints = false
        webView.customUserAgent = desktopUserAgent
        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.allowsMagnification = true
        if #available(macOS 13.3, *) {
            webView.isInspectable = true
        }

        let controls = makeControls()
        let splitView = NSSplitView()
        splitView.translatesAutoresizingMaskIntoConstraints = false
        splitView.isVertical = true
        splitView.dividerStyle = .thin
        splitView.addArrangedSubview(webView)
        splitView.addArrangedSubview(controls)
        splitView.setHoldingPriority(.defaultLow, forSubviewAt: 0)
        splitView.setHoldingPriority(.defaultHigh, forSubviewAt: 1)

        let contentView = NSView()
        contentView.addSubview(splitView)
        NSLayoutConstraint.activate([
            splitView.leadingAnchor.constraint(equalTo: contentView.leadingAnchor),
            splitView.trailingAnchor.constraint(equalTo: contentView.trailingAnchor),
            splitView.topAnchor.constraint(equalTo: contentView.topAnchor),
            splitView.bottomAnchor.constraint(equalTo: contentView.bottomAnchor),
            controls.widthAnchor.constraint(equalToConstant: 330)
        ])

        window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 1320, height: 900),
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false
        )
        window.title = "Grayjay – Exact YouTube Desktop Comments Lab"
        window.minSize = NSSize(width: 760, height: 600)
        window.contentView = contentView
        window.delegate = self
        window.center()
        window.makeKeyAndOrderFront(nil)
    }

    private func makeControls() -> NSView {
        let documentView = NSView()
        documentView.translatesAutoresizingMaskIntoConstraints = false

        let stack = NSStackView()
        stack.translatesAutoresizingMaskIntoConstraints = false
        stack.orientation = .vertical
        stack.alignment = .leading
        stack.spacing = 10
        stack.edgeInsets = NSEdgeInsets(top: 20, left: 18, bottom: 24, right: 18)
        documentView.addSubview(stack)

        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: documentView.leadingAnchor),
            stack.trailingAnchor.constraint(equalTo: documentView.trailingAnchor),
            stack.topAnchor.constraint(equalTo: documentView.topAnchor),
            stack.bottomAnchor.constraint(lessThanOrEqualTo: documentView.bottomAnchor),
            documentView.widthAnchor.constraint(equalToConstant: 312)
        ])

        stack.addArrangedSubview(makeLabel("EXACT OFFICIAL SURFACE", size: 11, weight: .bold, color: .secondaryLabelColor))
        stack.addArrangedSubview(makeLabel("YouTube desktop comments", size: 22, weight: .bold))
        stack.addArrangedSubview(makeLabel("The WebView loads YouTube's complete watch document. Only surrounding page branches are hidden; the comments custom elements and YouTube runtime remain attached.", size: 12, weight: .regular, color: .secondaryLabelColor))

        statusLabel = makeLabel("Loading the official desktop watch page…", size: 12, weight: .medium)
        statusLabel.textColor = .systemBlue
        stack.addArrangedSubview(statusLabel)

        let buttonRow = NSStackView()
        buttonRow.orientation = .horizontal
        buttonRow.spacing = 8
        buttonRow.addArrangedSubview(makeButton("Reload", action: #selector(reloadPage)))
        buttonRow.addArrangedSubview(makeButton("Reset", action: #selector(resetLab)))
        stack.addArrangedSubview(buttonRow)

        let isolationToggle = NSButton(checkboxWithTitle: "Isolate comments root", target: self, action: #selector(toggleIsolation(_:)))
        isolationToggle.state = .on
        stack.addArrangedSubview(isolationToggle)

        stack.addArrangedSubview(makeSeparator())
        stack.addArrangedSubview(makeLabel("Comments width", size: 13, weight: .semibold))

        widthLabel = makeLabel("880 px", size: 12, weight: .regular, color: .secondaryLabelColor)
        stack.addArrangedSubview(widthLabel)

        widthSlider = NSSlider(value: 880, minValue: 360, maxValue: 1200, target: self, action: #selector(changeWidth(_:)))
        widthSlider.isContinuous = true
        widthSlider.widthAnchor.constraint(equalToConstant: 276).isActive = true
        stack.addArrangedSubview(widthSlider)

        stack.addArrangedSubview(makeSeparator())
        stack.addArrangedSubview(makeLabel("Hide official elements", size: 13, weight: .semibold))

        let toggles: [(String, String)] = [
            ("Comment count", "hide-count"),
            ("Sort / filter controls", "hide-sort"),
            ("Comment composer", "hide-composer"),
            ("Avatars", "hide-avatars"),
            ("Pinned label", "hide-pinned"),
            ("Author badges", "hide-badges"),
            ("Timestamps", "hide-timestamps"),
            ("Likes and counts", "hide-likes"),
            ("Dislikes", "hide-dislikes"),
            ("Reply action", "hide-reply-action"),
            ("Reply expanders", "hide-reply-expanders"),
            ("Creator hearts", "hide-hearts"),
            ("Action menus", "hide-menus"),
            ("Reply connector lines", "hide-connectors"),
            ("Compact spacing", "compact")
        ]

        for (title, key) in toggles {
            let checkbox = NSButton(checkboxWithTitle: title, target: self, action: #selector(toggleOfficialElement(_:)))
            checkbox.identifier = NSUserInterfaceItemIdentifier(key)
            stack.addArrangedSubview(checkbox)
        }

        stack.addArrangedSubview(makeSeparator())
        stack.addArrangedSubview(makeLabel("Right-click any YouTube element and choose Inspect Element. Posting and reactions are YouTube's own controls; availability depends on the WebView's YouTube sign-in state.", size: 11, weight: .regular, color: .secondaryLabelColor))

        let scrollView = NSScrollView()
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.hasVerticalScroller = true
        scrollView.drawsBackground = true
        scrollView.backgroundColor = .windowBackgroundColor
        scrollView.documentView = documentView
        return scrollView
    }

    private func makeLabel(_ text: String, size: CGFloat, weight: NSFont.Weight, color: NSColor = .labelColor) -> NSTextField {
        let label = NSTextField(wrappingLabelWithString: text)
        label.font = NSFont.systemFont(ofSize: size, weight: weight)
        label.textColor = color
        label.maximumNumberOfLines = 0
        label.preferredMaxLayoutWidth = 276
        return label
    }

    private func makeButton(_ title: String, action: Selector) -> NSButton {
        let button = NSButton(title: title, target: self, action: action)
        button.bezelStyle = .rounded
        return button
    }

    private func makeSeparator() -> NSBox {
        let separator = NSBox()
        separator.boxType = .separator
        separator.widthAnchor.constraint(equalToConstant: 276).isActive = true
        return separator
    }

    private func configureMediaBlockingAndLoad() {
        let rules = """
        [{"trigger":{"url-filter":"https?://[^/]*\\\\.googlevideo\\\\.com/.*","resource-type":["media"]},"action":{"type":"block"}}]
        """
        WKContentRuleListStore.default().compileContentRuleList(forIdentifier: "GrayjayCommentsLabBlockMedia", encodedContentRuleList: rules) { [weak self] ruleList, _ in
            DispatchQueue.main.async {
                if let ruleList {
                    self?.webView.configuration.userContentController.add(ruleList)
                }
                self?.loadWatchPage()
            }
        }
    }

    private func loadWatchPage() {
        var request = URLRequest(url: watchURL)
        request.cachePolicy = .reloadIgnoringLocalCacheData
        webView.load(request)
        startStatusTimer()
    }

    private func startStatusTimer() {
        statusTimer?.invalidate()
        statusTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] _ in
            self?.pollStatus()
        }
    }

    private func pollStatus() {
        let script = "JSON.stringify(window.__grayjayExactCommentsLab?.status?.() ?? {rootFound:false,threads:0,replies:0})"
        webView.evaluateJavaScript(script) { [weak self] value, _ in
            guard let json = value as? String,
                  let data = json.data(using: .utf8),
                  let status = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return }

            let found = status["rootFound"] as? Bool ?? false
            let threads = status["threads"] as? Int ?? 0
            let replies = status["replies"] as? Int ?? 0
            let players = status["players"] as? Int ?? 0
            DispatchQueue.main.async {
                let statusLine: String
                if found {
                    statusLine = "Official root ready · \(threads) threads · \(replies) replies · \(players) players"
                    self?.statusLabel.stringValue = statusLine
                    self?.statusLabel.textColor = .systemGreen
                } else {
                    statusLine = "Waiting for YouTube's ytd-comments#comments root…"
                    self?.statusLabel.stringValue = statusLine
                    self?.statusLabel.textColor = .systemBlue
                }

                if self?.lastReportedStatus != statusLine {
                    self?.lastReportedStatus = statusLine
                    print(statusLine)
                    fflush(stdout)
                }
            }
        }
    }

    private func callLab(_ expression: String) {
        webView.evaluateJavaScript("window.__grayjayExactCommentsLab?.\(expression)", completionHandler: nil)
    }

    @objc private func reloadPage() {
        statusLabel.stringValue = "Reloading the official desktop watch page…"
        statusLabel.textColor = .systemBlue
        loadWatchPage()
    }

    @objc private func resetLab() {
        widthSlider.doubleValue = 880
        widthLabel.stringValue = "880 px"
        callLab("reset()")
    }

    @objc private func toggleIsolation(_ sender: NSButton) {
        callLab("setIsolated(\(sender.state == .on ? "true" : "false"))")
    }

    @objc private func changeWidth(_ sender: NSSlider) {
        let width = Int(sender.doubleValue.rounded())
        widthLabel.stringValue = "\(width) px"
        callLab("setWidth(\(width))")
    }

    @objc private func toggleOfficialElement(_ sender: NSButton) {
        guard let key = sender.identifier?.rawValue else { return }
        callLab("setHidden('\(key)', \(sender.state == .on ? "true" : "false"))")
    }

    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        statusLabel.stringValue = "Loading YouTube's desktop runtime…"
        statusLabel.textColor = .systemBlue
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        callLab("isolate()")
        pollStatus()
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        statusLabel.stringValue = "YouTube navigation failed: \(error.localizedDescription)"
        statusLabel.textColor = .systemRed
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        statusLabel.stringValue = "YouTube failed to load: \(error.localizedDescription)"
        statusLabel.textColor = .systemRed
    }

    func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
        guard let url = navigationAction.request.url else {
            decisionHandler(.cancel)
            return
        }

        if navigationAction.navigationType == .linkActivated,
           let host = url.host?.lowercased(),
           !host.hasSuffix("youtube.com"),
           !host.hasSuffix("google.com") {
            NSWorkspace.shared.open(url)
            decisionHandler(.cancel)
            return
        }

        decisionHandler(.allow)
    }

    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration, for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if let url = navigationAction.request.url,
           let host = url.host?.lowercased(),
           host.hasSuffix("youtube.com") || host.hasSuffix("google.com") {
            webView.load(URLRequest(url: url))
        }
        return nil
    }
}

guard CommandLine.arguments.count > 1 else {
    fputs("Usage: ExactCommentsLab /absolute/path/to/isolate-comments.js\n", stderr)
    exit(2)
}

let scriptPath = CommandLine.arguments[1]
guard let isolationSource = try? String(contentsOfFile: scriptPath, encoding: .utf8) else {
    fputs("Unable to read isolation script: \(scriptPath)\n", stderr)
    exit(2)
}

let application = NSApplication.shared
let controller = ExactCommentsLabController(isolationSource: isolationSource)
application.delegate = controller
application.run()
