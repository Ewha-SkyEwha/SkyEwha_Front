package com.h.trendie

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.request.CachePolicy
import coil.size.Precision
import coil.size.Scale
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import com.h.trendie.data.AppDatabase
import com.h.trendie.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExtraKeys {
    const val FEEDBACK_ID = "feedback_id"
    const val VIDEO_TITLE = "video_title"
    const val NICKNAME = "nickname"
    const val TITLES = "titles_fixed"
    const val HASHTAGS = "hashtags_ready"
    const val THUMBS = "thumbs_ready"
}

class FeedbackReportActivity : AppCompatActivity() {

    private lateinit var titleFromUser: String
    private var nickname: String = "유저"

    private lateinit var tagAdapter: HashtagGridAdapter
    private lateinit var similarAdapter: SimilarVideoThumbAdapter

    private var rvTags: RecyclerView? = null
    private var rvSimilar: RecyclerView? = null

    private var listTitles: LinearLayout? = null
    private var tvGreeting: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback_report)

        titleFromUser = intent.getStringOrNull(ExtraKeys.VIDEO_TITLE)
            ?: intent.getStringOrNull("videoTitle") ?: ""

        val nickFromIntent = intent.getStringOrNull(ExtraKeys.NICKNAME)
        nickname = if (nickFromIntent.isNullOrBlank()) {
            com.h.trendie.util.NicknameStore.get(this)
        } else {
            nickFromIntent
        }

        tvGreeting = findViewById(R.id.tvFeedbackGreeting)
        applyGreeting()

        val report = findViewById<View>(R.id.reportContent)
        val btnSavePdf = findViewById<Button>(R.id.btnSavePdf)
        val btnSaveHistory = findViewById<Button>(R.id.btnSaveHistory)
        val progressOverlay = findViewById<View>(R.id.progressOverlay)?.apply { isVisible = false }

        val toolbar = findViewById<MaterialToolbar>(R.id.fbToolbar)
        toolbar?.apply {
            menu.clear()
            title = "피드백 보고서"
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

            ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                val top = insets.getInsets(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
                ).top
                v.updatePadding(top = top)
                insets
            }
        }

        val footerActions = findViewById<View>(R.id.footerActions)
        if (footerActions != null) {
            ViewCompat.setOnApplyWindowInsetsListener(footerActions) { v, insets ->
                val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                v.updatePadding(bottom = bottom)
                insets
            }
        }

        val hideToolbar = intent.getBooleanExtra("hideToolbar", false)
        if (hideToolbar) {
            toolbar?.visibility = View.GONE
            footerActions?.visibility = View.GONE
        }

        btnSavePdf?.setOnClickListener {
            val safeTitle = sanitizeFileName(titleFromUser.ifBlank { "피드백보고서" })
            val timeTag = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
            val fileName = "Trendie-${safeTitle}-${timeTag}.pdf"

            val target = report ?: run {
                Toast.makeText(this, "PDF 대상 레이아웃을 찾지 못했어요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    progressOverlay?.isVisible = true
                    val file = withContext(Dispatchers.IO) { exportViewToPdf(target, fileName) }
                    Toast.makeText(
                        this@FeedbackReportActivity,
                        "PDF가 다운로드 폴더에 저장되었어요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("Feedback", "PDF saved: ${file.absolutePath}")
                } catch (e: Exception) {
                    Toast.makeText(this@FeedbackReportActivity, "PDF 저장 실패", Toast.LENGTH_SHORT)
                        .show()
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
                    Toast.makeText(this@FeedbackReportActivity, "내역에 저장되었습니다.", Toast.LENGTH_SHORT)
                        .show()
                } catch (e: Exception) {
                    Toast.makeText(this@FeedbackReportActivity, "내역 저장 실패", Toast.LENGTH_SHORT)
                        .show()
                } finally {
                    btn.isEnabled = true
                }
            }
        }

        listTitles = findViewById(R.id.listTitles)
        val tvTitlesEmpty = findViewById<TextView>(R.id.tvTitlesEmpty)

        tagAdapter = HashtagGridAdapter { tagNoHash -> openYouTubeSearch(tagNoHash) }
        rvTags = findViewById<RecyclerView>(R.id.rvTags)?.apply {
            layoutManager = GridLayoutManager(this@FeedbackReportActivity, 2)
            adapter = tagAdapter
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            val spacing = (resources.displayMetrics.density * 6).toInt()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(spacing, spacing, spacing, spacing)
                }
            })
        }

        similarAdapter = SimilarVideoThumbAdapter { urlOrId ->
            openYouTubeVideo(urlOrId)
        }
        val tvSimilarEmpty = findViewById<TextView>(R.id.tvSimilarEmpty)
        rvSimilar = findViewById<RecyclerView>(R.id.rvSimilarVideos)?.apply {
            layoutManager =
                LinearLayoutManager(this@FeedbackReportActivity, RecyclerView.HORIZONTAL, false)
            adapter = similarAdapter
            setHasFixedSize(true)
            PagerSnapHelper().attachToRecyclerView(this)
            isNestedScrollingEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            val vSpace = (resources.displayMetrics.density * 8).toInt()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.top = vSpace
                    outRect.bottom = vSpace
                }
            })
        }

        (findViewById<View>(R.id.scroll) as? ViewGroup)?.apply {
            clipToPadding = false
            ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
                val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
                v.updatePadding(bottom = bottom)
                insets
            }
        }

        val feedbackId: Long = intent.getLongFlexible(
            ExtraKeys.FEEDBACK_ID, "feedback_id", "video_id", "id"
        ) ?: -1L

        val titlesFromIntent = intent.getStringArrayListExtra(ExtraKeys.TITLES)
            ?: intent.getStringArrayListExtra("titles_fixed")
        val hashtagsFromIntent = intent.getStringArrayListExtra(ExtraKeys.HASHTAGS)
            ?: intent.getStringArrayListExtra("hashtags_ready")
        val thumbsFromIntent = intent.getStringArrayListExtra(ExtraKeys.THUMBS)
            ?: intent.getStringArrayListExtra("thumbs_ready")

        val titlesFromIntentList = titlesFromIntent?.toList().orEmpty()
        val hashtagsFromIntentList = hashtagsFromIntent?.toList().orEmpty()
        val thumbsFromIntentList = thumbsFromIntent?.toList().orEmpty()

        val gotAnyFromIntent =
            titlesFromIntentList.isNotEmpty() || hashtagsFromIntentList.isNotEmpty() || thumbsFromIntentList.isNotEmpty()

        if (gotAnyFromIntent) {
            if (titlesFromIntentList.isEmpty()) tvTitlesEmpty?.isVisible = true
            else {
                tvTitlesEmpty?.isVisible = false; renderTitles(titlesFromIntentList)
            }

            val tagsClean =
                hashtagsFromIntentList.map { it.trim() }.filter { it.isNotBlank() }.distinct()
                    .take(10)
            rvTags?.isVisible = tagsClean.isNotEmpty()
            tagAdapter.submitFromStrings(tagsClean)

            val tClean = thumbsFromIntentList.filter { it.isNotBlank() }
            if (tClean.isEmpty()) tvSimilarEmpty?.isVisible = true
            else {
                tvSimilarEmpty?.isVisible = false; similarAdapter.submit(tClean)
            }

            if (feedbackId > 0L && (titlesFromIntentList.isEmpty() || tagsClean.isEmpty() || tClean.isEmpty())) {
                lifecycleScope.launch {
                    loadFeedbackAll(
                        feedbackId,
                        tvTitlesEmpty,
                        tvSimilarEmpty,
                        progressOverlay
                    )
                }
            }
            return
        }

        if (feedbackId > 0) {
            lifecycleScope.launch {
                loadFeedbackAll(
                    feedbackId,
                    tvTitlesEmpty,
                    tvSimilarEmpty,
                    progressOverlay
                )
            }
        } else {
            rvTags?.isVisible = false
            tagAdapter.submitFromStrings(emptyList())
            tvTitlesEmpty?.isVisible = true
            tvSimilarEmpty?.isVisible = true
        }
    }

    override fun onResume() {
        super.onResume()
        findViewById<View>(R.id.progressOverlay)?.isVisible = false
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setDimAmount(0f)

        val latest = intent.getStringOrNull(ExtraKeys.NICKNAME)
            .takeUnless { it.isNullOrBlank() }
            ?: com.h.trendie.util.NicknameStore.get(this)
        if (latest != nickname) {
            nickname = latest
            applyGreeting()
        }
    }

    private fun applyGreeting() {
        val safeTitle = cleanTitle(titleFromUser).ifBlank { "제목 없음" }
        tvGreeting?.text = "${nickname} 님의 <${safeTitle}>에 대한\n피드백 보고서가 도착했습니다📨"
    }

    private suspend fun loadFeedbackAll(
        feedbackId: Long,
        tvTitlesEmpty: TextView?,
        tvSimilarEmpty: TextView?,
        progress: View?
    ) {
        try {
            progress?.isVisible = true
            Log.d("Feedback", "loading report for feedbackId=$feedbackId")

            val resp = withContext(Dispatchers.IO) { ApiClient.apiService.getFeedback(feedbackId) }

            Log.d(
                "Feedback",
                "code=${resp.code()} err=${resp.errorBody()?.string()} body=${resp.body()}"
            )

            val body = if (resp.isSuccessful) resp.body() else null

            val titles = (body?.titles ?: emptyList())
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { isTitleProbablyComplete(it) }
                .distinct()

            if (titles.isEmpty()) {
                tvTitlesEmpty?.isVisible = true; renderTitles(emptyList())
            } else {
                tvTitlesEmpty?.isVisible = false; renderTitles(titles)
            }

            val tagsClean = (body?.hashtags ?: emptyList())
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(10)
            rvTags?.isVisible = tagsClean.isNotEmpty()
            tagAdapter.submitFromStrings(tagsClean)

            val displayItems: List<String> = (body?.similarVideos ?: emptyList()).mapNotNull { sv ->
                val v = sv.videoUrl.orEmpty()
                val t = sv.thumbnailUrl.orEmpty()
                when {
                    v.contains("youtu", ignoreCase = true) -> v
                    t.isNotBlank() -> t
                    else -> null
                }
            }.distinct()

            if (displayItems.isEmpty()) {
                tvSimilarEmpty?.isVisible = true; similarAdapter.submit(emptyList())
            } else {
                tvSimilarEmpty?.isVisible = false; similarAdapter.submit(displayItems)
            }

        } catch (e: Exception) {
            Log.e("Feedback", "요청 실패", e)
            Toast.makeText(this, "요청 실패", Toast.LENGTH_LONG).show()
            tvTitlesEmpty?.isVisible = true
            tvSimilarEmpty?.isVisible = true
            renderTitles(emptyList())
            rvTags?.isVisible = false
            tagAdapter.submitFromStrings(emptyList())
            similarAdapter.submit(emptyList())
        } finally {
            progress?.isVisible = false
        }
    }

    private fun renderTitles(list: List<String>) {
        val container = listTitles ?: return
        container.removeAllViews()
        val d = resources.displayMetrics.density
        list.forEachIndexed { idx, raw ->
            val card = makeTitleCard(idx + 1, cleanTitle(raw))
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { if (idx > 0) topMargin = (8 * d).toInt() }
            container.addView(card, lp)
        }
        container.requestLayout()
    }

    private fun makeTitleCard(rank: Int, title: String): View {
        val d = resources.displayMetrics.density
        val root = MaterialCardView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            cardElevation = 0f
            radius = 16f * d
            setCardBackgroundColor(Color.parseColor("#F7F7FB"))
            strokeColor = Color.parseColor("#E5E7EB")
            strokeWidth = (1f * d).toInt()
        }

        val row = ConstraintLayout(this).apply { id = View.generateViewId() }
        val tvRank = TextView(this).apply { id = View.generateViewId() }
        val tvTitle = TextView(this).apply { id = View.generateViewId() }

        root.addView(row)
        row.setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())

        tvRank.textSize = 12f
        tvRank.setPadding((8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt(), (4 * d).toInt())
        tvRank.setTextColor(Color.parseColor("#4338CA"))
        tvRank.background = GradientDrawable().apply {
            cornerRadius = 999f
            setColor(0x1A4F46E5)
        }
        tvRank.text = "$rank"

        tvTitle.textSize = 16f
        tvTitle.setTextColor(Color.parseColor("#0F172A"))
        tvTitle.maxLines = Int.MAX_VALUE
        tvTitle.ellipsize = null
        tvTitle.ellipsize = android.text.TextUtils.TruncateAt.END
        tvTitle.text = title

        row.addView(tvRank)
        row.addView(tvTitle)

        ConstraintSet().apply {
            clone(row)
            connect(tvRank.id, ConstraintSet.START, row.id, ConstraintSet.START)
            connect(tvRank.id, ConstraintSet.TOP, row.id, ConstraintSet.TOP)
            connect(tvRank.id, ConstraintSet.BOTTOM, row.id, ConstraintSet.BOTTOM)

            constrainWidth(tvTitle.id, ConstraintSet.MATCH_CONSTRAINT)
            connect(tvTitle.id, ConstraintSet.START, tvRank.id, ConstraintSet.END, (8 * d).toInt())
            connect(tvTitle.id, ConstraintSet.END, row.id, ConstraintSet.END)
            connect(tvTitle.id, ConstraintSet.TOP, row.id, ConstraintSet.TOP)
            connect(tvTitle.id, ConstraintSet.BOTTOM, row.id, ConstraintSet.BOTTOM)
            applyTo(row)
        }
        return root
    }

    private fun cleanTitle(raw: String): String {
        var s = raw.trim()
        s = s.replaceFirst(Regex("""^\s*\d+\.\s*"""), "")
            .replaceFirst(Regex("""^\s*[•\-]\s*"""), "")
        val opens = listOf("\"", "“", "‘", "＂", "＇")
        val closes = listOf("\"", "”", "’", "＂", "＇")
        if (s.isNotEmpty()) {
            while (opens.any { s.startsWith(it) } || closes.any { s.endsWith(it) }) {
                s = s.removePrefix("\"").removePrefix("“").removePrefix("‘")
                    .removePrefix("＂").removePrefix("＇")
                    .removeSuffix("\"").removeSuffix("”").removeSuffix("’")
                    .removeSuffix("＂").removeSuffix("＇")
                    .trim()
            }
        }
        return s
    }

    private fun isTitleProbablyComplete(raw: String): Boolean {
        val cleaned = cleanTitle(raw)

        if (cleaned.length < 2) return false

        val quoteChars = charArrayOf('"', '“', '”', '‘', '’', '＂', '＇')
        val quoteCount = raw.count { c -> quoteChars.contains(c) }
        if (quoteCount % 2 != 0) return false

        val tokens = cleaned.split(' ').filter { it.isNotBlank() }
        if (tokens.size >= 2 && tokens.last().length == 1 && cleaned.length >= 5) {
            return false
        }

        return true
    }

    // -------------------- PDF 내보내기 --------------------
    private fun exportViewToPdf(target: View, fileName: String): File {
        val width = target.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val specW = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        target.measure(specW, specH)
        target.layout(0, 0, target.measuredWidth, target.measuredHeight)

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(
            target.measuredWidth, target.measuredHeight, 1
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

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|\r\n]+"""), "_").trim('_', ' ')

    private fun Intent.getStringOrNull(key: String): String? {
        val v = extras?.get(key) ?: return null
        return when (v) {
            is String -> v
            is CharSequence -> v.toString()
            is Number -> v.toString()
            else -> null
        }
    }

    private fun Intent.getLongFlexible(vararg keys: String): Long? {
        val b = extras ?: return null
        for (k in keys) {
            if (!b.containsKey(k)) continue
            when (val v = b.get(k)) {
                is Long -> return v
                is Int -> return v.toLong()
                is String -> v.toLongOrNull()?.let { return it }
                is CharSequence -> v.toString().toLongOrNull()?.let { return it }
            }
        }
        return null
    }

    // -------------------- Hashtag 카드 어댑터 --------------------
    private data class HashtagItem(
        val tag: String,
        val percent: Int? = null
    )

    private inner class HashtagGridAdapter(
        private val onClick: ((String) -> Unit)? = null
    ) : RecyclerView.Adapter<HashtagGridAdapter.VH>() {

        private val items = mutableListOf<HashtagItem>()

        fun submitFromStrings(raw: List<String>) {
            items.clear()
            items.addAll(raw.map { HashtagItem(tag = ensureHash(it)) })
            notifyDataSetChanged()
        }

        inner class VH(val root: MaterialCardView) : RecyclerView.ViewHolder(root) {
            val row = ConstraintLayout(root.context).apply { id = View.generateViewId() }
            val tvRank = TextView(root.context).apply { id = View.generateViewId() }
            val tvTag = TextView(root.context).apply { id = View.generateViewId() }
            val tvPercent = TextView(root.context).apply { id = View.generateViewId() }

            init {
                val d = root.resources.displayMetrics.density
                root.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                root.cardElevation = 0f
                root.radius = 16f * d
                root.setCardBackgroundColor(Color.parseColor("#EAF5FF"))
                root.strokeColor = Color.parseColor("#BFDFFF")
                root.strokeWidth = (1f * d).toInt()

                root.addView(row)
                row.setPadding((8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt(), (8 * d).toInt())

                tvRank.textSize = 12f
                tvRank.setPadding(
                    (8 * d).toInt(),
                    (4 * d).toInt(),
                    (8 * d).toInt(),
                    (4 * d).toInt()
                )
                tvRank.setTextColor(Color.parseColor("#2163C6"))
                tvRank.background = GradientDrawable().apply {
                    cornerRadius = 999f
                    setColor(0x1A2163C6)
                }

                tvTag.textSize = 16f
                tvTag.setTextColor(Color.parseColor("#0F172A"))
                tvTag.maxLines = 2
                tvTag.ellipsize = android.text.TextUtils.TruncateAt.END

                tvPercent.textSize = 12f
                tvPercent.setPadding(
                    (12 * d).toInt(),
                    (4 * d).toInt(),
                    (12 * d).toInt(),
                    (4 * d).toInt()
                )
                tvPercent.setTextColor(Color.parseColor("#047857"))
                tvPercent.background = GradientDrawable().apply {
                    cornerRadius = 999f
                    setColor(0x1A10B981)
                }

                row.addView(tvRank)
                row.addView(tvTag)
                row.addView(tvPercent)

                ConstraintSet().apply {
                    clone(row)
                    connect(tvRank.id, ConstraintSet.START, row.id, ConstraintSet.START)
                    connect(tvRank.id, ConstraintSet.TOP, row.id, ConstraintSet.TOP)
                    connect(tvRank.id, ConstraintSet.BOTTOM, row.id, ConstraintSet.BOTTOM)

                    connect(tvPercent.id, ConstraintSet.END, row.id, ConstraintSet.END)
                    connect(tvPercent.id, ConstraintSet.TOP, row.id, ConstraintSet.TOP)
                    connect(tvPercent.id, ConstraintSet.BOTTOM, row.id, ConstraintSet.BOTTOM)

                    constrainWidth(tvTag.id, ConstraintSet.MATCH_CONSTRAINT)
                    connect(
                        tvTag.id,
                        ConstraintSet.START,
                        tvRank.id,
                        ConstraintSet.END,
                        (8 * d).toInt()
                    )
                    connect(
                        tvTag.id,
                        ConstraintSet.END,
                        tvPercent.id,
                        ConstraintSet.START,
                        (8 * d).toInt()
                    )
                    connect(tvTag.id, ConstraintSet.TOP, row.id, ConstraintSet.TOP)
                    connect(tvTag.id, ConstraintSet.BOTTOM, row.id, ConstraintSet.BOTTOM)
                    applyTo(row)
                }

                root.setOnClickListener {
                    val pos = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                        ?: return@setOnClickListener
                    onClick?.invoke(items[pos].tag.removePrefix("#"))
                }
            }

            fun bind(item: HashtagItem, pos: Int) {
                tvRank.text = "${pos + 1}"
                tvTag.text = item.tag
                if (item.percent == null) {
                    tvPercent.visibility = View.GONE
                    ConstraintSet().apply {
                        clone(row)
                        clear(tvTag.id, ConstraintSet.END)
                        connect(tvTag.id, ConstraintSet.END, row.id, ConstraintSet.END)
                        applyTo(row)
                    }
                } else {
                    val sign = if (item.percent >= 0) "+" else ""
                    tvPercent.text = "$sign${item.percent}%"
                    tvPercent.visibility = View.VISIBLE
                    ConstraintSet().apply {
                        clone(row)
                        clear(tvTag.id, ConstraintSet.END)
                        connect(
                            tvTag.id,
                            ConstraintSet.END,
                            tvPercent.id,
                            ConstraintSet.START,
                            (8 * root.resources.displayMetrics.density).toInt()
                        )
                        applyTo(row)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(MaterialCardView(parent.context))

        override fun getItemCount(): Int = items.size
        override fun onBindViewHolder(h: VH, pos: Int) = h.bind(items[pos], pos)
    }

    private fun ensureHash(s: String): String {
        val t = s.trim()
        if (t.isEmpty()) return t
        return if (t.startsWith("#")) t else "#$t"
    }

    private inner class SimilarVideoThumbAdapter(
        private val onClick: ((String) -> Unit)? = null
    ) : RecyclerView.Adapter<SimilarVideoThumbAdapter.VH>() {
        private val items = mutableListOf<String>()
        fun submit(list: List<String>) {
            items.clear(); items.addAll(list); notifyDataSetChanged()
        }

        inner class VH(val iv: ShapeableImageView) : RecyclerView.ViewHolder(iv)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val iv = ShapeableImageView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                adjustViewBounds = true
                scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                setPadding(0, 0, 0, 0)
            }
            return VH(iv)
        }

        override fun getItemCount() = items.size
        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.iv.post {
                val w = holder.iv.width.takeIf { it > 0 }
                    ?: holder.iv.resources.displayMetrics.widthPixels
                val h = (w * 9f / 16f).toInt()
                if (holder.iv.layoutParams.height != h) {
                    holder.iv.layoutParams.height = h
                    holder.iv.requestLayout()
                }

                val urlOrId = items[position]
                val looksLikeYouTube = urlOrId.contains("youtu", ignoreCase = true) ||
                        urlOrId.contains("ytimg", ignoreCase = true)

                if (looksLikeYouTube && extractYouTubeId(urlOrId) != null) {
                    holder.iv.loadYouTubeBest(urlOrId, w, h)
                } else {
                    holder.iv.load(urlOrId) {
                        crossfade(true)
                        size(w, h)
                        precision(Precision.EXACT)
                        scale(Scale.FILL)
                        allowHardware(true)
                        memoryCachePolicy(CachePolicy.ENABLED)
                        diskCachePolicy(CachePolicy.ENABLED)
                        placeholder(android.R.color.darker_gray)
                        error(android.R.color.darker_gray)
                    }
                }
            }
            holder.iv.setOnClickListener { onClick?.invoke(items[position]) }
        }
    }

    private fun extractYouTubeId(input: String): String? {
        val idRegex = Regex("""^[A-Za-z0-9_-]{11}$""")
        val s = input.trim()

        if (idRegex.matches(s)) return s

        return try {
            val uri = Uri.parse(s)
            // youtu.be/<id>
            if (uri.host?.contains("youtu.be", ignoreCase = true) == true) {
                uri.lastPathSegment?.takeIf { idRegex.matches(it) }?.let { return it }
            }
            // youtube.com/watch?v=<id>
            uri.getQueryParameter("v")?.takeIf { idRegex.matches(it) }?.let { return it }
            // /shorts/<id>, /embed/<id>
            val pathId = Regex("""/(?:shorts|embed)/([A-Za-z0-9_-]{11})""")
                .find(s)?.groupValues?.getOrNull(1)
            if (pathId != null) return pathId
            // i.ytimg.com/vi/<id>/...
            Regex("""/vi(?:_webp)?/([A-Za-z0-9_-]{11})/""")
                .find(s)?.groupValues?.getOrNull(1)
        } catch (_: Exception) {
            null
        }
    }

    private fun ShapeableImageView.loadYouTubeBest(
        idOrUrl: String,
        wHint: Int? = null,
        hHint: Int? = null
    ) {
        val id = extractYouTubeId(idOrUrl) ?: return
        val urls = listOf(
            "https://i.ytimg.com/vi_webp/$id/maxresdefault.webp",
            "https://i.ytimg.com/vi/$id/maxresdefault.jpg",
            "https://i.ytimg.com/vi_webp/$id/sddefault.webp",
            "https://i.ytimg.com/vi/$id/sddefault.jpg",
            "https://i.ytimg.com/vi_webp/$id/hqdefault.webp",
            "https://i.ytimg.com/vi/$id/hqdefault.jpg",
            "https://i.ytimg.com/vi/$id/mqdefault.jpg",
            "https://i.ytimg.com/vi/$id/default.jpg"
        )
        post {
            val w = wHint ?: width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
            val h = hHint ?: (w * 9f / 16f).toInt()
            if (layoutParams.height != h) {
                layoutParams.height = h; requestLayout()
            }
            fun tryAt(i: Int) {
                if (i >= urls.size) {
                    setImageResource(android.R.color.darker_gray); return
                }
                load(urls[i]) {
                    crossfade(true)
                    size(w, h)
                    precision(Precision.EXACT)
                    scale(Scale.FILL)
                    allowHardware(true)
                    memoryCachePolicy(CachePolicy.ENABLED)
                    diskCachePolicy(CachePolicy.ENABLED)
                    placeholder(android.R.color.darker_gray)
                    error(android.R.color.darker_gray)
                    listener(onError = { _, _ -> tryAt(i + 1) })
                }
            }
            tryAt(0)
        }
    }

    private fun openYouTubeVideo(urlOrId: String) {
        val id = extractYouTubeId(urlOrId)
        val uri = if (id != null) {
            Uri.parse("https://www.youtube.com/watch?v=$id")
        } else {
            Uri.parse(urlOrId)
        }
        val appIntent = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube")
        try {
            startActivity(appIntent)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun openYouTubeSearch(query: String) {
        val uri = Uri.Builder()
            .scheme("https").authority("www.youtube.com")
            .appendPath("results")
            .appendQueryParameter("search_query", query)
            .build()
        val app = Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.youtube")
        try {
            startActivity(app)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }

    private fun readNicknameMulti(): String? {
        val intentNick = intent.getStringOrNull(ExtraKeys.NICKNAME)

        val prefsCandidates =
            listOf("user_prefs", "Userprefs", "UserPrefs", "profile_prefs", "mypage_prefs")
        val keyCandidates = listOf("nickname", "user_nickname", "nick_name", "name", "userName")
        var prefsNick: String? = null
        outer@ for (prefsName in prefsCandidates) {
            val sp = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            for (key in keyCandidates) {
                val v = sp.getString(key, null)
                if (!v.isNullOrBlank()) {
                    prefsNick = v; break@outer
                }
            }
        }

        Log.d("Nickname", "intent nickname=$intentNick")
        Log.d("Nickname", "prefs nickname=${prefsNick ?: "null"}")

        return intentNick ?: prefsNick
    }
}

private fun Context.readNickname(): String {
    val prefsNames = listOf("user_prefs", "Userprefs", "UserPrefs")
    for (name in prefsNames) {
        val v = getSharedPreferences(name, Context.MODE_PRIVATE).getString("nickname", null)
        if (!v.isNullOrBlank()) return v
    }
    return "유저"
}