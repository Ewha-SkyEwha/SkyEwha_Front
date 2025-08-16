package com.h.trendie

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.h.trendie.ui.theme.applyTopInsetPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MypageFragment : Fragment(R.layout.fragment_mypage) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 상태바 인셋 처리(유틸 없으면 ?.로 안전)
        view.findViewById<View>(R.id.mypageToolbar)?.applyTopInsetPadding()

        // 설정 진입
        view.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        val ctx = requireContext()
        val prefs = ctx.getSharedPreferences("user_prefs", MODE_PRIVATE)

        // 닉네임
        val nickname = prefs.getString("nickname", "유저") ?: "유저"
        view.findViewById<TextView>(R.id.profileName)?.text = nickname

        // 가입일(없으면 이번에 저장해둠)
        var installTs = prefs.getLong("install_date", -1L)
        if (installTs <= 0L) {
            installTs = System.currentTimeMillis()
            prefs.edit().putLong("install_date", installTs).apply()
        }
        val dateStr = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA)
            .format(Date(installTs))
        view.findViewById<TextView>(R.id.joinDate)?.text =
            "${nickname}님과 Trendie는\n${dateStr}부터 함께했어요!"

        // 닉네임 수정
        view.findViewById<ImageView>(R.id.editNickname)?.setOnClickListener {
            startActivity(Intent(ctx, NicknameEditActivity::class.java))
        }

        // 회원 정보 관리
        view.findViewById<TextView>(R.id.tvMemberInfo)?.setOnClickListener {
            startActivity(Intent(ctx, MemberInfoActivity::class.java))
        }

        // 피드백 보고서 내역
        view.findViewById<TextView>(R.id.tvFeedbackHistory)?.setOnClickListener {
            startActivity(Intent(ctx, FeedbackHistoryActivity::class.java))
        }

        // 선호도 조사 내역
        view.findViewById<TextView>(R.id.tvPreferenceHistory)?.setOnClickListener {
            startActivity(Intent(ctx, PreferenceHistoryActivity::class.java))
        }
    }
}