package com.h.trendie

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import coil.load
import com.h.trendie.ui.theme.applyTopInsetPadding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context.MODE_PRIVATE
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.h.trendie.data.UserPrefs

class MypageFragment : Fragment(R.layout.fragment_mypage) {

    private val spName = "user_prefs"
    private val keyPhoto = "profile_image_uri"
    private val keyUsePersonDefault = "use_person_icon" // 기본 아이콘 선택 여부 저장

    private var tvName: TextView? = null
    private var tvJoinDate: TextView? = null
    private var ivProfile: ImageView? = null
    private lateinit var prefs: SharedPreferences
    private var installTs: Long = -1L

    // 1) 갤러리/포토피커
    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { startCrop(it) }
    }

    // 2) uCrop 결과 수신
    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val outUri = UCrop.getOutput(result.data!!)
                outUri?.let { cropped ->
                    // 저장
                    prefs.edit().putString(keyPhoto, cropped.toString()).apply()
                    // 화면 반영
                    val fallback = resolveFallbackIcon(prefs)
                    ivProfile?.load(cropped) {
                        crossfade(true)
                        placeholder(fallback)
                        error(fallback)
                    }
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                Log.e("Mypage", "uCrop error", UCrop.getError(result.data!!))
            }
        }

    // uCrop 실행
    private fun startCrop(src: Uri) {
        val dst = Uri.fromFile(File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val intent = UCrop.of(src, dst)
            .withAspectRatio(1f, 1f)      // 프로필: 정사각형
            .withMaxResultSize(600, 600)  // 적당한 해상도 제한
            .getIntent(requireContext())
        cropLauncher.launch(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 상태바 인셋 처리(유틸)
        view.findViewById<View>(R.id.mypageToolbar)?.applyTopInsetPadding()

        // ✅ 멤버에 할당 (지역변수로 새로 만들지 않기!)
        tvName     = view.findViewById(R.id.profileName)
        tvJoinDate = view.findViewById(R.id.joinDate)
        ivProfile  = view.findViewById(R.id.profileImage)

        // 설정 진입
        view.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        prefs = requireContext().getSharedPreferences(spName, MODE_PRIVATE)

        // 가입일 (최초 저장)
        installTs = prefs.getLong("install_date", -1L)
        if (installTs <= 0L) {
            installTs = System.currentTimeMillis()
            prefs.edit().putLong("install_date", installTs).apply()
        }

        // 프로필 이미지 초기 반영
        ivProfile?.let { loadProfileImage(it, prefs) }

        ivProfile?.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        ivProfile?.setOnLongClickListener {
            showProfileOptions(ivProfile!!, prefs)
            true
        }

        // 닉네임 수정
        view.findViewById<ImageView>(R.id.editNickname)?.setOnClickListener {
            startActivity(Intent(requireContext(), NicknameEditActivity::class.java))
        }

        // 회원 정보 관리
        view.findViewById<TextView>(R.id.tvMemberInfo)?.setOnClickListener {
            startActivity(Intent(requireContext(), MemberInfoActivity::class.java))
        }

        // 피드백 보고서 내역
        view.findViewById<TextView>(R.id.tvFeedbackHistory)?.setOnClickListener {
            startActivity(Intent(requireContext(), FeedbackHistoryActivity::class.java))
        }

        // 선호도 조사 내역
        view.findViewById<TextView>(R.id.tvPreferenceHistory)?.setOnClickListener {
            startActivity(Intent(requireContext(), PreferenceHistoryActivity::class.java))
        }

        // ✅ DataStore(닉네임) 실시간 반영 — 처음 진입/처음 설정 직후 모두 즉시 갱신됨
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UserPrefs.nicknameFlow(requireContext()).collect { name ->
                    updateNicknameUI(name)
                }
            }
        }
    }

    private fun updateNicknameUI(name: String) {
        tvName?.text = name
        val dateStr = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date(installTs))
        tvJoinDate?.text = "${name}님과 Trendie는\n${dateStr}부터 함께했어요!"
    }

    /** 현재 설정에 맞는 기본 아이콘 리소스 결정 (ic_person 없으면 자동 폴백) */
    private fun resolveFallbackIcon(prefs: SharedPreferences): Int {
        val personId = requireContext().resources.getIdentifier(
            "ic_person", "drawable", requireContext().packageName
        )
        val usePerson = prefs.getBoolean(keyUsePersonDefault, false) && personId != 0
        return if (usePerson) personId else R.drawable.trendie_logo
    }

    /** 저장된 사진 or 기본 아이콘 로딩 */
    private fun loadProfileImage(img: ImageView, prefs: SharedPreferences) {
        val uriStr = prefs.getString(keyPhoto, null)
        val fallback = resolveFallbackIcon(prefs)

        if (uriStr.isNullOrBlank()) {
            img.setImageResource(fallback)
        } else {
            val uri = Uri.parse(uriStr)
            img.load(uri) {
                crossfade(true)
                placeholder(fallback)
                error(fallback)
                // 필요하면 실제 로딩 크기 줄이기: size(300, 300)
            }
        }
    }

    private fun showProfileOptions(img: ImageView, prefs: SharedPreferences) {
        val hasPerson = requireContext().resources.getIdentifier(
            "ic_person", "drawable", requireContext().packageName
        ) != 0

        val items = if (hasPerson) {
            arrayOf("사진 선택", "기본 아이콘: 로고", "기본 아이콘: 사람", "사진 제거")
        } else {
            arrayOf("사진 선택", "기본 아이콘: 로고", "사진 제거")
        }

        AlertDialog.Builder(requireContext())
            .setItems(items) { _, which ->
                when {
                    which == 0 -> pickPhoto.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                    which == 1 -> {
                        prefs.edit().putBoolean(keyUsePersonDefault, false).apply()
                        prefs.edit().remove(keyPhoto).apply()
                        loadProfileImage(img, prefs)
                    }
                    hasPerson && which == 2 -> {
                        prefs.edit().putBoolean(keyUsePersonDefault, true).apply()
                        prefs.edit().remove(keyPhoto).apply()
                        loadProfileImage(img, prefs)
                    }
                    (!hasPerson && which == 2) || (hasPerson && which == 3) -> {
                        prefs.edit().remove(keyPhoto).apply()
                        loadProfileImage(img, prefs)
                    }
                }
            }
            .show()
    }
}