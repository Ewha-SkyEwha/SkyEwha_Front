package com.h.trendie

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment

class FeedbackUploadFragment : Fragment(R.layout.fragment_feedback_upload) {

    companion object { private const val REQUEST_VIDEO = 1001 }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnUpload = view.findViewById<Button>(R.id.btnUpload)
        val etFeedbackTitle = view.findViewById<EditText>(R.id.etFeedbackTitle)

        btnUpload.setOnClickListener {
            // 제목 필수 체크
            val title = etFeedbackTitle?.text?.toString()?.trim().orEmpty()
            if (title.isBlank()) {
                Toast.makeText(requireContext(), "제목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "video/mp4"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(
                Intent.createChooser(intent, "MP4 영상을 선택하세요"),
                REQUEST_VIDEO
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIDEO && resultCode == Activity.RESULT_OK) {
            val videoUri = data?.data ?: run {
                Toast.makeText(requireContext(), "영상 선택 실패", Toast.LENGTH_SHORT).show()
                return
            }
            val nickname = requireContext()
                .getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
                .getString("nickname", "유저")!!

            val title = view?.findViewById<EditText>(R.id.etFeedbackTitle)
                ?.text?.toString()?.trim().orEmpty()

            // ⬇ 제목은 반드시 넘어감
            val intent = Intent(requireContext(), FeedbackReportActivity::class.java).apply {
                putExtra("videoUri", videoUri.toString())
                putExtra("videoTitle", title)
                putExtra("nickname", nickname)
            }
            startActivity(intent)
        }
    }
}