package com.h.trendie.util

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.buffer
import okio.source

fun Context.uriToPart(
    uri: Uri,
    partName: String = "file",
    fileName: String? = null
): MultipartBody.Part {
    val cr = contentResolver
    val mime = cr.getType(uri)
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            MimeTypeMap.getFileExtensionFromUrl(uri.toString())
        ) ?: "application/octet-stream"

    val requestBody = object : RequestBody() {
        override fun contentType() = mime.toMediaTypeOrNull()
        override fun writeTo(sink: okio.BufferedSink) {
            cr.openInputStream(uri)?.use { input ->
                input.source().buffer().use { src -> sink.writeAll(src) }
            }
        }
    }

    val name = fileName ?: "upload_${System.currentTimeMillis()}"
    return MultipartBody.Part.createFormData(partName, name, requestBody)
}

fun String.asTextBody(): RequestBody =
    RequestBody.create("text/plain".toMediaTypeOrNull(), this)
