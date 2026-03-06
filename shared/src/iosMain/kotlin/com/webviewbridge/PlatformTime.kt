package com.webviewbridge
import platform.Foundation.NSDate
internal actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSinceReferenceDate * 1000).toLong() + 978307200000L
