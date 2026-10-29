package com.redbus.ai.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.redbus.ai.data.local.SessionManager
import com.redbus.ai.ui.theme.RedbusCrimson
import com.redbus.ai.ui.viewmodel.RedbusUiState

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebPortalScreen(
    uiState: RedbusUiState,
    initialUrl: String = uiState.activeWebUrl,
    onSyncWebSessionToNative: (String?, String?) -> Unit
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isPageLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }

    BackHandler(enabled = canGoBack) {
        webViewRef?.goBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "redbusai.app • Live Cross-Platform Sync",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (uiState.isAuthenticated && uiState.user != null)
                                "Signed in as ${uiState.user.name} (₹${(uiState.user.walletBalance ?: 0.0).toInt()} Wallet)"
                            else
                                "Sign in on Web or App — Account syncs automatically",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            webViewRef?.evaluateJavascript(
                                """
                                (function() {
                                    var t = window.localStorage.getItem('redbus_token');
                                    var u = window.localStorage.getItem('redbus_user');
                                    if (window.AndroidBridge) {
                                        window.AndroidBridge.onSessionSynced(t || '', u || '');
                                    }
                                })();
                                """.trimIndent(),
                                null
                            )
                            webViewRef?.reload()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync & Reload",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = RedbusCrimson)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadsImagesAutomatically = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(false)
                            userAgentString = settings.userAgentString + " RedBusAIAndroid/2.0"
                        }

                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onSessionSynced(token: String, userJson: String) {
                                    onSyncWebSessionToNative(
                                        token.takeIf { it.isNotBlank() },
                                        userJson.takeIf { it.isNotBlank() }
                                    )
                                }
                            },
                            "AndroidBridge"
                        )

                        webChromeClient = WebChromeClient()

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isPageLoading = true
                                // If native app has a logged-in token, inject it into redbusai.app localStorage
                                val nativeToken = SessionManager.getToken()
                                val nativeUserJson = SessionManager.getUserJson()
                                if (!nativeToken.isNullOrBlank() && !nativeUserJson.isNullOrBlank()) {
                                    val escapedUser = nativeUserJson
                                        .replace("\\", "\\\\")
                                        .replace("'", "\\'")
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
                                            if (!window.localStorage.getItem('redbus_token')) {
                                                window.localStorage.setItem('redbus_token', '$nativeToken');
                                                window.localStorage.setItem('redbus_user', '$escapedUser');
                                            }
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isPageLoading = false
                                canGoBack = view?.canGoBack() ?: false

                                // Poll localStorage on redbusai.app so any Web Login (including Google/OTP) or Wallet update syncs back to Native Android immediately
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        function syncToNative() {
                                            var t = window.localStorage.getItem('redbus_token') || '';
                                            var u = window.localStorage.getItem('redbus_user') || '';
                                            if (window.AndroidBridge && t) {
                                                window.AndroidBridge.onSessionSynced(t, u);
                                            }
                                        }
                                        syncToNative();
                                        if (!window.__redbusAndroidSyncInterval) {
                                            window.__redbusAndroidSyncInterval = setInterval(syncToNative, 2500);
                                        }
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }
                        }

                        loadUrl(initialUrl)
                        webViewRef = this
                    }
                },
                update = { view ->
                    canGoBack = view.canGoBack()
                }
            )

            if (isPageLoading) {
                LinearProgressIndicator(
                    color = RedbusCrimson,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}
