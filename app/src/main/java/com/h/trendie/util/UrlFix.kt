package com.h.trendie.util

fun String.ensureHttps(): String =
    if (startsWith("http://", ignoreCase = true)) "https://${substring(7)}" else this
