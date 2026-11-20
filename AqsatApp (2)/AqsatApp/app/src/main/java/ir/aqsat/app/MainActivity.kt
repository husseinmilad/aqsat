package ir.aqsat.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.*
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var picker: ValueCallback<Array<Uri>>? = null

    class Bridge(private val c: Context) {
        @JavascriptInterface fun schedule(json: String) = Notifier.schedule(c, json)
        @JavascriptInterface fun test() = Notifier.notifyNow(c, 1, "📒 مدیریت اقساط", "اعلان آزمایشی")

        /** Saves PDF / Excel / JSON / PNG exported from the page into the Downloads folder. */
        @JavascriptInterface fun saveFile(name: String, mime: String, b64: String) {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val where: String
            if (Build.VERSION.SDK_INT >= 29) {
                val v = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name)
                    put(MediaStore.Downloads.MIME_TYPE, mime)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val u = c.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)!!
                c.contentResolver.openOutputStream(u)!!.use { it.write(bytes) }
                where = "Downloads"
            } else {
                File(c.getExternalFilesDir(null), name).writeBytes(bytes)
                where = "Android/data/ir.aqsat.app/files"
            }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(c, "ذخیره شد: $name ($where)", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                picker?.onReceiveValue(null); picker = cb
                startActivityForResult(p.createIntent(), 7)   // receipts (images) and JSON restore
                return true
            }
        }
        web.addJavascriptInterface(Bridge(applicationContext), "Android")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(rc: Int, res: Int, d: Intent?) {
        if (rc == 7) { picker?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, d)); picker = null }
        else super.onActivityResult(rc, res, d)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { if (web.canGoBack()) web.goBack() else super.onBackPressed() }
}
