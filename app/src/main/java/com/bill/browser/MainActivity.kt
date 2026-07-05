package com.bill.browser

import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bill.browser.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val homepage: String by lazy { getString(R.string.default_homepage) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWebView()
        setupListeners()

        if (savedInstanceState == null) {
            binding.webView.loadUrl(homepage)
            binding.etUrl.setText(getString(R.string.app_name))
        } else {
            binding.webView.restoreState(savedInstanceState)
        }
    }

    private fun setupWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = true
            allowFileAccess = true
            allowContentAccess = true
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                view?.loadUrl(request?.url?.toString().orEmpty())
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                binding.progressBar.apply {
                    progress = 0
                    visibility = View.VISIBLE
                }
                url?.let {
                    binding.etUrl.setText(if (it.startsWith("file://")) getString(R.string.app_name) else it)
                }
                updateNavButtons()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                updateNavButtons()
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                binding.progressBar.progress = newProgress
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) binding.webView.goBack()
        }
        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }
        binding.btnRefresh.setOnClickListener { binding.webView.reload() }
        binding.btnHome.setOnClickListener { binding.webView.loadUrl(homepage) }
        binding.btnGo.setOnClickListener { loadInputUrl() }

        binding.btnTabs.setOnClickListener {
            Toast.makeText(this, R.string.tabs_hint, Toast.LENGTH_SHORT).show()
        }

        binding.btnMenu.setOnClickListener { showMenu() }

        binding.etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadInputUrl()
                true
            } else false
        }
    }

    private fun showMenu() {
        val popup = PopupMenu(this, binding.btnMenu)
        popup.menuInflater.inflate(R.menu.browser_menu, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> shareCurrentPage()
                R.id.action_copy_url -> copyUrl()
                R.id.action_clear_history -> clearHistory()
            }
            true
        }
        popup.show()
    }

    private fun shareCurrentPage() {
        val url = binding.webView.url ?: return
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, url)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun copyUrl() {
        val url = binding.webView.url ?: return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun clearHistory() {
        binding.webView.clearHistory()
        updateNavButtons()
        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun loadInputUrl() {
        val raw = binding.etUrl.text.toString().trim()
        if (raw.isEmpty()) return

        val url = when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.contains(".") && !raw.contains(" ") -> "https://$raw"
            else -> "https://www.baidu.com/s?wd=${java.net.URLEncoder.encode(raw, "UTF-8")}"
        }
        binding.webView.loadUrl(url)
        binding.webView.requestFocus()
    }

    private fun updateNavButtons() {
        binding.btnBack.isEnabled = binding.webView.canGoBack()
        binding.btnForward.isEnabled = binding.webView.canGoForward()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.webView.saveState(outState)
    }

    override fun onDestroy() {
        binding.webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            (parent as? android.view.ViewGroup)?.removeView(this)
            destroy()
        }
        super.onDestroy()
    }
}
