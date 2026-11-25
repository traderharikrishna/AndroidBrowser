package com.example.proxy

object TrackerBlocklist {
    private val blockedDomainSuffixes = setOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleadservices.com",
        "google-analytics.com",
        "googletagmanager.com",
        "googletagservices.com",
        "adservice.google.com",
        "facebook.net",
        "connect.facebook.net",
        "pixel.facebook.com",
        "analytics.twitter.com",
        "ads-twitter.com",
        "ads.yahoo.com",
        "adnxs.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "criteo.com",
        "criteo.net",
        "outbrain.com",
        "taboola.com",
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "mixpanel.com",
        "segment.io",
        "segment.com",
        "amplitude.com",
        "appsflyer.com",
        "adjust.com",
        "branch.io",
        "moatads.com",
        "amazon-adsystem.com",
        "clarity.ms",
        "omtrdc.net",
        "demdex.net",
        "krxd.net",
        "chartbeat.com",
        "newrelic.com",
        "nr-data.net",
        "sentry.io",
        "bugsnag.com",
        "adcolony.com",
        "unityads.unity3d.com",
        "applovin.com"
    )

    fun isBlocked(host: String): Boolean {
        val cleanHost = host.trim().lowercase().substringBefore(':')
        if (cleanHost.isEmpty()) return false
        var candidate = cleanHost
        while (candidate.isNotEmpty()) {
            if (blockedDomainSuffixes.contains(candidate)) return true
            val dotIndex = candidate.indexOf('.')
            if (dotIndex < 0) break
            candidate = candidate.substring(dotIndex + 1)
        }
        return false
    }
}
