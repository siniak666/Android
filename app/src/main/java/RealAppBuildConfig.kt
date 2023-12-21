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

package com.duckduckgo.app.global

import android.os.Build
import com.duckduckgo.app.abc.BuildConfig
import com.duckduckgo.appbuildconfig.api.AppBuildConfig
import com.duckduckgo.appbuildconfig.api.BuildFlavor
import com.duckduckgo.di.scopes.AppScope
import com.duckduckgo.experiments.api.VariantManager
import com.squareup.anvil.annotations.ContributesBinding
import com.squareup.anvil.annotations.ContributesTo
import dagger.Lazy
import dagger.Module
import dagger.Provides
import java.lang.IllegalStateException
import java.util.*
import javax.inject.Inject

@Module
@ContributesTo(AppScope::class)
object RealAppBuildConfigModule {

    @Provides
    fun providesRealAppBuildConfig(
        variantManager: Lazy<VariantManager>,
    ): AppBuildConfig = RealAppBuildConfig(variantManager)
}

@ContributesBinding(AppScope::class)
class RealAppBuildConfig @Inject constructor(
    private val variantManager: Lazy<VariantManager>, // break any possible DI dependency cycle
) : AppBuildConfig {
    override val isDebug: Boolean = BuildConfig.DEBUG
    override val applicationId: String = BuildConfig.APPLICATION_ID
    override val buildType: String = BuildConfig.BUILD_TYPE
    override val versionCode: Int = BuildConfig.VERSION_CODE
    override val versionName: String = BuildConfig.VERSION_NAME
    override val flavor: BuildFlavor
        get() = when (BuildConfig.FLAVOR) {
            "internal" -> BuildFlavor.INTERNAL
            "fdroid" -> BuildFlavor.FDROID
            "play" -> BuildFlavor.PLAY
            else -> throw IllegalStateException("Unknown app flavor")
        }
    override val sdkInt: Int = Build.VERSION.SDK_INT
    override val manufacturer: String = Build.MANUFACTURER
    override val model: String = Build.MODEL
    override val isTest by lazy {
        try {
            Class.forName("org.junit.Test")
            true
        } catch (e: Exception) {
            false
        }
    }
    override val isPerformanceTest: Boolean = BuildConfig.IS_PERFORMANCE_TEST

    override val isDefaultVariantForced: Boolean = BuildConfig.FORCE_DEFAULT_VARIANT
    override val deviceLocale: Locale
        get() = Locale.getDefault()

    override val variantName: String?
        get() = variantManager.get().getVariantKey()
}
