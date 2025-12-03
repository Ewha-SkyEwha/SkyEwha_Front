package com.h.trendie

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import com.h.trendie.auth.TokenProvider
import com.h.trendie.data.ProcessVideoKeywordsReq
import com.h.trendie.network.ApiClient
import com.h.trendie.ui.sheet.ChooseInputMethodBottomSheet
import com.h.trendie.ui.theme.applyTopInsetPadding
import com.h.trendie.util.getFileName
import com.h.trendie.util.getFileSize
import com.h.trendie.util.textPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.source
import retrofit2.Response
import java.io.File
import java.util.Locale

class FeedbackUploadFragment : Fragment(R.layout.fragment_feedback_upload) {

    private lateinit var etFeedbackTitle: EditText
    private lateinit var btnUpload: View
    private lateinit var centerGuide: View
    private var selectedFileUri: Uri? = null

    private val TAG = "FeedbackUpload"

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            toast("영상 선택이 취소되었어요.")
            return@registerForActivityResult
        }
        if (!isMp4(requireContext(), uri)) {
            toast("mp4 파일만 업로드할 수 있어요.")
            return@registerForActivityResult
        }

        val title = etFeedbackTitle.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            toast("제목을 입력해 주세요.")
            return@registerForActivityResult
        }

        val nickname = com.h.trendie.util.NicknameStore.get(requireContext())

        Log.d(TAG, "PickVideo: uri=$uri, title='$title', nickname='$nickname'")
        selectedFileUri = uri
        uploadThenGoLoading(uri, title, nickname)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etFeedbackTitle = view.findViewById(R.id.etFeedbackTitle)
        btnUpload       = view.findViewById(R.id.btnUpload)
        centerGuide     = view.findViewById(R.id.centerGuide)

        view.findViewById<View>(R.id.headerContainer)?.applyTopInsetPadding()

        val baseBtnMargin = (btnUpload.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val imeVisible = insets.isVisible(Type.ime())
            val imeBottom  = insets.getInsets(Type.ime()).bottom
            (btnUpload.layoutParams as? ViewGroup.MarginLayoutParams)?.let { lp ->
                val newMargin = if (imeVisible) baseBtnMargin + imeBottom else baseBtnMargin
                if (lp.bottomMargin != newMargin) {
                    lp.bottomMargin = newMargin
                    btnUpload.layoutParams = lp
                }
            }
            centerGuide.visibility = if (imeVisible) View.GONE else View.VISIBLE
            insets
        }

        view.findViewById<TextView>(R.id.tvGuideUpload)?.text =
            "제목을 입력하고\nTRENDIE가 제공하는 피드백 보고서를\n확인해 보세요!"

        setFragmentResultListener(REQ_CHOOSE_METHOD) { _, result ->
            when (result.getString(KEY_CHOICE)) {
                CHOICE_VIDEO -> pickVideo.launch("video/mp4")
                CHOICE_TEXT  -> {
                    val textTitle = etFeedbackTitle.text?.toString()?.trim().orEmpty()
                    if (textTitle.isBlank()) {
                        toast("제목을 입력해 주세요.")
                        return@setFragmentResultListener
                    }
                    startActivity(Intent(requireContext(), FeedbackTextuploadActivity::class.java).apply {
                        putExtra(FeedbackTextuploadActivity.EXTRA_TEXT_TITLE, textTitle)
                    })
                }
            }
        }

        btnUpload.setOnClickListener {
            val title = etFeedbackTitle.text?.toString()?.trim().orEmpty()
            if (title.isBlank()) {
                toast("제목을 입력해 주세요.")
                return@setOnClickListener
            }
            ViewCompat.getWindowInsetsController(requireView())?.hide(Type.ime())
            ChooseInputMethodBottomSheet.newInstance()
                .show(parentFragmentManager, "ChooseInputMethod")
        }
    }

    private fun uploadThenGoLoading(videoUri: Uri, title: String, nickname: String) {
        btnUpload.isEnabled = false
        showLoading()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                runCatching { Log.d(TAG, "Token preview='${TokenProvider.preview(requireContext())}'") }

                val filePart = withContext(Dispatchers.IO) { buildVideoPartStable(videoUri) }
                val titlePart: RequestBody = textPart(title)

                Log.d(TAG, "Upload: start POST /api/v1/video/upload_video/")
                val up = ApiClient.apiService.uploadVideo(filePart, titlePart)
                Log.d(TAG, "Upload: response code=${up.code()} isSuccessful=${up.isSuccessful}")
                if (!up.isSuccessful) {
                    logErrorResponse("UploadVideo", up)
                    toast("영상 업로드 실패! 다시 로그인해 주세요.")
                    return@launch
                }
                val body = up.body()

                val videoIdLong: Long = when (val id = body?.videoId) {
                    is Long -> id
                    is Int -> id.toLong()
                    is String -> id.toLongOrNull()
                        ?: run { toast("video_id가 숫자가 아니에요: $id"); return@launch }
                    is Number -> id.toLong()
                    else -> { toast("video_id가 응답에 없어요."); return@launch }
                }

                Log.d(TAG, "ProcessKeywords: POST /api/v1/keyword/process_video_keywords/ (video_id=$videoIdLong)")
                val proc = runCatching {
                    ApiClient.apiService.processVideoKeywords(
                        ProcessVideoKeywordsReq(videoId = videoIdLong)
                    )
                }.getOrNull()

                var feedbackIdLong: Long? = null
                if (proc != null && proc.isSuccessful) {
                    feedbackIdLong = proc.body()?.feedbackId.toLongLenient()
                    Log.d(TAG, "ProcessKeywords OK: feedback_id=$feedbackIdLong")
                } else {
                    Log.w(TAG, "ProcessKeywords not OK: code=${proc?.code()} body=${proc?.errorBody()?.string()}")
                }

                val finalTitle = body?.videoTitle ?: title
                Log.d(TAG, "Navigate -> LoadingActivity(video_id=$videoIdLong, title='$finalTitle', nickname='$nickname', feedback_id=$feedbackIdLong)")
                startActivity(Intent(requireContext(), LoadingActivity::class.java).apply {
                    putExtra("video_id", videoIdLong)
                    putExtra("videoTitle", finalTitle)
                    putExtra("nickname", nickname)
                    feedbackIdLong?.let { putExtra("feedback_id", it) }
                })
            } catch (e: Exception) {
                Log.e(TAG, "❌ uploadThenGoLoading error: ${e.message}", e)
                toast("네트워크 오류: ${e.localizedMessage}")
            } finally {
                hideLoading()
                btnUpload.isEnabled = true
            }
        }
    }

    // ---- 유틸 ----

    private fun Any?.toLongLenient(): Long? = when (this) {
        null -> null
        is Long -> this
        is Int -> this.toLong()
        is Number -> this.toLong()
        is String -> this.trim().toLongOrNull()
        is CharSequence -> this.toString().trim().toLongOrNull()
        else -> null
    }

    private fun logErrorResponse(tag: String, resp: Response<*>) {
        val errText = runCatching { resp.errorBody()?.string() }.getOrNull()
        val headersStr = buildString {
            try { resp.headers().forEach { h -> appendLine("${h.first}: ${h.second}") } } catch (_: Throwable) {}
        }
        Log.e(tag, """
            ⛔ 요청 실패
            code = ${resp.code()}
            — Response headers —
            ${headersStr.ifBlank { "<none>" }}
            — Error body —
            ${errText ?: "<null>"}
        """.trimIndent())
    }

    private suspend fun buildVideoPartStable(uri: Uri): MultipartBody.Part =
        withContext(Dispatchers.IO) {
            val cr = requireContext().contentResolver
            val fileName = cr.getFileName(uri)
            val mime = cr.getType(uri) ?: "video/mp4"
            val size = cr.getFileSize(uri).takeIf { it > 0 }

            if (size != null) {
                val body = object : okhttp3.RequestBody() {
                    override fun contentType() = mime.toMediaType()
                    override fun contentLength() = size
                    override fun writeTo(sink: okio.BufferedSink) {
                        cr.openInputStream(uri)?.use { input ->
                            sink.writeAll(input.source())
                        } ?: error("Cannot open input stream: $uri")
                    }
                }
                return@withContext MultipartBody.Part.createFormData("file", fileName, body)
            }

            val tmp = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}_$fileName")
            cr.openInputStream(uri)?.use { input ->
                tmp.outputStream().buffered().use { out -> input.copyTo(out) }
            } ?: error("Cannot open input stream: $uri")

            val body = tmp.asRequestBody(mime.toMediaType())
            MultipartBody.Part.createFormData("file", fileName, body)
        }

    private fun isMp4(context: Context, uri: Uri): Boolean {
        val ct = context.contentResolver.getType(uri)
        if (ct == "video/mp4") return true
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(ct)?.lowercase()
        if (ext == "mp4") return true
        val name: String = context.contentResolver.getFileName(uri).lowercase(Locale.ROOT)
        return name.endsWith(".mp4")
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private val TAG_LOADING = "LoadingDialog"
    private fun showLoading() {
        if (parentFragmentManager.findFragmentByTag(TAG_LOADING) == null) {
            LoadingDialogFragment().show(parentFragmentManager, TAG_LOADING)
        }
    }
    private fun hideLoading() {
        (parentFragmentManager.findFragmentByTag(TAG_LOADING) as? LoadingDialogFragment)
            ?.dismissAllowingStateLoss()
    }

    override fun onDestroyView() {
        hideLoading()
        super.onDestroyView()
    }

    companion object {
        const val REQ_CHOOSE_METHOD = "req_choose_method"
        const val KEY_CHOICE = "key_choice"
        const val CHOICE_VIDEO = "video"
        const val CHOICE_TEXT = "text"
    }
}
