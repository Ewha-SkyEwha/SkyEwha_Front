package com.h.trendie

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.updatePadding
import androidx.core.widget.doOnTextChanged
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.h.trendie.data.ProcessTextKeywordsReq
import com.h.trendie.data.ProcessTextKeywordsRes
import com.h.trendie.network.ApiClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FeedbackTextuploadActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TEXT_TITLE = "extra_text_title"
    }

    private lateinit var tilTitle: TextInputLayout
    private lateinit var etFeedbackTitle: TextInputEditText
    private lateinit var btnUpload: MaterialButton
    private var centerGuide: View? = null
    private var bottomArea: View? = null

    private var incomingTitle: String = ""

    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_textupload)

        tilTitle = findViewById(R.id.tilTitle)
        etFeedbackTitle = findViewById(R.id.etFeedbackTitle)
        btnUpload = findViewById(R.id.btnUpload)
        centerGuide = findViewById(R.id.centerGuide)
        bottomArea = findViewById(R.id.bottomArea)

        incomingTitle = intent.getStringExtra(EXTRA_TEXT_TITLE).orEmpty()

        val root = findViewById<View>(R.id.pageRoot)
        val baseGap = resources.getDimensionPixelSize(R.dimen.page_bottom_gap)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val imeVisible = insets.isVisible(Type.ime())
            val sysBottom = insets.getInsets(Type.navigationBars() or Type.ime()).bottom

            centerGuide?.visibility = if (imeVisible) View.GONE else View.VISIBLE

            val lp = btnUpload.layoutParams
            if (lp is ViewGroup.MarginLayoutParams) {
                val target = baseGap + sysBottom
                if (lp.bottomMargin != target) {
                    lp.bottomMargin = target
                    btnUpload.layoutParams = lp
                }
            } else {
                bottomArea?.updatePadding(bottom = baseGap + sysBottom)
            }
            WindowInsetsCompat.CONSUMED
        }

        updateButtonState("")

        etFeedbackTitle.doOnTextChanged { text, _, _, _ ->
            tilTitle.isErrorEnabled = false
            updateButtonState(text?.toString().orEmpty())
        }

        etFeedbackTitle.setOnEditorActionListener { _, actionId, event ->
            val isDone = actionId == EditorInfo.IME_ACTION_DONE ||
                    (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            if (isDone) { btnUpload.performClick(); true } else false
        }

        btnUpload.setOnClickListener {
            val inputBody = etFeedbackTitle.text?.toString()?.trim().orEmpty()
            if (inputBody.isBlank()) {
                showFieldError("내용을 입력해 주세요")
                return@setOnClickListener
            }
            currentFocus?.hideKeyboard()
            createFeedbackAndGoLoading(inputBody)
        }
    }

    private fun createFeedbackAndGoLoading(inputBody: String) {
        btnUpload.isEnabled = false
        btnUpload.text = "분석 중…"

        scope.launch {
            try {
                val titleForServer = incomingTitle.take(60).ifBlank { "텍스트 피드백" }

                val createRes = ApiClient.apiService.processTextKeywords(
                    ProcessTextKeywordsReq(
                        textTitle = titleForServer,
                        inputText = inputBody
                    )
                )
                if (!createRes.isSuccessful) {
                    val err = runCatching { createRes.errorBody()?.string() }.getOrNull()
                    throw RuntimeException("요청 실패(${createRes.code()})${if (!err.isNullOrBlank()) "\n$err" else ""}")
                }
                val created: ProcessTextKeywordsRes =
                    createRes.body() ?: throw RuntimeException("응답이 비어 있음")
                val feedbackId: Long = created.feedbackId

                startActivity(Intent(this@FeedbackTextuploadActivity, LoadingActivity::class.java).apply {
                    putExtra("feedback_id", feedbackId)
                    putExtra("videoTitle", titleForServer)
                })
                finish()

            } catch (ce: CancellationException) {
                // no-op
            } catch (e: Exception) {
                Toast.makeText(this@FeedbackTextuploadActivity, e.message ?: "요청 실패", Toast.LENGTH_LONG).show()
            } finally {
                btnUpload.isEnabled = true
                btnUpload.text = "자막 업로드"
            }
        }
    }

    private fun showFieldError(message: String) {
        tilTitle.error = message
        tilTitle.isErrorEnabled = true
        etFeedbackTitle.requestFocus()
    }

    private fun updateButtonState(text: String) {
        val enabled = text.isNotBlank()
        btnUpload.isEnabled = enabled
        btnUpload.alpha = if (enabled) 1f else 0.5f
    }

    private fun View.hideKeyboard() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(windowToken, 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
