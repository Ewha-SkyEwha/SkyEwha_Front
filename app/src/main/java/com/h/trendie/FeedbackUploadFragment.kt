package com.h.trendie

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.h.trendie.data.UploadResponse
import com.h.trendie.network.ApiClient
import com.h.trendie.util.buildVideoPart
import com.h.trendie.util.getFileName
import com.h.trendie.util.textPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.Locale

class FeedbackUploadFragment : Fragment(R.layout.fragment_feedback_upload) {

    private lateinit var etFeedbackTitle: EditText
    private lateinit var btnUpload: Button
    private var selectedFileUri: Uri? = null

    private val pickVideo = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            Toast.makeText(requireContext(), "영상 선택이 취소되었어요.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        if (!isMp4(requireContext(), uri)) {
            Toast.makeText(requireContext(), "mp4 파일만 업로드할 수 있어요.", Toast.LENGTH_LONG).show()
            return@registerForActivityResult
        }
        selectedFileUri = uri

        val title = etFeedbackTitle.text?.toString()?.trim().orEmpty()
        if (title.isBlank()) {
            Toast.makeText(requireContext(), "제목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val nickname = requireContext()
            .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            .getString("nickname", "유저") ?: "유저"

        showLoading()
        uploadVideoToServer(uri, title, nickname, onFinally = { hideLoading() })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etFeedbackTitle = view.findViewById(R.id.etFeedbackTitle)
        btnUpload = view.findViewById(R.id.btnUpload)

        // ✅ 상단 안내문에 닉네임 끼워넣기
        view.findViewById<TextView>(R.id.tvGuideUpload)?.let { tv ->
            val nickname = requireContext()
                .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                .getString("nickname", "유저") ?: "유저"
            tv.text = "${nickname} 님의 영상을 업로드하고\n" +
                    "TRENDIE가 제공하는 피드백 보고서를\n바로 확인해 보세요!"
        }

        btnUpload.setOnClickListener {
            val title = etFeedbackTitle.text?.toString()?.trim().orEmpty()
            if (title.isBlank()) {
                Toast.makeText(requireContext(), "제목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickVideo.launch("video/*")
        }
    }

    private fun uploadVideoToServer(
        videoUri: Uri,
        title: String,
        nickname: String,
        onFinally: () -> Unit = {}
    ) {
        btnUpload.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val cr = requireContext().contentResolver
                val filePart = withContext(Dispatchers.IO) { buildVideoPart(cr, videoUri) }
                val titlePart = textPart(title)

                // 서버 엔드포인트: /api/v1/feedback/upload (프로젝트에 맞게)
                val resp: Response<UploadResponse> = withContext(Dispatchers.IO) {
                    ApiClient.apiService.uploadVideo(filePart, titlePart)
                }

                if (resp.isSuccessful) {
                    val body = resp.body()
                    Toast.makeText(requireContext(), "업로드 완료!", Toast.LENGTH_SHORT).show()

                    val vid = body?.video_id ?: -1
                    val intent = Intent(requireContext(), FeedbackReportActivity::class.java).apply {
                        putExtra("videoUri", videoUri.toString())
                        putExtra("videoTitle", body?.video_title ?: title)
                        putExtra("nickname", nickname)   // ✅ 보고서에서도 닉네임 보장
                        putExtra("video_id", vid)
                        putExtra("upload_date", body?.upload_date)
                        putExtra("user_id", body?.user_id)
                    }
                    startActivity(intent)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "업로드 실패 (${resp.code()})",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    requireContext(),
                    "네트워크 오류: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                btnUpload.isEnabled = true
                onFinally()
            }
        }
    }

    private fun isMp4(context: Context, uri: Uri): Boolean {
        val ct = context.contentResolver.getType(uri)
        if (ct == "video/mp4") return true
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(ct)?.lowercase()
        if (ext == "mp4") return true
        val name: String = context.contentResolver.getFileName(uri).lowercase(Locale.ROOT)
        return name.endsWith(".mp4")
    }

    // ── 로딩 오버레이
    private val TAG_LOADING = "LoadingFragmentTag"
    private fun showLoading() {
        val fm = parentFragmentManager
        if (fm.findFragmentByTag(TAG_LOADING) != null) return
        fm.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in, android.R.anim.fade_out
            )
            .add(android.R.id.content, LoadingFragment(), TAG_LOADING)
            .commitAllowingStateLoss()
    }
    private fun hideLoading() {
        val fm = parentFragmentManager
        fm.findFragmentByTag(TAG_LOADING)?.let {
            fm.beginTransaction().remove(it).commitAllowingStateLoss()
            fm.executePendingTransactions()
        }
    }
    override fun onDestroyView() {
        hideLoading()
        super.onDestroyView()
    }
}