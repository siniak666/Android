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

package com.duckduckgo.app.generalsettings.showonapplaunch.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.duckduckgo.app.generalsettings.showonapplaunch.ShowOnAppLaunchUrlConverterImpl
import com.duckduckgo.app.generalsettings.showonapplaunch.UrlConverter
import com.duckduckgo.di.scopes.AppScope
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@ContributesTo(AppScope::class)
@Module
object ShowOnAppLaunchDataStoreModule {

    private val Context.showOnAppLaunchDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "show_on_app_launch",
    )

    @Provides
    @ShowOnAppLaunch
    fun showOnAppLaunchDataStore(context: Context): DataStore<Preferences> = context.showOnAppLaunchDataStore

    @Provides
    fun showOnAppLaunchUrlConverter(): UrlConverter = ShowOnAppLaunchUrlConverterImpl()
}

@Qualifier
annotation class ShowOnAppLaunch