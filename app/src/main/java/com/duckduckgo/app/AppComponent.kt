/*
 * Copyright (c) 2023 DuckDuckGo
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

package com.duckduckgo.app

import android.app.Application
import com.duckduckgo.app.di.AppCoroutineScope
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.widget.EmptyFavoritesWidgetService
import com.duckduckgo.widget.FavoritesWidgetService
import com.duckduckgo.widget.SearchAndFavoritesWidget
import com.duckduckgo.widget.SearchWidget
import com.squareup.anvil.annotations.MergeComponent
import dagger.BindsInstance
import dagger.Component
import dagger.SingleInstanceIn
import dagger.android.AndroidInjector
import kotlinx.coroutines.CoroutineScope
// import retrofit2.Retrofit

@SingleInstanceIn(AppScope::class)
@MergeComponent(scope = AppScope::class)
interface AppComponent : AndroidInjector<DuckDuckGoApplication> {

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun applicationCoroutineScope(@AppCoroutineScope applicationCoroutineScope: CoroutineScope): Builder

        fun build(): AppComponent
    }

    fun inject(searchWidget: SearchWidget)

    fun inject(searchAndFavsWidget: SearchAndFavoritesWidget)

    fun inject(favoritesWidgetItemFactory: FavoritesWidgetService.FavoritesWidgetItemFactory)

    fun inject(emptyFavoritesWidgetItemFactory: EmptyFavoritesWidgetService.EmptyFavoritesWidgetItemFactory)

    // accessor to Retrofit instance for test only only for test
    // @Named("api")
    // fun retrofit(): Retrofit
}
