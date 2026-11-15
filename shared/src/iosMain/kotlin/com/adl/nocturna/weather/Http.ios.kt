package com.adl.nocturna.weather

import kotlinx.cinterop.BetaInteropApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSURLSession
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.setValue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(BetaInteropApi::class)
internal actual suspend fun httpGet(url: String): String = suspendCancellableCoroutine { cont ->
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl == null) {
        cont.resumeWithException(IllegalArgumentException("Not a URL: $url"))
        return@suspendCancellableCoroutine
    }
    val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
        setTimeoutInterval(10.0)
        setValue("Nocturna-iOS", forHTTPHeaderField = "User-Agent")
    }
    val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
        val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
        when {
            error != null -> cont.resumeWithException(IllegalStateException(error.localizedDescription))
            status !in 200..299 -> cont.resumeWithException(IllegalStateException("HTTP $status"))
            else -> cont.resume(data?.let { NSString.create(data = it, encoding = NSUTF8StringEncoding)?.toString() }.orEmpty())
        }
    }
    cont.invokeOnCancellation { task.cancel() }
    task.resume()
}
