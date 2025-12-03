package com.h.trendie.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openExternalUrl(url: String) {
    val uri = Uri.parse(url)

    val feedbackId = Regex("""v=([^&]+)""").find(url)?.groupValues?.getOrNull(1)
    val ytIntent = feedbackId?.let { Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$it")) }

    val webIntent = Intent(Intent.ACTION_VIEW, uri)

    listOfNotNull(ytIntent, webIntent).forEach { intent ->
        if (this !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
            return
        }
    }
}