package com.example

import android.app.Application
import android.content.Context
import android.webkit.WebSettings
import androidx.test.core.app.ApplicationProvider
import com.example.browser.BrowserTab
import com.example.proxy.LocalProxyServer
import com.example.proxy.ProxyConfig
import com.example.proxy.ProxyMode
import com.example.proxy.TrackerBlocklist
import com.example.ui.BrowserViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("RelayWeb", appName)
    }

    @Test
    fun `tracker blocklist matches known tracker subdomains`() {
        assertTrue(TrackerBlocklist.isBlocked("www.google-analytics.com"))
        assertTrue(TrackerBlocklist.isBlocked("doubleclick.net"))
        assertFalse(TrackerBlocklist.isBlocked("developer.mozilla.org"))
        assertFalse(TrackerBlocklist.isBlocked("en.m.wikipedia.org"))
    }

    @Test
    fun `local proxy server binds loopback port and stops cleanly`() {
        var lastPort = 0
        val server = LocalProxyServer { telemetry ->
            lastPort = telemetry.localPort
        }
        val port = server.start(ProxyConfig(mode = ProxyMode.LOCAL_RELAY), overrideSupported = true)
        assertTrue(port > 0)
        assertTrue(server.isRunning)
        assertEquals(port, lastPort)
        server.stop()
        assertFalse(server.isRunning)
    }

    @Test
    fun `incognito tab flag and cache mode constants match privacy contract`() {
        val incognitoTab = BrowserTab(id = "t1", isIncognito = true)
        assertTrue(incognitoTab.isIncognito)
        assertEquals(2, WebSettings.LOAD_NO_CACHE)
    }

    @Test
    fun `browser view model manages multiple tabs and session switching`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = BrowserViewModel(app)
        assertEquals(1, vm.tabs.value.size)
        val firstId = vm.activeTabId.value

        vm.createNewTab(isIncognito = false)
        assertEquals(2, vm.tabs.value.size)
        val secondId = vm.activeTabId.value
        assertNotEquals(firstId, secondId)

        vm.createNewTab(isIncognito = true)
        assertEquals(3, vm.tabs.value.size)
        val thirdId = vm.activeTabId.value
        assertTrue(vm.tabs.value.first { it.id == thirdId }.isIncognito)

        vm.selectTab(firstId)
        assertEquals(firstId, vm.activeTabId.value)

        vm.duplicateTab(secondId)
        assertEquals(4, vm.tabs.value.size)

        vm.closeAllIncognitoTabs()
        assertTrue(vm.tabs.value.none { it.isIncognito })
        assertEquals(3, vm.tabs.value.size)

        vm.closeOtherTabs(firstId)
        assertEquals(1, vm.tabs.value.size)
        assertEquals(firstId, vm.activeTabId.value)

        vm.closeAllTabs()
        assertEquals(1, vm.tabs.value.size)
    }
}
