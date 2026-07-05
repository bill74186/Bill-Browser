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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bill.browser.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val tabs = mutableListOf<Tab>()
    private var currentTab: Tab? = null
    private var nextTabId = 1

    private val homepage: String by lazy { getString(R.string.default_homepage) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        addNewTab(homepage, select = true)
    }

    // --- WebView 工厂 ---

    private fun createWebView(): WebView {
        return WebView(this).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
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
        }
    }

    // --- 标签管理 ---

    private fun addNewTab(url: String = homepage, select: Boolean = true): Tab {
        val webView = createWebView()
        val tab = Tab(id = nextTabId++, webView = webView, url = url)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString().orEmpty()
                return !url.startsWith("http://") && !url.startsWith("https://")
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                tab.url = url ?: ""
                if (tab == currentTab) {
                    binding.progressBar.apply {
                        progress = 0
                        visibility = View.VISIBLE
                    }
                    updateUrlBar(tab.url)
                    updateNavButtons()
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                tab.url = url ?: ""
                tab.title = view?.title ?: ""
                if (tab == currentTab) {
                    binding.progressBar.visibility = View.GONE
                    updateUrlBar(tab.url)
                    updateNavButtons()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (tab == currentTab) {
                    binding.progressBar.progress = newProgress
                }
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                tab.title = title ?: ""
            }
        }

        webView.loadUrl(url)
        tabs.add(tab)

        if (select) {
            selectTab(tab)
        } else {
            updateTabCountBadge()
        }

        return tab
    }

    private fun selectTab(tab: Tab) {
        if (currentTab == tab) return

        currentTab?.webView?.let {
            binding.webViewContainer.removeView(it)
        }

        binding.webViewContainer.addView(tab.webView)
        currentTab = tab

        updateUrlBar(tab.url)
        updateNavButtons()
        updateTabCountBadge()

        if (tab.webView.progress in 1..99) {
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.progress = tab.webView.progress
        } else {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun closeTab(tab: Tab) {
        val index = tabs.indexOf(tab)
        if (index < 0) return

        if (tab == currentTab) {
            val nextTab = when {
                tabs.size > 1 && index < tabs.size - 1 -> tabs[index + 1]
                tabs.size > 1 && index > 0 -> tabs[index - 1]
                else -> null
            }
            if (nextTab != null) {
                selectTab(nextTab)
            } else {
                currentTab = null
                binding.webViewContainer.removeAllViews()
                addNewTab(homepage, select = true)
                return
            }
        }

        binding.webViewContainer.removeView(tab.webView)
        tab.webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            destroy()
        }
        tabs.removeAt(index)
        updateTabCountBadge()
    }

    // --- UI 更新 ---

    private fun updateUrlBar(url: String) {
        binding.etUrl.setText(
            if (url.startsWith("file://")) getString(R.string.app_name) else url
        )
    }

    private fun updateNavButtons() {
        val wv = currentTab?.webView ?: return
        binding.btnBack.isEnabled = wv.canGoBack()
        binding.btnForward.isEnabled = wv.canGoForward()
    }

    private fun updateTabCountBadge() {
        binding.tvTabCount.text = tabs.size.toString()
        binding.tvTabCount.visibility = if (tabs.size <= 99) View.VISIBLE else View.GONE
    }

    // --- 弹层 ---

    private fun showTabsDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_tabs, null)

        val rvTabs = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvTabs)
        rvTabs.layoutManager = LinearLayoutManager(this)

        fun refreshAdapter() {
            rvTabs.adapter = TabAdapter(
                tabs = tabs.toList(),
                currentTabId = currentTab?.id ?: -1,
                onTabClick = { tabId ->
                    tabs.find { it.id == tabId }?.let { selectTab(it) }
                    dialog.dismiss()
                },
                onTabClose = { tabId ->
                    tabs.find { it.id == tabId }?.let { closeTab(it) }
                    refreshAdapter()
                    if (tabs.isEmpty()) dialog.dismiss()
                }
            )
        }
        refreshAdapter()

        val newTabAction: () -> Unit = {
            addNewTab(homepage, select = true)
            dialog.dismiss()
        }
        view.findViewById<View>(R.id.btnNewTab).setOnClickListener { newTabAction() }
        view.findViewById<View>(R.id.tvNewTab).setOnClickListener { newTabAction() }

        dialog.setContentView(view)
        dialog.show()
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

    // --- 事件监听 ---

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            currentTab?.webView?.takeIf { it.canGoBack() }?.goBack()
        }
        binding.btnForward.setOnClickListener {
            currentTab?.webView?.takeIf { it.canGoForward() }?.goForward()
        }
        binding.btnRefresh.setOnClickListener { currentTab?.webView?.reload() }
        binding.btnHome.setOnClickListener { currentTab?.webView?.loadUrl(homepage) }
        binding.btnGo.setOnClickListener { loadInputUrl() }

        binding.btnTabs.setOnClickListener { showTabsDialog() }
        binding.btnMenu.setOnClickListener { showMenu() }

        binding.etUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadInputUrl()
                true
            } else false
        }
    }

    private fun loadInputUrl() {
        val raw = binding.etUrl.text.toString().trim()
        if (raw.isEmpty()) return

        val url = when {
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            raw.contains(".") && !raw.contains(" ") -> "https://$raw"
            else -> "https://www.baidu.com/s?wd=${java.net.URLEncoder.encode(raw, "UTF-8")}"
        }
        currentTab?.webView?.loadUrl(url)
        currentTab?.webView?.requestFocus()
    }

    // --- 菜单功能 ---

    private fun shareCurrentPage() {
        val url = currentTab?.url ?: return
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, url)
        }
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.share)))
    }

    private fun copyUrl() {
        val url = currentTab?.url ?: return
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("URL", url)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun clearHistory() {
        currentTab?.webView?.clearHistory()
        updateNavButtons()
        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
    }

    // --- 生命周期 & 按键 ---

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            currentTab?.webView?.let {
                if (it.canGoBack()) {
                    it.goBack()
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentTab?.webView?.saveState(outState)
    }

    override fun onDestroy() {
        tabs.forEach { tab ->
            tab.webView.apply {
                stopLoading()
                loadUrl("about:blank")
                clearHistory()
                (parent as? android.view.ViewGroup)?.removeView(this)
                destroy()
            }
        }
        tabs.clear()
        super.onDestroy()
    }
}
