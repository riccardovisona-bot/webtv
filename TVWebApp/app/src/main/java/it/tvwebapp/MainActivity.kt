package it.tvwebapp

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.app.AlertDialog
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast

/**
 * App TV che apre un sito a schermo intero.
 * - Frecce: muovono il puntatore (al bordo dello schermo la pagina scorre)
 * - OK: clic
 * - Indietro: pagina precedente / esci
 * - Tasto 0 o Info: attiva/disattiva il puntatore (navigazione classica con le frecce)
 * - Tasto Menu (o tenere premuto OK 2 secondi): cambia il sito
 * Il sito viene chiesto al primo avvio e salvato.
 */
class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private lateinit var cursor: View
    private var cx = 0f
    private var cy = 0f
    private var cursorMode = true
    private var okDownAt = 0L
    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(-1, -1))

        val size = (22 * resources.displayMetrics.density).toInt()
        cursor = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xDDFFFFFF.toInt())
                setStroke((3 * resources.displayMetrics.density).toInt(), Color.BLACK)
            }
            elevation = 50f
        }
        root.addView(cursor, FrameLayout.LayoutParams(size, size))
        setContentView(root)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            userAgentString = userAgentString.replace("; wv", "")
        }
        web.webViewClient = WebViewClient()
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                fullscreenView = view
                fullscreenCallback = callback
                root.addView(view, FrameLayout.LayoutParams(-1, -1))
                web.visibility = View.GONE
                cursor.visibility = View.GONE
            }
            override fun onHideCustomView() { exitFullscreen() }
        }

        val saved = getPreferences(MODE_PRIVATE).getString("url", "")!!.trim()
        val url = if (saved.isNotEmpty()) saved else getString(R.string.site_url).trim()
        if (url.isEmpty()) askUrl() else web.loadUrl(url)

        root.post { cx = root.width / 2f; cy = root.height / 2f; placeCursor() }
    }

    private fun askUrl() {
        val current = getPreferences(MODE_PRIVATE).getString("url", "")!!
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setText(if (current.isEmpty()) "https://" else current)
            setSelection(text.length)
            setSingleLine()
        }
        AlertDialog.Builder(this)
            .setTitle("Indirizzo del sito")
            .setMessage("Scrivi l'indirizzo completo, es. https://www.esempio.it")
            .setView(input)
            .setCancelable(current.isNotEmpty())
            .setPositiveButton("Apri") { _, _ ->
                var u = input.text.toString().trim()
                if (u.isNotEmpty() && !u.startsWith("http")) u = "https://$u"
                if (u.length > 8) {
                    getPreferences(MODE_PRIVATE).edit().putString("url", u).apply()
                    web.clearHistory()
                    web.loadUrl(u)
                } else askUrl()
            }
            .show()
    }

    private fun exitFullscreen() {
        fullscreenView?.let { root.removeView(it) }
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
        web.visibility = View.VISIBLE
        cursor.visibility = if (cursorMode) View.VISIBLE else View.GONE
    }

    private fun placeCursor() {
        cursor.x = cx - cursor.width / 2f
        cursor.y = cy - cursor.height / 2f
    }

    private fun scrollPage(dx: Int, dy: Int) {
        web.evaluateJavascript("window.scrollBy($dx,$dy);", null)
    }

    private fun click() {
        val t = SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, cx, cy, 0)
        val up = MotionEvent.obtain(t, t + 50, MotionEvent.ACTION_UP, cx, cy, 0)
        web.dispatchTouchEvent(down); web.dispatchTouchEvent(up)
        down.recycle(); up.recycle()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val code = event.keyCode

        if (code == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                when {
                    fullscreenView != null -> exitFullscreen()
                    web.canGoBack() -> web.goBack()
                    else -> finish()
                }
            }
            return true
        }

        if (fullscreenView != null) return super.dispatchKeyEvent(event)

        if (code == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_UP) askUrl()
            return true
        }

        if (code == KeyEvent.KEYCODE_INFO || code == KeyEvent.KEYCODE_0) {
            if (event.action == KeyEvent.ACTION_UP) {
                cursorMode = !cursorMode
                cursor.visibility = if (cursorMode) View.VISIBLE else View.GONE
                Toast.makeText(this, if (cursorMode) "Puntatore attivo" else "Navigazione con le frecce", Toast.LENGTH_SHORT).show()
            }
            return true
        }

        if (!cursorMode) return super.dispatchKeyEvent(event)

        val density = resources.displayMetrics.density
        val step = 12 * density * (1 + minOf(event.repeatCount, 12) * 0.4f)
        val margin = 10 * density
        val scroll = (120 * density).toInt()

        when (code) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (code) {
                        KeyEvent.KEYCODE_DPAD_UP -> if (cy - step < margin) { cy = margin; scrollPage(0, -scroll) } else cy -= step
                        KeyEvent.KEYCODE_DPAD_DOWN -> if (cy + step > root.height - margin) { cy = root.height - margin; scrollPage(0, scroll) } else cy += step
                        KeyEvent.KEYCODE_DPAD_LEFT -> if (cx - step < margin) { cx = margin; scrollPage(-scroll, 0) } else cx -= step
                        KeyEvent.KEYCODE_DPAD_RIGHT -> if (cx + step > root.width - margin) { cx = root.width - margin; scrollPage(scroll, 0) } else cx += step
                    }
                    placeCursor()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) okDownAt = SystemClock.uptimeMillis()
                if (event.action == KeyEvent.ACTION_UP) {
                    if (SystemClock.uptimeMillis() - okDownAt > 2000) askUrl() else click()
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onPause() { super.onPause(); web.onPause() }
    override fun onResume() { super.onResume(); web.onResume() }
    override fun onDestroy() { web.destroy(); super.onDestroy() }
}
