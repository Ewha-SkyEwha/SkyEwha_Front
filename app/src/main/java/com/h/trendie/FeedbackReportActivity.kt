package com.h.trendie

import android.content.ContentValues
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.h.trendie.data.AppDatabase
import com.h.trendie.data.ProcessKeywordsRequest
import com.h.trendie.data.ProcessKeywordsResponse
import com.h.trendie.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FeedbackReportActivity : AppCompatActivity() {

    private lateinit var titleFromUser: String
    private lateinit var nickname: String

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_report)

        // 인텐트
        titleFromUser = intent.getStringExtra("videoTitle").orEmpty()
        nickname = intent.getStringExtra("nickname").orEmpty()

        findViewById<TextView>(R.id.tvFeedbackGreeting)?.text =
            "${nickname} 님의 <${titleFromUser}>에 대한\n피드백 보고서가 도착했습니다📨"

        val report = findViewById<View>(R.id.reportContent)
        val btnSavePdf = findViewById<Button>(R.id.btnSavePdf)
        val btnSaveHistory = findViewById<Button>(R.id.btnSaveHistory)
        val progressOverlay = findViewById<View>(R.id.progressOverlay)

        val toolbar = findViewById<View>(R.id.fbToolbar)
        val footerActions = findViewById<View>(R.id.footerActions)
        val tvTitle = toolbar?.findViewById<TextView>(R.id.tvTitle)
        val btnBack = toolbar?.findViewById<View>(R.id.btnBack)

        // 해시태그 리스트
        val rvTags = findViewById<RecyclerView>(R.id.rvTags)
        val tagAdapter = SimpleTagAdapter()
        rvTags?.layoutManager = LinearLayoutManager(this)
        rvTags?.adapter = tagAdapter

        // WindowInsets
        if (toolbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
                val top = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
                ).top
                v.updatePadding(top = top)
                insets
            }
        }
        if (footerActions != null) {
            ViewCompat.setOnApplyWindowInsetsListener(footerActions) { v, insets ->
                val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                v.updatePadding(bottom = bottom)
                insets
            }
        }

        val hideToolbar = intent.getBooleanExtra("hideToolbar", false)
        if (hideToolbar) {
            tvTitle?.visibility = View.GONE
            footerActions?.visibility = View.GONE
            btnBack?.setOnClickListener { finish() }
        } else {
            btnBack?.setOnClickListener { finish() }

            btnSavePdf?.setOnClickListener {
                val safeTitle = sanitizeFileName(titleFromUser.ifBlank { "피드백보고서" })
                val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
                val fileName = "Trendie-${safeTitle}-${timeTag}.pdf"

                lifecycleScope.launch {
                    try {
                        progressOverlay?.isVisible = true
                        val file = withContext(Dispatchers.IO) { exportViewToPdf(report, fileName) }
                        Toast.makeText(
                            this@FeedbackReportActivity,
                            "PDF 저장 완료:\n${file.absolutePath}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@FeedbackReportActivity,
                            "PDF 저장 실패: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        progressOverlay?.isVisible = false
                    }
                }
            }

            btnSaveHistory?.setOnClickListener { btn ->
                btn.isEnabled = false
                lifecycleScope.launch {
                    try {
                        val db = AppDatabase.getInstance(this@FeedbackReportActivity)
                        val t = titleFromUser.ifBlank { "피드백 보고서" }
                        val d = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA).format(Date())
                        db.feedbackHistoryDao().insertSimple(t, d)
                        Toast.makeText(
                            this@FeedbackReportActivity,
                            "내역에 저장되었습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@FeedbackReportActivity,
                            "내역 저장 실패: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        btn.isEnabled = true
                    }
                }
            }
        }

        // 업로드에서 전달된 video_id
        val videoIdFromInt = intent.getIntExtra("video_id", -1)
        val videoIdFromStr = intent.getStringExtra("video_id")?.toIntOrNull() ?: -1
        val videoId = if (videoIdFromInt > 0) videoIdFromInt else videoIdFromStr

        // 자동 요청 흐름
        if (videoId > 0) {
            runKeywordAndHashtagFlow(
                videoId = videoId,
                onTags = { tags -> tagAdapter.submit(tags) },
                progress = progressOverlay
            )
        } else {
            tagAdapter.submit(listOf("영상 ID가 없어 추천 해시태그를 표시할 수 없어요."))
        }
    }

    private fun runKeywordAndHashtagFlow(
        videoId: Int,
        onTags: (List<String>) -> Unit,
        progress: View?
    ) {
        lifecycleScope.launch {
            try {
                progress?.isVisible = true

                // 1) 키워드 처리
                val processResp: Response<ProcessKeywordsResponse> = withContext(Dispatchers.IO) {
                    ApiClient.apiService.processKeywords(ProcessKeywordsRequest(videoId))
                }
                if (!processResp.isSuccessful) {
                    Toast.makeText(
                        this@FeedbackReportActivity,
                        "키워드 처리 실패(${processResp.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // 2) 해시태그 추천
                val tagsResp: Response<List<String>> = withContext(Dispatchers.IO) {
                    ApiClient.apiService.recommendHashtags(videoId)
                }
                if (!tagsResp.isSuccessful) {
                    val msg = if (tagsResp.code() == 404)
                        "Video not found (404)"
                    else
                        "해시태그 불러오기 실패(${tagsResp.code()})"
                    Toast.makeText(this@FeedbackReportActivity, msg, Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val tags: List<String> = tagsResp.body().orEmpty()
                onTags(if (tags.isEmpty()) listOf("추천 해시태그가 아직 없어요.") else tags)

            } catch (e: Exception) {
                Toast.makeText(
                    this@FeedbackReportActivity,
                    "요청 오류: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                progress?.isVisible = false
            }
        }
    }

    private fun exportViewToPdf(target: View, fileName: String): File {
        val width = target.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val specW = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        target.measure(specW, specH)
        target.layout(0, 0, target.measuredWidth, target.measuredHeight)

        val doc = PdfDocument()
        val pageInfo =
            PdfDocument.PageInfo.Builder(target.measuredWidth, target.measuredHeight, 1).create()
        val page = doc.startPage(pageInfo)
        target.draw(page.canvas)
        doc.finishPage(page)

        val outFile: File
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("MediaStore에 파일 생성 실패")
            resolver.openOutputStream(uri).use { out ->
                if (out == null) throw Exception("OutputStream 열기 실패")
                doc.writeTo(out)
            }
            outFile =
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            outFile = File(dir, fileName)
            FileOutputStream(outFile).use { doc.writeTo(it) }
        }

        doc.close()
        return outFile
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|\r\n]+"""), "_").trim('_', ' ')

    // 간단 태그 어댑터
    private inner class SimpleTagAdapter : RecyclerView.Adapter<SimpleTagAdapter.VH>() {
        private val items = mutableListOf<String>()
        fun submit(list: List<String>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tv: TextView = view as TextView
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val tv = TextView(parent.context).apply {
                setPadding(24, 16, 24, 16); textSize = 15f
            }
            return VH(tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.tv.text = items[position]
        }

        override fun getItemCount(): Int = items.size
    }
}