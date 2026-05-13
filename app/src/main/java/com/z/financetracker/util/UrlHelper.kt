package com.z.financetracker.util

import com.z.financetracker.BuildConfig

object UrlHelper {

    fun normalizeUrl(url: String?): String? {

        if (url == null) return null

        return when (BuildConfig.FLAVOR) {

            "local" ->
                url.replace("localhost", "10.0.2.2")

            else ->
                url
        }
    }
}