package com.example

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.webkit.WebViewAssetLoader
import java.io.File

/**
 * Billing app (MyBillBook jaisa) — ye pura web app phone ke andar bundle hokar chalta hai.
 *
 * - Assets: `app/src/main/assets/webapp/`  (banane ke liye: `cd webapp && npm run build:android`)
 * - Offline chalta hai, data phone me hi (IndexedDB) rehta hai
 * - Print: Android ke print framework se (PDF / printer)
 * - Download (CSV backup, PNG bill): seedha Download folder me
 * - WhatsApp share: bahar ka intent
 */
class BillingWebActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView = WebView(this)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
            useWideViewPort = false
            loadWithOverviewMode = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest,
            ): WebResourceResponse? = assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                if (url.host == APP_HOST) return false
                return openExternally(url)
            }

            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript(BRIDGE_JS, null)
            }
        }

        webView.addJavascriptInterface(Bridge(), "AndroidBridge")

        setContentView(webView)
        webView.loadUrl(START_URL)

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }

    private fun openExternally(uri: Uri): Boolean = try {
        startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }

    /** Web app ke liye native kaam: print, file save, share */
    inner class Bridge {

        @JavascriptInterface
        fun printPage() {
            runOnUiThread {
                try {
                    val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val jobName = "Showroom Bill"
                    printManager.print(
                        jobName,
                        webView.createPrintDocumentAdapter(jobName),
                        PrintAttributes.Builder().build(),
                    )
                } catch (e: Exception) {
                    toast("Print nahi ho paya: ${e.message}")
                }
            }
        }

        @JavascriptInterface
        fun saveBase64(name: String, mime: String, base64: String) {
            runOnUiThread {
                try {
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "showroom-file" }
                    val ok = writeFile(safeName, mime, bytes)
                    toast(if (ok) "Save ho gaya: Download/$safeName" else "File save nahi ho payi")
                } catch (e: Exception) {
                    toast("File save nahi ho payi: ${e.message}")
                }
            }
        }

        @JavascriptInterface
        fun shareText(text: String) {
            runOnUiThread {
                try {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    startActivity(Intent.createChooser(send, "Share bill"))
                } catch (_: Exception) {
                    toast("Share nahi ho paya")
                }
            }
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    /** CSV / JSON / PNG ko Download folder me likhta hai (Android 10+ me MediaStore se) */
    private fun writeFile(name: String, mime: String, bytes: ByteArray): Boolean {
        val safeMime = mime.ifBlank { "application/octet-stream" }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, safeMime)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val uri = contentResolver.insert(collection, values) ?: return false
                contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
                true
            } else {
                val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: filesDir
                if (!dir.exists()) dir.mkdirs()
                File(dir, name).writeBytes(bytes)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val APP_HOST = "appassets.androidplatform.net"
        private const val START_URL = "https://appassets.androidplatform.net/assets/webapp/index.html"

        /**
         * Web app ke andar `window.print()` aur file-download ko native kaam par bhejta hai
         * (Android WebView me print/share/download seedha kaam nahi karte).
         */
        private val BRIDGE_JS = """
            (function () {
              if (window.__showroomBridge) return;
              window.__showroomBridge = true;
              function bridge() { try { return window.AndroidBridge; } catch (e) { return null; } }
              var origPrint = window.print;
              window.print = function () {
                var b = bridge();
                if (b && b.printPage) { try { b.printPage(); return; } catch (e) {} }
                try { origPrint.call(window); } catch (e) {}
              };
              var blobs = {};
              var origCreate = URL.createObjectURL;
              URL.createObjectURL = function (obj) {
                var u = origCreate.call(URL, obj);
                try { if (obj && (obj instanceof Blob)) blobs[u] = obj; } catch (e) {}
                return u;
              };
              var origClick = HTMLAnchorElement.prototype.click;
              HTMLAnchorElement.prototype.click = function () {
                try {
                  var href = this.href || '';
                  var name = this.getAttribute('download') || 'showroom-file';
                  if (href.indexOf('blob:') === 0 && blobs[href]) {
                    var blob = blobs[href];
                    var b = bridge();
                    if (b && b.saveBase64) {
                      var fr = new FileReader();
                      fr.onload = function () {
                        var s = String(fr.result || '');
                        var i = s.indexOf(',');
                        b.saveBase64(name, blob.type || 'application/octet-stream', i >= 0 ? s.substring(i + 1) : '');
                      };
                      fr.readAsDataURL(blob);
                      return;
                    }
                  }
                } catch (e) {}
                return origClick.apply(this, arguments);
              };
            })();
        """.trimIndent()
    }
}
