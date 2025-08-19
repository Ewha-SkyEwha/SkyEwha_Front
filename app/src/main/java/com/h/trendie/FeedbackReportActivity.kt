package com.h.trendie

import android.content.ContentValues
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.h.trendie.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class FeedbackReportActivity : AppCompatActivity() {

    private lateinit var titleFromUser: String
    private lateinit var nickname: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_report)

        // 인텐트 데이터 수신
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
        val tvTitle = toolbar.findViewById<TextView>(R.id.tvTitle)
        val btnBack = toolbar.findViewById<View>(R.id.btnBack)

        // 👇 내역에서 열었는지 여부 체크
        val hideToolbar = intent.getBooleanExtra("hideToolbar", false)

        if (hideToolbar) {
            // 내역에서 열면: 뒤로가기만 남기고 나머지 숨김
            tvTitle?.visibility = View.GONE
            footerActions?.visibility = View.GONE
            btnBack?.setOnClickListener { finish() }
        } else {
            // 원래 로직 (업로드 직후 열 때)
            btnBack?.setOnClickListener { finish() }

            // ============================
            // PDF 저장 버튼 로직
            // ============================
            btnSavePdf.setOnClickListener {
                val safeTitle = sanitizeFileName(titleFromUser.ifBlank { "피드백보고서" })
                val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
                val fileName = "Trendie-${safeTitle}-${timeTag}.pdf"

                lifecycleScope.launch {
                    try {
                        progressOverlay.isVisible = true
                        val file = withContext(Dispatchers.IO) {
                            exportViewToPdf(report, fileName)
                        }
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
                        progressOverlay.isVisible = false
                    }
                }
            }

            // ============================
            // 내역 저장 버튼 로직
            // ============================
            btnSaveHistory.setOnClickListener {
                it.isEnabled = false
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
                        it.isEnabled = true
                    }
                }
            }
        }
    }

    // 📌 뷰를 PDF로 변환 후 Downloads에 저장
    private fun exportViewToPdf(target: View, fileName: String): File {
        val width = target.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val specW = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        target.measure(specW, specH)
        target.layout(0, 0, target.measuredWidth, target.measuredHeight)

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(
            target.measuredWidth,
            target.measuredHeight,
            1
        ).create()
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
            outFile = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                fileName
            )
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            outFile = File(dir, fileName)
            FileOutputStream(outFile).use { doc.writeTo(it) }
        }

        doc.close()
        return outFile
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|\r\n]+"""), "_").trim('_', ' ')
    }
}