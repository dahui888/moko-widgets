/*
 * Copyright 2020 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.widgets.factory

import android.annotation.TargetApi
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import dev.icerock.moko.mvvm.livedata.MutableLiveData
import dev.icerock.moko.widgets.WebViewWidget
import dev.icerock.moko.widgets.core.ViewBundle
import dev.icerock.moko.widgets.core.ViewFactory
import dev.icerock.moko.widgets.core.ViewFactoryContext
import dev.icerock.moko.widgets.style.applyBackgroundIfNeeded
import dev.icerock.moko.widgets.style.background.Background
import dev.icerock.moko.widgets.style.view.MarginValues
import dev.icerock.moko.widgets.style.view.WidgetSize
import dev.icerock.moko.widgets.utils.WebViewRedirectUrlHandler

actual class WebViewFactory actual constructor(
    private val margins: MarginValues?,
    private val background: Background?
) : ViewFactory<WebViewWidget<out WidgetSize>> {

    override fun <WS : WidgetSize> build(
        widget: WebViewWidget<out WidgetSize>,
        size: WS,
        viewFactoryContext: ViewFactoryContext
    ): ViewBundle<WS> {
        val context = viewFactoryContext.androidContext

        val root = WebView(context).apply {
            applyBackgroundIfNeeded(this@WebViewFactory.background)
            settings.javaScriptEnabled = widget.isJavaScriptEnabled

            webViewClient = CustomWebViewClient(
                successRedirectUrl = widget.successRedirectUrl,
                failureRedirectUrl = widget.failureRedirectUrl,
                onSuccessBlock = widget.onSuccessRedirectBlock,
                onFailureBlock = widget.onFailureRedirectBlock,
                isPageLoading = widget._isWebPageLoading
            )
            loadUrl(widget.targetUrl)
        }

        return ViewBundle(
            view = root,
            size = size,
            margins = margins
        )
    }

    private class CustomWebViewClient(
        successRedirectUrl: String?,
        failureRedirectUrl: String?,
        onSuccessBlock: ((String) -> Unit)? = null,
        onFailureBlock: ((String) -> Unit)? = null,
        private val isPageLoading: MutableLiveData<Boolean>
    ) : WebViewClient() {

        private val redirectUrlHandler = WebViewRedirectUrlHandler(
            successRedirectUrl = successRedirectUrl,
            failureRedirectUrl = failureRedirectUrl,
            onSuccessBlock = onSuccessBlock,
            onFailureBlock = onFailureBlock
        )

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            isPageLoading.value = true

            super.onPageStarted(view, url, favicon)
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            isPageLoading.value = false
            super.onPageFinished(view, url)
        }

        @SuppressWarnings("deprecation")
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            if (url == null)
                return true

            return redirectUrlHandler.handleUrl(url)
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            if (request == null)
                return true

            val requestedUrl = Uri.parse(request.url.toString()).toString()
            return redirectUrlHandler.handleUrl(requestedUrl)
        }

    }

}
