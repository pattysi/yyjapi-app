package com.yyjapi.app;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebChromeClient;
import com.getcapacitor.BridgeWebViewClient;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "YYJAPI";
    private static final String HOME_URL = "https://www.yyjapi.com";
    private static final String GITHUB_LATEST_RELEASE_API = "https://api.github.com/repos/%s/releases/latest";
    private static final String UPDATE_APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final int[] RETRY_DELAYS_MS = { 1000, 2000, 4000, 8000, 15000, 30000 };
    private static final Set<String> APP_HOSTS = new HashSet<>(
        Arrays.asList("yyjapi.com", "www.yyjapi.com", "cf.yyjapi.com")
    );
    private static final Set<String> OAUTH_HOSTS = new HashSet<>(
        Arrays.asList("github.com", "www.github.com", "linux.do", "www.linux.do")
    );
    private static final String WEB_BACK_HANDLER_SCRIPT =
        "(function() {" +
        "  try {" +
        "    var detail = { source: 'android-back', handled: false };" +
        "    try {" +
        "      var event;" +
        "      if (typeof CustomEvent === 'function') {" +
        "        event = new CustomEvent('yyjapiNativeBack', { cancelable: true, detail: detail });" +
        "      } else if (document.createEvent) {" +
        "        event = document.createEvent('CustomEvent');" +
        "        event.initCustomEvent('yyjapiNativeBack', true, true, detail);" +
        "      }" +
        "      if (event) {" +
        "        document.dispatchEvent(event);" +
        "        if (event.defaultPrevented || detail.handled === true) { return true; }" +
        "      }" +
        "    } catch (eventError) {" +
        "      if (detail.handled === true) { return true; }" +
        "    }" +
        "    function visible(el) {" +
        "      if (!el) { return false; }" +
        "      var style = window.getComputedStyle(el);" +
        "      var rect = el.getBoundingClientRect();" +
        "      return style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0' && rect.width > 0 && rect.height > 0;" +
        "    }" +
        "    function visibleElements(selector, root) {" +
        "      return Array.prototype.slice.call((root || document).querySelectorAll(selector)).filter(visible);" +
        "    }" +
        "    function activate(el) {" +
        "      try {" +
        "        if (el && typeof el.click === 'function') { el.click(); return true; }" +
        "        if (el && typeof MouseEvent === 'function') {" +
        "          el.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));" +
        "          return true;" +
        "        }" +
        "      } catch (activationError) {}" +
        "      return false;" +
        "    }" +
        "    function clickFirst(selectors, root) {" +
        "      for (var i = 0; i < selectors.length; i++) {" +
        "        var elements = visibleElements(selectors[i], root);" +
        "        for (var j = elements.length - 1; j >= 0; j--) {" +
        "          var el = elements[j];" +
        "          if (activate(el)) { return true; }" +
        "        }" +
        "      }" +
        "      return false;" +
        "    }" +
        "    function textCloseButton(root) {" +
        "      var buttons = visibleElements('button, [role=\"button\"]', root);" +
        "      for (var i = buttons.length - 1; i >= 0; i--) {" +
        "        var label = ((buttons[i].getAttribute('aria-label') || '') + ' ' + (buttons[i].getAttribute('title') || '') + ' ' + (buttons[i].innerText || buttons[i].textContent || '')).trim().toLowerCase();" +
        "        if (label === 'close' || label === '\\u5173\\u95ed' || label === '\\u53d6\\u6d88' || label === '\\u00d7' || label === 'x') { if (activate(buttons[i])) { return true; } }" +
        "      }" +
        "      return false;" +
        "    }" +
        "    var openDialog = document.querySelector('dialog[open]');" +
        "    if (openDialog && visible(openDialog)) { openDialog.close(); return true; }" +
        "    var popupRoots = visibleElements('[role=\"dialog\"], [aria-modal=\"true\"], [data-slot=\"sheet-content\"], [data-slot=\"dialog-content\"], .modal.show, .ant-modal-root, .ant-drawer, .el-overlay, .van-popup--show, .adm-popup, .nut-popup, .uni-popup');" +
        "    var closeSelectors = [" +
        "      '[data-back-close]'," +
        "      '[data-native-back-close]'," +
        "      '[data-slot=\"sheet-close\"]'," +
        "      '[data-slot=\"dialog-close\"]'," +
        "      '[data-dismiss=\"modal\"]'," +
        "      '[data-bs-dismiss=\"modal\"]'," +
        "      '[aria-label=\"Close\"]'," +
        "      '[aria-label=\"close\"]'," +
        "      '[aria-label=\"\\u5173\\u95ed\"]'," +
        "      '[title=\"Close\"]'," +
        "      '[title=\"\\u5173\\u95ed\"]'," +
        "      '.btn-close'," +
        "      '.close'," +
        "      '.modal.show .btn-close'," +
        "      '.modal.show .close'," +
        "      '.modal.show [aria-label=\"Close\"]'," +
        "      '.van-popup__close-icon'," +
        "      '.van-dialog__cancel'," +
        "      '.ant-modal-close'," +
        "      '.ant-drawer-close'," +
        "      '.el-dialog__headerbtn'," +
        "      '.el-drawer__close-btn'," +
        "      '.nut-icon-close'," +
        "      '.adm-popup-close-icon'," +
        "      '.uni-popup__close'" +
        "    ];" +
        "    for (var rootIndex = popupRoots.length - 1; rootIndex >= 0; rootIndex--) {" +
        "      if (clickFirst(closeSelectors, popupRoots[rootIndex]) || textCloseButton(popupRoots[rootIndex])) { return true; }" +
        "    }" +
        "    if (clickFirst([" +
        "      '[data-back-close]'," +
        "      '[data-native-back-close]'," +
        "      '[data-slot=\"sheet-close\"]'," +
        "      '[data-slot=\"dialog-close\"]'," +
        "      '[data-dismiss=\"modal\"]'," +
        "      '[data-bs-dismiss=\"modal\"]'," +
        "      '.modal.show .btn-close'," +
        "      '.modal.show .close'," +
        "      '.modal.show [aria-label=\"Close\"]'," +
        "      '[role=\"dialog\"] [aria-label=\"Close\"]'," +
        "      '[aria-modal=\"true\"] [aria-label=\"Close\"]'," +
        "      '.van-popup--show .van-popup__close-icon'," +
        "      '.van-dialog .van-dialog__cancel'," +
        "      '.ant-modal-root .ant-modal-close'," +
        "      '.ant-drawer .ant-drawer-close'," +
        "      '.el-overlay .el-dialog__headerbtn'," +
        "      '.el-drawer__close-btn'," +
        "      '.nut-popup .nut-icon-close'," +
        "      '.adm-popup .adm-popup-close-icon'," +
        "      '.uni-popup .uni-popup__close'" +
        "    ])) { return true; }" +
        "    var escapeEvent = new KeyboardEvent('keydown', {" +
        "      key: 'Escape', code: 'Escape', keyCode: 27, which: 27, bubbles: true, cancelable: true" +
        "    });" +
        "    document.dispatchEvent(escapeEvent);" +
        "    if (escapeEvent.defaultPrevented) { return true; }" +
        "    if (popupRoots.length > 0) { return true; }" +
        "  } catch (e) {}" +
        "  return false;" +
        "})();";

    private final Handler retryHandler = new Handler(Looper.getMainLooper());
    private int retryAttempt = 0;
    private boolean mainFrameLoadFailed = false;
    private Runnable pendingRetryRunnable;
    private long pendingUpdateDownloadId = -1L;
    private File pendingUpdateApkFile;
    private BroadcastReceiver updateDownloadReceiver;

    private static class UpdateInfo {
        final String version;
        final String releaseName;
        final String releaseNotes;
        final String apkName;
        final String apkUrl;

        UpdateInfo(String version, String releaseName, String releaseNotes, String apkName, String apkUrl) {
            this.version = version;
            this.releaseName = releaseName;
            this.releaseNotes = releaseNotes;
            this.apkName = apkName;
            this.apkUrl = apkUrl;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBarsAndInsets();
        configureWebView();
        configureBackButtonHandling();
        handleIncomingIntent(getIntent());
        checkForAppUpdate();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
    }

    private void configureBackButtonHandling() {
        getOnBackPressedDispatcher()
            .addCallback(
                this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        handleBackPressed();
                    }
                }
            );
    }

    private void configureSystemBarsAndInsets() {
        Window window = getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.WHITE);
        window.setNavigationBarColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            window.getDecorView().setSystemUiVisibility(flags);
        }

        if (bridge == null || bridge.getWebView() == null) {
            return;
        }

        WebView webView = bridge.getWebView();
        View rootView = findViewById(R.id.capacitor_root);
        if (rootView == null && webView.getParent() instanceof View) {
            rootView = (View) webView.getParent();
        }
        if (rootView == null) {
            rootView = findViewById(android.R.id.content);
        }

        final View safeAreaRoot = rootView;
        safeAreaRoot.setFitsSystemWindows(false);
        if (safeAreaRoot instanceof ViewGroup) {
            ((ViewGroup) safeAreaRoot).setClipToPadding(true);
        }

        ViewCompat.setOnApplyWindowInsetsListener(safeAreaRoot, (view, insets) -> {
            Insets safeInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars()
                    | WindowInsetsCompat.Type.navigationBars()
                    | WindowInsetsCompat.Type.displayCutout()
            );
            Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
            boolean keyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
            int bottomInset = keyboardVisible ? imeInsets.bottom : safeInsets.bottom;

            view.setPadding(safeInsets.left, safeInsets.top, safeInsets.right, bottomInset);

            return insets;
        });
        safeAreaRoot.post(() -> ViewCompat.requestApplyInsets(safeAreaRoot));
    }

    private void configureWebView() {
        if (bridge == null || bridge.getWebView() == null) {
            return;
        }

        WebView webView = bridge.getWebView();
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        WebSettings settings = webView.getSettings();
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            settings.setOffscreenPreRaster(true);
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
            WebSettingsCompat.setWebAuthenticationSupport(
                settings,
                WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP
            );
            Log.i(TAG, "WebAuthenticationSupport=" + WebSettingsCompat.getWebAuthenticationSupport(settings));
        } else {
            Log.w(TAG, "WebAuthentication is not supported by this Android System WebView.");
        }

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        bridge.setWebViewClient(new AppWebViewClient(bridge));
        webView.setWebChromeClient(new AppWebChromeClient(bridge));
    }

    @Override
    public void onBackPressed() {
        handleBackPressed();
    }

    private void handleBackPressed() {
        if (bridge == null || bridge.getWebView() == null) {
            finish();
            return;
        }

        WebView webView = bridge.getWebView();
        webView.evaluateJavascript(
            WEB_BACK_HANDLER_SCRIPT,
            handled -> {
                if ("true".equals(handled)) {
                    return;
                }

                if (webView.canGoBack()) {
                    webView.goBack();
                    return;
                }

                finish();
            }
        );
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }

        Uri uri = intent.getData();
        if (!shouldLoadInWebView(uri, getCurrentWebViewUrl())) {
            return;
        }

        if (bridge == null || bridge.getWebView() == null) {
            return;
        }

        bridge.getWebView().post(() -> bridge.getWebView().loadUrl(uri.toString()));
    }

    @Override
    public void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        maybeInstallPendingUpdate();
    }

    @Override
    public void onDestroy() {
        cancelScheduledRetry();
        unregisterUpdateDownloadReceiver();
        super.onDestroy();
    }

    private class AppWebViewClient extends BridgeWebViewClient {

        AppWebViewClient(Bridge bridge) {
            super(bridge);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (request == null || !request.isForMainFrame()) {
                return false;
            }

            return handleNavigation(request.getUrl());
        }

        @Deprecated
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return url != null && handleNavigation(Uri.parse(url));
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            cancelScheduledRetry();
            mainFrameLoadFailed = false;
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            if (!mainFrameLoadFailed) {
                clearRetryState();
            }
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            if (request != null && request.isForMainFrame()) {
                mainFrameLoadFailed = true;
                String description = error == null ? "web resource error" : String.valueOf(error.getDescription());
                scheduleWebViewRetry(request.getUrl(), description);
            }

            super.onReceivedError(view, request, error);
        }

        @Deprecated
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            mainFrameLoadFailed = true;
            scheduleWebViewRetry(failingUrl == null ? null : Uri.parse(failingUrl), description);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            if (request != null && request.isForMainFrame() && isRetriableHttpStatus(errorResponse)) {
                mainFrameLoadFailed = true;
                scheduleWebViewRetry(request.getUrl(), "HTTP " + errorResponse.getStatusCode());
            }

            super.onReceivedHttpError(view, request, errorResponse);
        }
    }

    private boolean isRetriableHttpStatus(WebResourceResponse errorResponse) {
        if (errorResponse == null) {
            return false;
        }

        int statusCode = errorResponse.getStatusCode();
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private void scheduleWebViewRetry(Uri uri, String reason) {
        Uri retryUri = uri == null ? Uri.parse(HOME_URL) : uri;
        if (!isRetriableInAppUrl(retryUri)) {
            return;
        }

        int retryDelay = RETRY_DELAYS_MS[Math.min(retryAttempt, RETRY_DELAYS_MS.length - 1)];
        retryAttempt++;
        cancelScheduledRetry();

        String retryUrl = retryUri.toString();
        Runnable retryRunnable = new Runnable() {
            @Override
            public void run() {
                if (pendingRetryRunnable != this) {
                    return;
                }

                pendingRetryRunnable = null;
                if (bridge == null || bridge.getWebView() == null) {
                    return;
                }

                Log.i(TAG, "Retrying WebView load: " + retryUrl);
                bridge.getWebView().loadUrl(retryUrl);
            }
        };

        pendingRetryRunnable = retryRunnable;
        Log.w(TAG, "WebView load failed (" + reason + "), retrying in " + retryDelay + "ms: " + retryUrl);
        retryHandler.postDelayed(retryRunnable, retryDelay);
    }

    private boolean isRetriableInAppUrl(Uri uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }

        String scheme = uri.getScheme().toLowerCase(Locale.US);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return false;
        }

        String host = uri.getHost();
        return isAllowedHost(host) || isOAuthHost(host);
    }

    private void clearRetryState() {
        retryAttempt = 0;
        mainFrameLoadFailed = false;
        cancelScheduledRetry();
    }

    private void cancelScheduledRetry() {
        if (pendingRetryRunnable == null) {
            return;
        }

        retryHandler.removeCallbacks(pendingRetryRunnable);
        pendingRetryRunnable = null;
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }

        if (shouldLoadInWebView(uri, getCurrentWebViewUrl())) {
            return false;
        }

        if (isHttpOrHttps(uri)) {
            openInAppBrowserTab(uri);
            return true;
        }

        openExternal(uri);
        return true;
    }

    private void checkForAppUpdate() {
        String releaseRepo = normalizeGithubRepo(BuildConfig.GITHUB_RELEASE_REPO);
        if (releaseRepo.isEmpty()) {
            Log.i(TAG, "GitHub release repo is not configured; skipping update check.");
            return;
        }

        new Thread(() -> {
            try {
                UpdateInfo updateInfo = fetchLatestRelease(releaseRepo);
                if (updateInfo == null || updateInfo.apkUrl == null || updateInfo.apkUrl.isEmpty()) {
                    return;
                }

                if (compareVersions(updateInfo.version, getCurrentVersionName()) <= 0) {
                    return;
                }

                runOnUiThread(() -> showUpdatePrompt(updateInfo));
            } catch (Exception e) {
                Log.w(TAG, "Update check failed", e);
            }
        }, "YYJAPI update check").start();
    }

    private UpdateInfo fetchLatestRelease(String releaseRepo) throws Exception {
        URL url = new URL(String.format(Locale.US, GITHUB_LATEST_RELEASE_API, releaseRepo));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Accept", "application/vnd.github+json");
        connection.setRequestProperty("User-Agent", "YYJAPI-Android/" + getCurrentVersionName());

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                Log.w(TAG, "GitHub release request failed: HTTP " + responseCode);
                return null;
            }

            JSONObject releaseJson = new JSONObject(readResponseBody(connection.getInputStream()));
            String tagName = releaseJson.optString("tag_name", "");
            String releaseName = releaseJson.optString("name", tagName);
            String releaseNotes = releaseJson.optString("body", "");
            JSONArray assets = releaseJson.optJSONArray("assets");
            String apkUrl = null;
            String apkName = null;

            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    JSONObject asset = assets.optJSONObject(i);
                    if (asset == null) {
                        continue;
                    }

                    String name = asset.optString("name", "");
                    if (!name.toLowerCase(Locale.US).endsWith(".apk")) {
                        continue;
                    }

                    apkName = name;
                    apkUrl = asset.optString("browser_download_url", "");
                    break;
                }
            }

            String version = normalizeVersion(tagName.isEmpty() ? releaseName : tagName);
            if (version.isEmpty()) {
                return null;
            }

            return new UpdateInfo(version, releaseName, releaseNotes, apkName, apkUrl);
        } finally {
            connection.disconnect();
        }
    }

    private void showUpdatePrompt(UpdateInfo updateInfo) {
        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("当前版本：").append(getCurrentVersionName()).append("\n");
        message.append("最新版本：").append(updateInfo.version);
        if (updateInfo.releaseNotes != null && !updateInfo.releaseNotes.trim().isEmpty()) {
            message.append("\n\n").append(updateInfo.releaseNotes.trim());
        }

        new AlertDialog.Builder(this)
            .setTitle("发现新版本")
            .setMessage(message.toString())
            .setPositiveButton("下载更新", (dialog, which) -> downloadUpdate(updateInfo))
            .setNegativeButton("稍后", null)
            .show();
    }

    private void downloadUpdate(UpdateInfo updateInfo) {
        if (updateInfo == null || updateInfo.apkUrl == null || updateInfo.apkUrl.isEmpty()) {
            Toast.makeText(this, "没有找到可下载的 APK", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File outputDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (outputDir == null) {
                outputDir = getCacheDir();
            }
            if (!outputDir.exists() && !outputDir.mkdirs()) {
                Toast.makeText(this, "无法创建下载目录", Toast.LENGTH_SHORT).show();
                return;
            }

            String fileName = updateInfo.apkName == null || updateInfo.apkName.trim().isEmpty()
                ? "yyjapi-" + sanitizeFilePart(updateInfo.version) + ".apk"
                : sanitizeApkFileName(updateInfo.apkName);
            File outputFile = new File(outputDir, fileName);
            if (outputFile.exists() && !outputFile.delete()) {
                Log.w(TAG, "Could not delete existing update APK: " + outputFile);
            }

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateInfo.apkUrl));
            request.setTitle("优易接API 更新");
            request.setDescription("正在下载 " + updateInfo.version);
            request.setMimeType(UPDATE_APK_MIME_TYPE);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.addRequestHeader("User-Agent", "YYJAPI-Android/" + getCurrentVersionName());
            request.setDestinationUri(Uri.fromFile(outputFile));

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (downloadManager == null) {
                Toast.makeText(this, "系统下载服务不可用", Toast.LENGTH_SHORT).show();
                return;
            }

            registerUpdateDownloadReceiver();
            pendingUpdateApkFile = outputFile;
            pendingUpdateDownloadId = downloadManager.enqueue(request);
            Toast.makeText(this, "已开始下载更新", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w(TAG, "Could not start update download", e);
            Toast.makeText(this, "下载更新失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void registerUpdateDownloadReceiver() {
        if (updateDownloadReceiver != null) {
            return;
        }

        updateDownloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || !DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                    return;
                }

                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (downloadId != pendingUpdateDownloadId) {
                    return;
                }

                handleUpdateDownloadComplete(downloadId);
            }
        };

        IntentFilter filter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(updateDownloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(updateDownloadReceiver, filter);
        }
    }

    private void handleUpdateDownloadComplete(long downloadId) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            return;
        }

        DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
        try (Cursor cursor = downloadManager.query(query)) {
            if (cursor == null || !cursor.moveToFirst()) {
                Toast.makeText(this, "更新下载状态未知", Toast.LENGTH_SHORT).show();
                return;
            }

            int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int status = statusIndex >= 0 ? cursor.getInt(statusIndex) : DownloadManager.STATUS_FAILED;
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                Toast.makeText(this, "更新下载完成", Toast.LENGTH_SHORT).show();
                maybeInstallPendingUpdate();
            } else if (status == DownloadManager.STATUS_FAILED) {
                Toast.makeText(this, "更新下载失败", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not inspect update download", e);
        }
    }

    private void maybeInstallPendingUpdate() {
        if (pendingUpdateApkFile == null || !pendingUpdateApkFile.exists()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            promptForInstallPermission();
            return;
        }

        installUpdateApk(pendingUpdateApkFile);
    }

    private void promptForInstallPermission() {
        if (isFinishing() || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && isDestroyed())) {
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("需要安装权限")
            .setMessage("请允许优易接API安装应用更新，然后返回 App 继续安装。")
            .setPositiveButton("去开启", (dialog, which) -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent intent = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())
                    );
                    startActivity(intent);
                }
            })
            .setNegativeButton("稍后", null)
            .show();
    }

    private void installUpdateApk(File apkFile) {
        try {
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", apkFile);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, UPDATE_APK_MIME_TYPE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            pendingUpdateApkFile = null;
            pendingUpdateDownloadId = -1L;
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "没有找到可用的安装器", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w(TAG, "Could not install update APK", e);
            Toast.makeText(this, "无法打开更新安装包", Toast.LENGTH_SHORT).show();
        }
    }

    private void unregisterUpdateDownloadReceiver() {
        if (updateDownloadReceiver == null) {
            return;
        }

        try {
            unregisterReceiver(updateDownloadReceiver);
        } catch (IllegalArgumentException ignored) {
            // Receiver may already be unregistered when the Activity is torn down.
        } finally {
            updateDownloadReceiver = null;
        }
    }

    private String readResponseBody(InputStream inputStream) throws Exception {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        return body.toString();
    }

    private String normalizeGithubRepo(String repo) {
        if (repo == null) {
            return "";
        }

        String normalized = repo.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        if (normalized.startsWith("https://github.com/")) {
            normalized = normalized.substring("https://github.com/".length());
        } else if (normalized.startsWith("http://github.com/")) {
            normalized = normalized.substring("http://github.com/".length());
        }

        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }

        int hashIndex = normalized.indexOf('#');
        if (hashIndex >= 0) {
            normalized = normalized.substring(0, hashIndex);
        }

        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        normalized = normalized.replace('\\', '/');
        String[] parts = normalized.split("/");
        if (parts.length < 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
            return "";
        }

        return parts[0] + "/" + parts[1];
    }

    private String getCurrentVersionName() {
        try {
            PackageManager packageManager = getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(getPackageName(), 0);
            return packageInfo.versionName == null ? "" : packageInfo.versionName;
        } catch (Exception ignored) {
            return BuildConfig.VERSION_NAME;
        }
    }

    private int compareVersions(String firstVersion, String secondVersion) {
        int[] firstParts = versionParts(firstVersion);
        int[] secondParts = versionParts(secondVersion);
        int length = Math.max(firstParts.length, secondParts.length);

        for (int i = 0; i < length; i++) {
            int first = i < firstParts.length ? firstParts[i] : 0;
            int second = i < secondParts.length ? secondParts[i] : 0;
            if (first != second) {
                return first > second ? 1 : -1;
            }
        }

        return 0;
    }

    private int[] versionParts(String version) {
        String normalized = normalizeVersion(version);
        if (normalized.isEmpty()) {
            return new int[] { 0 };
        }

        String[] tokens = normalized.split("\\.");
        int[] parts = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            try {
                parts[i] = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException ignored) {
                parts[i] = 0;
            }
        }
        return parts;
    }

    private String normalizeVersion(String version) {
        if (version == null) {
            return "";
        }

        String normalized = version.trim().toLowerCase(Locale.US);
        if (normalized.startsWith("v")) {
            normalized = normalized.substring(1);
        }

        StringBuilder result = new StringBuilder();
        boolean lastWasDot = false;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c >= '0' && c <= '9') {
                result.append(c);
                lastWasDot = false;
            } else if ((c == '.' || c == '-' || c == '_') && result.length() > 0 && !lastWasDot) {
                result.append('.');
                lastWasDot = true;
            } else if (result.length() > 0) {
                break;
            }
        }

        while (result.length() > 0 && result.charAt(result.length() - 1) == '.') {
            result.deleteCharAt(result.length() - 1);
        }

        return result.toString();
    }

    private String sanitizeApkFileName(String fileName) {
        String sanitized = sanitizeFilePart(fileName);
        if (!sanitized.toLowerCase(Locale.US).endsWith(".apk")) {
            sanitized += ".apk";
        }
        return sanitized;
    }

    private String sanitizeFilePart(String value) {
        String sanitized = value == null ? "" : value.replaceAll("[^A-Za-z0-9._-]", "_");
        return sanitized.isEmpty() ? "yyjapi-update.apk" : sanitized;
    }

    private boolean isHttpOrHttps(Uri uri) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }

        String scheme = uri.getScheme().toLowerCase(Locale.US);
        return "http".equals(scheme) || "https".equals(scheme);
    }

    private boolean isAllowedHost(String host) {
        return host != null && APP_HOSTS.contains(host.toLowerCase(Locale.US));
    }

    private boolean shouldLoadInWebView(Uri uri, String currentUrl) {
        if (uri == null || uri.getScheme() == null) {
            return false;
        }

        String scheme = uri.getScheme().toLowerCase(Locale.US);
        if ("data".equals(scheme) || "blob".equals(scheme) || "about".equals(scheme)) {
            return true;
        }

        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            return false;
        }

        String host = uri.getHost();
        if (isAllowedHost(host)) {
            return true;
        }

        return isOAuthHost(host) && (isAuthContext(currentUrl) || looksLikeOAuthUrl(uri));
    }

    private boolean isOAuthHost(String host) {
        return host != null && OAUTH_HOSTS.contains(host.toLowerCase(Locale.US));
    }

    private boolean isAuthContext(String currentUrl) {
        if (currentUrl == null) {
            return false;
        }

        try {
            Uri currentUri = Uri.parse(currentUrl);
            if (isOAuthHost(currentUri.getHost())) {
                return true;
            }

            if (!isAllowedHost(currentUri.getHost())) {
                return false;
            }

            return containsAuthToken(currentUri.getPath());
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean looksLikeOAuthUrl(Uri uri) {
        String path = uri.getPath();
        String query = uri.getEncodedQuery();

        return containsAuthToken(path)
            || containsAuthToken(query)
            || containsIgnoreCase(query, "client_id")
            || containsIgnoreCase(query, "redirect_uri")
            || containsIgnoreCase(query, "return_to")
            || containsIgnoreCase(query, "state");
    }

    private boolean containsAuthToken(String value) {
        return containsIgnoreCase(value, "oauth")
            || containsIgnoreCase(value, "authorize")
            || containsIgnoreCase(value, "login")
            || containsIgnoreCase(value, "signin")
            || containsIgnoreCase(value, "sign-in")
            || containsIgnoreCase(value, "callback")
            || containsIgnoreCase(value, "session");
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.US).contains(needle);
    }

    private String getCurrentWebViewUrl() {
        if (bridge == null || bridge.getWebView() == null) {
            return null;
        }

        return bridge.getWebView().getUrl();
    }

    private void configurePopupWebView(WebView popupWebView) {
        WebSettings settings = popupWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
            CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, true);
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_AUTHENTICATION)) {
            WebSettingsCompat.setWebAuthenticationSupport(
                settings,
                WebSettingsCompat.WEB_AUTHENTICATION_SUPPORT_FOR_APP
            );
        }
    }

    private void closePopup(WebView popupWebView) {
        try {
            popupWebView.stopLoading();
            popupWebView.destroy();
        } catch (Exception ignored) {
            // Popup WebViews are transient.
        }
    }

    private void openInAppBrowserTab(Uri uri) {
        try {
            CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(Color.WHITE)
                .build();
            CustomTabsIntent customTabsIntent = new CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(params)
                .build();
            customTabsIntent.launchUrl(this, uri);
        } catch (ActivityNotFoundException e) {
            openExternal(uri);
        }
    }

    private void openExternal(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            // Leave the WebView on the current page when Android has no handler.
        }
    }

    private class AppWebChromeClient extends BridgeWebChromeClient {

        AppWebChromeClient(Bridge bridge) {
            super(bridge);
        }

        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            WebView popupWebView = new WebView(view.getContext());
            configurePopupWebView(popupWebView);
            popupWebView.setWebViewClient(new PopupWebViewClient(bridge, view, popupWebView));

            WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
            transport.setWebView(popupWebView);
            resultMsg.sendToTarget();
            return true;
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            if (consoleMessage != null) {
                String message = consoleMessage.message();
                if (
                    containsIgnoreCase(message, "passkey")
                        || containsIgnoreCase(message, "credential")
                        || containsIgnoreCase(message, "webauthn")
                ) {
                    Log.d(TAG, "Web console: " + message);
                }
            }

            return super.onConsoleMessage(consoleMessage);
        }
    }

    private class PopupWebViewClient extends BridgeWebViewClient {

        private final WebView parentWebView;
        private final WebView popupWebView;

        PopupWebViewClient(Bridge bridge, WebView parentWebView, WebView popupWebView) {
            super(bridge);
            this.parentWebView = parentWebView;
            this.popupWebView = popupWebView;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            if (request == null || !request.isForMainFrame()) {
                return false;
            }

            return handlePopupNavigation(request.getUrl());
        }

        @Deprecated
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return url != null && handlePopupNavigation(Uri.parse(url));
        }

        private boolean handlePopupNavigation(Uri uri) {
            if (shouldLoadInWebView(uri, parentWebView.getUrl())) {
                parentWebView.loadUrl(uri.toString());
            } else if (isHttpOrHttps(uri)) {
                openInAppBrowserTab(uri);
            } else {
                openExternal(uri);
            }

            closePopup(popupWebView);
            return true;
        }
    }
}
