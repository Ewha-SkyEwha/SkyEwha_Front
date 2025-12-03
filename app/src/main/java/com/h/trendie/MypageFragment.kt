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
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.h.trendie.BookmarkActivity
import kotlinx.coroutines.launch

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class MypageFragment : Fragment(R.layout.fragment_mypage) {

    private lateinit var appPrefs: SharedPreferences

    private lateinit var userPrefs: SharedPreferences

    private val keyPhoto = "profile_image_uri"
    private val keyUsePersonDefault = "use_person_icon"
    private val keyInstallDate = "install_date"

    private var tvName: TextView? = null
    private var tvJoinDate: TextView? = null
    private var ivProfile: ImageView? = null
    private var installTs: Long = -1L

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { startCrop(it) }
    }

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val outUri = UCrop.getOutput(result.data!!)
                outUri?.let { cropped ->
                    appPrefs.edit().putString(keyPhoto, cropped.toString()).apply()
                    val fallback = resolveFallbackIcon()
                    ivProfile?.load(cropped) {
                        crossfade(true)
                        placeholder(fallback)
                        error(fallback)
                    }
                }
            } else if (result.resultCode == UCrop.RESULT_ERROR) {
                val err = runCatching { UCrop.getError(result.data ?: return@registerForActivityResult) }.getOrNull()
                Log.e("Mypage", "uCrop error", err)
                Toast.makeText(requireContext(), "이미지 편집 중 오류가 발생했어요.", Toast.LENGTH_SHORT).show()
            }
        }

    private val editNicknameLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val changed = result.data!!.getStringExtra("nickname")
                if (!changed.isNullOrBlank()) {
                    updateNicknameUI(changed)
                }
            }
        }

    private fun startCrop(src: Uri) {
        val dst = Uri.fromFile(File(requireContext().cacheDir, "cropped_${System.currentTimeMillis()}.jpg"))
        val intent = UCrop.of(src, dst)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(600, 600)
            .getIntent(requireContext())
        cropLauncher.launch(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.mypageToolbar)?.applyTopInsetPadding()

        tvName     = view.findViewById(R.id.profileName)
        tvJoinDate = view.findViewById(R.id.joinDate)
        ivProfile  = view.findViewById(R.id.profileImage)

        view.findViewById<View>(R.id.btnSettings)?.setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }

        appPrefs  = requireContext().getSharedPreferences(ApiConfig.PREFS_APP, MODE_PRIVATE)

        val masterKey = MasterKey.Builder(requireContext())
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        userPrefs = EncryptedSharedPreferences.create(
            requireContext(),
            ApiConfig.PREFS_USER,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        installTs = appPrefs.getLong(keyInstallDate, -1L)
        if (installTs <= 0L) {
            installTs = System.currentTimeMillis()
            appPrefs.edit().putLong(keyInstallDate, installTs).apply()
        }

        ivProfile?.let { loadProfileImage(it) }
        ivProfile?.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        ivProfile?.setOnLongClickListener {
            ivProfile?.let { img -> showProfileOptions(img) }
            true
        }

        view.findViewById<ImageView>(R.id.editNickname)?.setOnClickListener {
            val userId = userPrefs.getLong(ApiConfig.KEY_USER_ID, -1L)

            if (userId <= 0L) {
                AlertDialog.Builder(requireContext())
                    .setMessage("로그인이 필요합니다.")
                    .setPositiveButton("확인", null)
                    .show()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), NicknameEditActivity::class.java)
                .putExtra("user_id", userId)
            editNicknameLauncher.launch(intent)
        }

        view.findViewById<TextView>(R.id.tvMemberInfo)?.setOnClickListener {
            startActivity(Intent(requireContext(), MemberInfoActivity::class.java))
        }

        view.findViewById<TextView>(R.id.tvFeedbackHistory)?.setOnClickListener {
            startActivity(Intent(requireContext(), FeedbackHistoryActivity::class.java))
        }

        view.findViewById<TextView>(R.id.tvBookmark)?.setOnClickListener {
            startActivity(Intent(requireContext(), BookmarkActivity::class.java))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                UserPrefs.nicknameFlow(requireContext()).collect { name ->
                    updateNicknameUI(name)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val cached = UserPrefs.getNickname(requireContext())
            if (!cached.isNullOrBlank()) updateNicknameUI(cached)
        }
    }

    private fun updateNicknameUI(name: String) {
        val safe = name.ifBlank { "사용자" }
        tvName?.text = safe
        val dateStr = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date(installTs))
        tvJoinDate?.text = "${safe}님과 Trendie는\n${dateStr}부터 함께했어요!"
    }

    private fun resolveFallbackIcon(): Int {
        val personId = requireContext().resources.getIdentifier(
            "ic_person", "drawable", requireContext().packageName
        )
        val usePerson = appPrefs.getBoolean(keyUsePersonDefault, false) && personId != 0
        return if (usePerson) personId else R.drawable.trendie_logo
    }

    private fun loadProfileImage(img: ImageView) {
        val uriStr = appPrefs.getString(keyPhoto, null)
        val fallback = resolveFallbackIcon()

        if (uriStr.isNullOrBlank()) {
            img.setImageResource(fallback)
        } else {
            val uri = Uri.parse(uriStr)
            img.load(uri) {
                crossfade(true)
                placeholder(fallback)
                error(fallback)
                // size(300, 300) // 필요시
            }
        }
    }

    private fun showProfileOptions(img: ImageView) {
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
                        appPrefs.edit().putBoolean(keyUsePersonDefault, false).apply()
                        appPrefs.edit().remove(keyPhoto).apply()
                        loadProfileImage(img)
                    }
                    hasPerson && which == 2 -> {
                        appPrefs.edit().putBoolean(keyUsePersonDefault, true).apply()
                        appPrefs.edit().remove(keyPhoto).apply()
                        loadProfileImage(img)
                    }
                    (!hasPerson && which == 2) || (hasPerson && which == 3) -> {
                        appPrefs.edit().remove(keyPhoto).apply()
                        loadProfileImage(img)
                    }
                }
            }
            .show()
    }
}
