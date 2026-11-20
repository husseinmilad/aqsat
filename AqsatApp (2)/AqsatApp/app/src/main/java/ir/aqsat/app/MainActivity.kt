package ir.aqsat.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.*
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var picker: ValueCallback<Array<Uri>>? = null
    private var pendingSaveBytes: ByteArray? = null
    private var pendingSaveName: String = ""

    class Bridge(private val activity: MainActivity) {
        @JavascriptInterface fun schedule(json: String) = Notifier.schedule(activity.applicationContext, json)
        @JavascriptInterface fun test() = Notifier.notifyNow(activity.applicationContext, 1, "📒 مدیریت اقساط", "اعلان آزمایشی")

        /** Opens Android's system document picker and writes the exported file to the chosen URI. */
        @JavascriptInterface fun saveFile(name: String, mime: String, b64: String) {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            activity.runOnUiThread { activity.createBackupDocument(name, mime, bytes) }
        }
    }

    private fun createBackupDocument(name: String, mime: String, bytes: ByteArray) {
        pendingSaveBytes = bytes
        pendingSaveName = name
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = mime.ifBlank { "application/octet-stream" }
            putExtra(Intent.EXTRA_TITLE, name)
        }
        try {
            startActivityForResult(intent, 8)
        } catch (e: Exception) {
            pendingSaveBytes = null
            Toast.makeText(this, "باز کردن محل ذخیره فایل ممکن نشد.", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        if (android.os.Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                picker?.onReceiveValue(null)
                picker = cb
                startActivityForResult(p.createIntent(), 7) // receipts and JSON restore
                return true
            }
        }
        web.addJavascriptInterface(Bridge(this), "Android")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        super.onActivityResult(rc, res, d)
        when (rc) {
            7 -> {
                picker?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, d))
                picker = null
            }
            8 -> {
                val bytes = pendingSaveBytes
                val name = pendingSaveName
                pendingSaveBytes = null
                if (res == Activity.RESULT_OK && d?.data != null && bytes != null) {
                    try {
                        contentResolver.openOutputStream(d.data!!, "w")!!.use { it.write(bytes) }
                        Toast.makeText(this, "نسخه پشتیبان ذخیره شد: $name", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "ذخیره فایل ناموفق بود: ${e.localizedMessage ?: "خطای نوشتن"}", Toast.LENGTH_LONG).show()
                    }
                } else if (res != Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "ذخیره نسخه پشتیبان انجام نشد.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}
