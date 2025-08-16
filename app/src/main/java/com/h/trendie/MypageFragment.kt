package com.h.trendie

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.h.trendie.FeedbackHistoryActivity
import com.h.trendie.ui.theme.applyTopInsetPadding

class MypageFragment : Fragment(R.layout.fragment_mypage) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 상태바 인셋만큼 내림
        view.findViewById<View>(R.id.mypageToolbar)?.applyTopInsetPadding()

        // 설정
        view.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        val prefs = requireContext().getSharedPreferences("user_prefs", MODE_PRIVATE)
        val storedName = prefs.getString("nickname", "유저")
        view.findViewById<TextView>(R.id.profileName).text = storedName

        val installTs = prefs.getLong("install_date", System.currentTimeMillis())
        val sdf = java.text.SimpleDateFormat("yyyy년 MM월 dd일", java.util.Locale.KOREA)
        val dateStr = sdf.format(java.util.Date(installTs))
        view.findViewById<TextView>(R.id.joinDate).text =
            "${storedName}님과 Trendie는\n${dateStr}부터 함께했어요!"

        // 닉네임 수정
        view.findViewById<ImageView>(R.id.editNickname).setOnClickListener {
            startActivity(Intent(requireContext(), NicknameEditActivity::class.java))
        }

        // 회원 정보 관리
        view.findViewById<TextView>(R.id.tvMemberInfo).setOnClickListener {
            startActivity(Intent(requireContext(), MemberInfoActivity::class.java))
        }
        // 피드백 보고서 내역
        view.findViewById<TextView>(R.id.tvFeedbackHistory).setOnClickListener {
            startActivity(Intent(requireContext(), FeedbackHistoryActivity::class.java))
        }
        // 선호도 조사 내역
        view.findViewById<TextView>(R.id.tvPreferenceHistory).setOnClickListener {
            startActivity(Intent(requireContext(), PreferenceHistoryActivity::class.java))
        }
    }
}