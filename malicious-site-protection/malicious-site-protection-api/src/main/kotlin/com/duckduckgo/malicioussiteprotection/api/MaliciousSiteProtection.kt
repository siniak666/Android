/*
 * Copyright (c) 2024 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.malicioussiteprotection.api

import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView

interface MaliciousSiteProtection {
    suspend fun shouldIntercept(
        request: WebResourceRequest,
        webView: WebView,
        documentUri: Uri?,
        onSiteBlockedAsync: () -> Unit,
    ): WebResourceResponse?

    fun onPageLoadStarted()

    fun shouldOverrideUrlLoading(
        url: Uri,
        webView: WebView,
        isForMainFrame: Boolean,
        isRedirect: Boolean,
        onSiteBlockedAsync: () -> Unit,
    ): Boolean
}
