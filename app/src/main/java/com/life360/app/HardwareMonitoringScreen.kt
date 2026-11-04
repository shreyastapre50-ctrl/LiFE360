package com.life360.app

import android.webkit.WebChromeClient
import androidx.compose.ui.unit.dp
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun HardwareMonitoringScreen(
    onBack: () -> Unit
) {

    var isLoading by remember {
        mutableStateOf(true)
    }

    var hasError by remember {
        mutableStateOf(false)
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "Hardware Monitoring",
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(
                onClick = onBack
            ) {
                Text("Back")
            }
        }

        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (hasError) {
            Text(
                text = "Unable to connect to ESP32. Check your Wi-Fi connection.",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),

            factory = { context ->

                WebView(context).apply {

                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    webViewClient = object : WebViewClient() {

                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: android.graphics.Bitmap?
                        ) {
                            isLoading = true
                            hasError = false
                        }

                        override fun onPageFinished(
                            view: WebView?,
                            url: String?
                        ) {
                            isLoading = false
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                hasError = true
                            }
                        }
                    }

                    webChromeClient = WebChromeClient()

                    loadUrl("http://172.19.37.14")
                }
            }
        )
    }
}