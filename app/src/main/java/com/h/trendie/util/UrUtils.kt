package com.h.trendie.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.File
import java.io.InputStream

fun ContentResolver.getFileName(uri: Uri): String {
    var name: String? = null
    val c: Cursor? = query(uri, null, null, null, null)
    c?.use {
        val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (idx >= 0 && it.moveToFirst()) name = it.getString(idx)
    }
    return (name ?: "upload.mp4").ifBlank { "upload.mp4" }
}

fun ContentResolver.getFileSize(uri: Uri): Long {
    // 1) Cursor
    query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.SIZE)
        if (idx >= 0 && c.moveToFirst()) {
            val v = c.getLong(idx)
            if (v > 0) return v
        }
    }
    // 2) AssetFileDescriptor
    runCatching {
        openAssetFileDescriptor(uri, "r")?.use { afd ->
            val len = afd.length
            if (len > 0) return len
        }
    }
    // 모르면 -1
    return -1L
}

private class UriRequestBody(
    private val cr: ContentResolver,
    private val uri: Uri,
    private val mime: String,
    private val knownLength: Long
) : RequestBody() {
    override fun contentType() = mime.toMediaType()
    override fun contentLength(): Long = if (knownLength > 0) knownLength else -1
    override fun writeTo(sink: BufferedSink) {
        cr.openInputStream(uri).use { input: InputStream? ->
            requireNotNull(input) { "Cannot open input stream: $uri" }
            val buf = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val r = input.read(buf)
                if (r == -1) break
                sink.write(buf, 0, r)
            }
        }
    }
}

// 공용: 제목 파트
fun textPart(v: String): RequestBody =
    v.toRequestBody("text/plain".toMediaType())

/**
 * 업로드 파일 파트 생성.
 * 1) 크기 알면: content:// 스트리밍 + content-length 지정
 * 2) 크기 모르면: 캐시에 복사 → 실제 파일로 업로드(고정 length)  → ALB 400 회피
 */
fun buildVideoPart(context: Context, uri: Uri): MultipartBody.Part {
    val cr = context.contentResolver
    val fileName = cr.getFileName(uri)
    val mime = cr.getType(uri) ?: "video/mp4"
    val size = cr.getFileSize(uri)

    val body: RequestBody = if (size > 0) {
        UriRequestBody(cr, uri, mime, size)
    } else {
        val suffix = when {
            fileName.endsWith(".mp4", true) -> ".mp4"
            else -> ".bin"
        }
        val tmp = File.createTempFile("trendie_upload_", suffix, context.cacheDir)
        cr.openInputStream(uri).use { input ->
            requireNotNull(input) { "Cannot open input stream: $uri" }
            tmp.outputStream().use { out ->
                val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val r = input.read(buf)
                    if (r == -1) break
                    out.write(buf, 0, r)
                }
                out.flush()
            }
        }
        tmp.asRequestBody(mime.toMediaType())
    }

    return MultipartBody.Part.createFormData("file", fileName, body)
}