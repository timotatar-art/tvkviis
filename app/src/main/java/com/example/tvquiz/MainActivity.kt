package com.example.tvquiz

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    // Päris backend: Cloudflare Worker + Durable Object mänguserver.
    // /tv loob iga kord uue mänguruumi ja kuvab TV ekraani.
    private val tvDisplayUrl = "https://quiz-backend.timo-tatar.workers.dev/tv"
    private val versionCheckUrl = "https://quiz-backend.timo-tatar.workers.dev/app-version"

    private lateinit var webView: WebView
    private var downloadId: Long = -1L
    private var downloadReceiverRegistered = false

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            if (id == downloadId) {
                installDownloadedApk()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        hideSystemUi()

        webView = findViewById(R.id.webview)
        configureWebView(webView)
        webView.loadUrl(tvDisplayUrl)

        checkForUpdate()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView(webView: WebView) {
        val settings: WebSettings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportZoom(false)
        settings.textZoom = 100

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    Toast.makeText(
                        this@MainActivity,
                        "Ei saanud ühendust serveriga. Kontrolli internetiiühendust.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.requestFocus()
    }

    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    // Küsib serverilt uusima ehituse numbrit ja võrdleb hetkel paigaldatuga.
    // Käib eraldi lõimes, et mitte blokeerida UI-d; tulemus tuuakse tagasi
    // peamisse lõime Handleri kaudu. Vaikne ebaõnnestumine - versioonikontroll
    // pole kriitiline, mäng töötab ka ilma selleta.
    private fun checkForUpdate() {
        Thread {
            try {
                val connection = URL(versionCheckUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()
                val json = JSONObject(body)
                val latestVersionCode = json.optInt("versionCode", 0)
                val apkUrl = json.optString("url", "")

                val currentVersionCode = BuildConfig.VERSION_CODE
                if (latestVersionCode > currentVersionCode && apkUrl.isNotEmpty()) {
                    Handler(Looper.getMainLooper()).post {
                        showUpdateDialog(latestVersionCode, apkUrl)
                    }
                }
            } catch (_: Exception) {
                // Server pole kättesaadav vms - jäta vaikimisi vahele.
            }
        }.start()
    }

    private fun showUpdateDialog(versionCode: Int, apkUrl: String) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle("Uuendus saadaval")
            .setMessage("LuVu Quiz'ist on saadaval uuem versioon (#$versionCode). Kas soovid selle kohe alla laadida ja installida?")
            .setPositiveButton("Installi") { _, _ -> startUpdateDownload(apkUrl) }
            .setNegativeButton("Hiljem", null)
            .setCancelable(true)
            .show()
    }

    private fun startUpdateDownload(apkUrl: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            Toast.makeText(
                this,
                "Luba tundmatute rakenduste installimine ja proovi uuendust uuesti.",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName"))
            startActivity(intent)
            return
        }
        downloadApk(apkUrl)
    }

    private fun downloadApk(apkUrl: String) {
        if (!downloadReceiverRegistered) {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(downloadReceiver, filter)
            }
            downloadReceiverRegistered = true
        }

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("LuVu Quiz uuendus")
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "luvu-update.apk")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = manager.enqueue(request)
        Toast.makeText(this, "Laadin uuendust alla...", Toast.LENGTH_SHORT).show()
    }

    private fun installDownloadedApk() {
        val destFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "luvu-update.apk")
        if (!destFile.exists()) return

        val apkUri = FileProvider.getUriForFile(this, "$packageName.fileprovider", destFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        startActivity(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        if (downloadReceiverRegistered) {
            unregisterReceiver(downloadReceiver)
        }
        webView.destroy()
        super.onDestroy()
    }
}
