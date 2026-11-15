package com.adl.nocturna.weather

/** The body of a GET of [url], off the main thread; throws on a network error or a status other than 2xx. */
internal expect suspend fun httpGet(url: String): String
