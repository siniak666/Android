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

package com.duckduckgo.networkprotection.impl

import com.duckduckgo.app.di.AppCoroutineScope
import com.duckduckgo.common.utils.DispatcherProvider
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.feature.toggles.api.FeatureSettings
import com.duckduckgo.feature.toggles.api.RemoteFeatureStoreNamed
import com.duckduckgo.networkprotection.api.NetworkProtectionState
import com.duckduckgo.networkprotection.store.db.CategorizedSystemApp
import com.duckduckgo.networkprotection.store.db.CategorizedSystemAppsDao
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import dagger.SingleInstanceIn
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.asLog
import logcat.logcat

@ContributesBinding(AppScope::class)
@RemoteFeatureStoreNamed(VpnRemoteFeatures::class)
@SingleInstanceIn(AppScope::class)
class VpnRemoteSettingsStore @Inject constructor(
    @AppCoroutineScope private val coroutineScope: CoroutineScope,
    private val dispatcherProvider: DispatcherProvider,
    private val networkProtectionState: NetworkProtectionState,
    private val categorizedSystemAppsDao: CategorizedSystemAppsDao,
) : FeatureSettings.Store {

    private val jsonAdapter = Moshi.Builder().build().adapter(SettingsModel::class.java)

    override fun store(jsonString: String) {
        logcat { "Received configuration: $jsonString" }

        runCatching {
            jsonAdapter.fromJson(jsonString)?.let { model ->
                model.systemAppCategories.also {
                    if (it.isNotEmpty()) {
                        categorizedSystemAppsDao.upsertSystemAppCategories(it)
                    }
                }

                // Restart VPN now that the lists were updated
                coroutineScope.launch(dispatcherProvider.io()) {
                    networkProtectionState.restart()
                }
            }
        }.onFailure {
            logcat(LogPriority.WARN) { it.asLog() }
        }
    }

    data class SettingsModel(
        @field:Json(name = "systemAppCategories")
        val systemAppCategories: List<CategorizedSystemApp>,
    )
}
