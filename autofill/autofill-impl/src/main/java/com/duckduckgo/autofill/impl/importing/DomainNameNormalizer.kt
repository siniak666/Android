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

package com.duckduckgo.autofill.impl.importing

import com.duckduckgo.autofill.impl.urlmatcher.AutofillUrlMatcher
import com.duckduckgo.di.scopes.AppScope
import com.squareup.anvil.annotations.ContributesBinding
import javax.inject.Inject

interface DomainNameNormalizer {
    suspend fun normalize(unnormalizedUrl: String?): String?
}

@ContributesBinding(AppScope::class)
class DefaultDomainNameNormalizer @Inject constructor(
    private val urlMatcher: AutofillUrlMatcher,
) : DomainNameNormalizer {
    override suspend fun normalize(unnormalizedUrl: String?): String? {
        return if (unnormalizedUrl == null) {
            null
        } else {
            urlMatcher.cleanRawUrl(unnormalizedUrl)
        }
    }
}
