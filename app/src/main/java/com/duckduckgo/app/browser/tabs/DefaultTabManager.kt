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

package com.duckduckgo.app.browser.tabs

import android.os.Message
import androidx.lifecycle.lifecycleScope
import com.duckduckgo.app.browser.BrowserActivity
import com.duckduckgo.app.browser.BrowserTabFragment
import com.duckduckgo.app.browser.R
import com.duckduckgo.app.browser.SwipingTabsFeature
import com.duckduckgo.app.browser.tabs.TabManager.Companion.MAX_ACTIVE_TABS
import com.duckduckgo.app.tabs.model.TabEntity
import com.duckduckgo.app.tabs.model.TabRepository
import com.duckduckgo.di.scopes.ActivityScope
import com.squareup.anvil.annotations.ContributesBinding
import dagger.SingleInstanceIn
import dagger.android.DaggerActivity
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@ContributesBinding(ActivityScope::class)
@SingleInstanceIn(ActivityScope::class)
class DefaultTabManager @Inject constructor(
    activity: DaggerActivity,
    private val swipingTabsFeature: SwipingTabsFeature,
    private val tabRepository: TabRepository,
) : TabManager {
    private val browserActivity = activity as BrowserActivity
    private val lastActiveTabs = TabList()
    private val supportFragmentManager = activity.supportFragmentManager
    private var openMessageInNewTabJob: Job? = null

    override val tabPagerAdapter by lazy {
        TabPagerAdapter(
            fragmentManager = supportFragmentManager,
            lifecycle = browserActivity.lifecycle,
            activityIntent = browserActivity.intent,
            moveToTabIndex = { index, smoothScroll -> browserActivity.onMoveToTabRequested(index, smoothScroll) },
            getCurrentTabIndex = { browserActivity.tabPager.currentItem },
            getSelectedTabId = ::getSelectedTab,
            getTabById = ::getTabById,
            requestNewTab = ::requestNewTab,
            onTabSelected = ::getTabById,
            setOffScreenPageLimit = { limit -> browserActivity.tabPager.offscreenPageLimit = limit },
            getOffScreenPageLimit = { browserActivity.tabPager.offscreenPageLimit },
        )
    }

    private var _currentTab: BrowserTabFragment? = null
    override var currentTab: BrowserTabFragment?
        get() {
            return if (swipingTabsFeature.self().isEnabled()) {
                tabPagerAdapter.currentFragment
            } else {
                _currentTab
            }
        }
        set(value) {
            _currentTab = value
        }

    override fun onSelectedTabChanged(tab: TabEntity?) {
        if (tab != null) {
            if (swipingTabsFeature.self().isEnabled()) {
                tabPagerAdapter.onSelectedTabChanged(tab.tabId)
            } else {
                selectTab(tab)
            }
        }
    }

    override fun onTabsUpdated(updatedTabs: List<TabEntity>) {
        if (swipingTabsFeature.self().isEnabled()) {
            tabPagerAdapter.onTabsUpdated(updatedTabs.map { it.tabId })
        } else {
            clearStaleTabs(updatedTabs)
        }
    }

    override fun openMessageInNewTab(
        message: Message,
        sourceTabId: String?,
    ) {
        if (swipingTabsFeature.self().isEnabled()) {
            openMessageInNewTabJob = browserActivity.lifecycleScope.launch {
                tabPagerAdapter.setMessageForNewFragment(message)
                browserActivity.viewModel.onNewTabRequested(sourceTabId)
            }
        } else {
            openMessageInNewTabJob = browserActivity.lifecycleScope.launch {
                val tabId = browserActivity.viewModel.onNewTabRequested(sourceTabId)
                val fragment = openNewTab(
                    tabId = tabId,
                    url = null,
                    skipHome = false,
                    isExternal = browserActivity.intent?.getBooleanExtra(
                        BrowserActivity.LAUNCH_FROM_EXTERNAL_EXTRA,
                        false,
                    ) == true,
                )
                fragment.messageFromPreviousTab = message
            }
        }
    }

    override fun openExistingTab(tabId: String) {
        browserActivity.lifecycleScope.launch {
            browserActivity.viewModel.onTabSelected(tabId)
        }
    }

    override fun launchNewTab() {
        browserActivity.lifecycleScope.launch { browserActivity.viewModel.onNewTabRequested() }
    }

    override fun openQueryInNewTab(
        query: String,
        sourceTabId: String?,
    ) {
        browserActivity.lifecycleScope.launch {
            browserActivity.viewModel.onOpenInNewTabRequested(
                query = query,
                sourceTabId = sourceTabId,
            )
        }
    }

    override fun onCleanup() {
        openMessageInNewTabJob?.cancel()
    }

    private fun requestNewTab(): TabEntity = runBlocking {
        val tabId = browserActivity.viewModel.onNewTabRequested()
        tabRepository.flowTabs.first().first { it.tabId == tabId }
    }

    private fun getSelectedTab(): String? = runBlocking {
        tabRepository.flowSelectedTab.firstOrNull()?.tabId
    }

    private fun getTabById(tabId: String): TabEntity? = runBlocking {
        tabRepository.flowTabs.first().firstOrNull { it.tabId == tabId }
    }

    private fun selectTab(tab: TabEntity) {
        Timber.v("Select tab: $tab")

        if (tab.tabId == currentTab?.tabId) return

        lastActiveTabs.add(tab.tabId)

        browserActivity.viewModel.onTabActivated(tab.tabId)

        val fragment = supportFragmentManager.findFragmentByTag(tab.tabId) as? BrowserTabFragment
        if (fragment == null) {
            openNewTab(
                tabId = tab.tabId,
                url = tab.url,
                skipHome = tab.skipHome,
                isExternal = browserActivity.intent?.getBooleanExtra(
                    BrowserActivity.LAUNCH_FROM_EXTERNAL_EXTRA,
                    false,
                ) == true,
            )
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        currentTab?.let {
            transaction.hide(it)
        }
        transaction.show(fragment)
        transaction.commit()
        currentTab = fragment
    }

    private fun openNewTab(
        tabId: String,
        url: String? = null,
        skipHome: Boolean,
        isExternal: Boolean,
    ): BrowserTabFragment {
        Timber.i("Opening new tab, url: $url, tabId: $tabId")
        val fragment = BrowserTabFragment.newInstance(tabId, url, skipHome, isExternal)
        addOrReplaceNewTab(fragment, tabId)
        currentTab = fragment
        return fragment
    }

    private fun addOrReplaceNewTab(
        fragment: BrowserTabFragment,
        tabId: String,
    ) {
        if (supportFragmentManager.isStateSaved) {
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        val tab = currentTab
        if (tab == null) {
            transaction.replace(R.id.fragmentContainer, fragment, tabId)
        } else {
            transaction.hide(tab)
            transaction.add(R.id.fragmentContainer, fragment, tabId)
        }
        transaction.commit()
    }

    private fun clearStaleTabs(updatedTabs: List<TabEntity>?) {
        if (swipingTabsFeature.self().isEnabled()) {
            return
        }

        if (updatedTabs == null) {
            return
        }

        val stale = supportFragmentManager
            .fragments.mapNotNull { it as? BrowserTabFragment }
            .filter { fragment -> updatedTabs.none { it.tabId == fragment.tabId } }

        if (stale.isNotEmpty()) {
            removeTabs(stale)
        }

        removeOldTabs()
    }

    private fun removeOldTabs() {
        val candidatesToRemove = lastActiveTabs.dropLast(MAX_ACTIVE_TABS)
        if (candidatesToRemove.isEmpty()) return

        val tabsToRemove = supportFragmentManager.fragments
            .mapNotNull { it as? BrowserTabFragment }
            .filter { candidatesToRemove.contains(it.tabId) }

        if (tabsToRemove.isNotEmpty()) {
            removeTabs(tabsToRemove)
        }
    }

    private fun removeTabs(fragments: List<BrowserTabFragment>) {
        val transaction = supportFragmentManager.beginTransaction()
        fragments.forEach {
            transaction.remove(it)
            lastActiveTabs.remove(it.tabId)
        }
        transaction.commit()
    }

    // Temporary class to keep track of latest visited tabs, keeping unique ids.
    private class TabList : ArrayList<String>() {
        override fun add(element: String): Boolean {
            if (this.contains(element)) {
                this.remove(element)
            }
            return super.add(element)
        }
    }
}
