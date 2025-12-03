package com.h.trendie

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.card.MaterialCardView
import com.h.trendie.model.HomeSnapshot
import com.h.trendie.ui.theme.applyTopInsetPadding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val vm by viewModels<HomeViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = if (BuildConfig.DEBUG) {
                    com.h.trendie.data.RealHomeRepository()
                } else {
                    com.h.trendie.data.RealHomeRepository()
                }
                return HomeViewModel(repo) as T
            }
        }
    }

    private lateinit var keywordrisingAdapter: KeywordRisingAdapter
    private lateinit var rvRising: RecyclerView

    private lateinit var videosAdapter: VideosAdapter
    private lateinit var barChart: BarChart
    private var chartCard: MaterialCardView? = null

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        v.findViewById<View>(R.id.headerContainer)?.applyTopInsetPadding()

        rvRising = v.findViewById(R.id.rvRising)
        barChart = v.findViewById(R.id.barChartHashtags)
        chartCard = (barChart.parent as? MaterialCardView)

        runCatching {
            barChart.renderer = RoundedBarChartRenderer(
                barChart, barChart.animator, barChart.viewPortHandler
            )
        }

        // 급상승 키워드
        keywordrisingAdapter = KeywordRisingAdapter()
        rvRising.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = keywordrisingAdapter
            itemAnimator = null
            setHasFixedSize(false)
        }

        // 인기 동영상
        videosAdapter = VideosAdapter { item ->
            val url = item.videoUrl
            if (!url.isNullOrBlank()) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } else {
                Toast.makeText(requireContext(), "영상 URL이 없어요.", Toast.LENGTH_SHORT).show()
            }
        }
        v.findViewById<RecyclerView>(R.id.rvVideos).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = videosAdapter
            itemAnimator = null
            setHasFixedSize(true)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            vm.state.collectLatest { st: UiState<HomeSnapshot> ->
                val snap = st.data ?: return@collectLatest renderEmpty()
                bind(snap)
            }
        }
    }

    private fun bind(snap: HomeSnapshot) {
        // 기본 텍스트 색 (제목/수치표시)
        val txt = safeColor(R.color.colorPrimaryText, Color.DKGRAY)

        val topBarColor    = android.graphics.Color.parseColor("#CFF7D9") // 1등 막대
        val normalBarColor = android.graphics.Color.parseColor("#C2E0F9") // 나머지 막대

        val surfaceColor   = safeColor(R.color.colorSurface, android.graphics.Color.WHITE)
        val gridSky        = safeColor(R.color.colorDivider, 0xFFD5E4FF.toInt())

        // ---------- 해시태그 차트 ----------
        val items = snap.hashtagsTop10.orEmpty()
        if (items.isEmpty()) {
            chartCard?.isVisible = false
        } else {
            chartCard?.isVisible = true

            chartCard?.setCardBackgroundColor(surfaceColor)
            barChart.setBackgroundColor(surfaceColor)

            val topIndex = items.withIndex().maxByOrNull { it.value.count }?.index ?: 0

            val barColors = items.mapIndexed { i, _ ->
                if (i == topIndex) topBarColor else normalBarColor
            }

            val entries = items.mapIndexed { i, h ->
                BarEntry(i.toFloat(), h.count.coerceAtLeast(0).toFloat(), h.tag)
            }

            val set = BarDataSet(entries, "해시태그").apply {
                colors = barColors
                setDrawValues(true)
                valueTextSize = 10f
                setValueTextColors(List(items.size) { txt })
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(e: BarEntry?): String {
                        e ?: return ""
                        val crown = if (e.x.toInt() == topIndex) "👑 " else ""
                        return crown + e.y.toInt().toString()
                    }
                }
                highLightColor = Color.TRANSPARENT
            }

            barChart.apply {
                data = BarData(set).apply {
                    barWidth = 0.32f
                }
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f

                setFitBars(true)
                setMinOffset(16f)
                setExtraOffsets(12f, 8f, 12f, 16f)

                val labels2 = items.map { wrapLabel2Lines(it.tag.orEmpty(), maxPerLine = 6) }

                xAxis.apply {
                    textColor = txt
                    axisLineColor = gridSky
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    granularity = 1f
                    isGranularityEnabled = true
                    setLabelCount(minOf(5, items.size), false)
                    labelRotationAngle = 0f
                    textSize = 9f
                    yOffset = 8f
                    axisMinimum = -0.5f
                    axisMaximum = items.size - 0.5f
                    valueFormatter = IndexAxisValueFormatter(labels2)
                }

                axisLeft.apply {
                    textColor = txt
                    axisLineColor = gridSky
                    gridColor = gridSky
                }

                setDragEnabled(items.size > 5)
                setScaleXEnabled(false)
                setScaleYEnabled(false)
                setVisibleXRangeMaximum(5f)
                moveViewToX(0f)

                setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                    override fun onValueSelected(
                        e: com.github.mikephil.charting.data.Entry?,
                        h: com.github.mikephil.charting.highlight.Highlight?
                    ) {
                        val raw = (e as? BarEntry)?.data as? String ?: return
                        val tag = raw.trim().removePrefix("#")
                        if (tag.isNotBlank()) openYouTubeSearch(tag)
                    }
                    override fun onNothingSelected() {}
                })

                notifyDataSetChanged()
                animateY(800)
                invalidate()
            }
        }

        // ---------- 급상승 키워드 ----------
        keywordrisingAdapter.submit(snap.risingTop10.orEmpty())

        // ---------- 인기 동영상 ----------
        videosAdapter.submit(snap.popularVideos.orEmpty())
    }

    private fun renderEmpty() {
        chartCard?.isVisible = false
        if (this::keywordrisingAdapter.isInitialized) keywordrisingAdapter.submit(emptyList())
        if (this::videosAdapter.isInitialized) videosAdapter.submit(emptyList())
        if (this::barChart.isInitialized) {
            barChart.clear()
            barChart.invalidate()
        }
    }

    private fun wrapLabel2Lines(s: String, maxPerLine: Int = 6): String {
        val t = s.trim()
        if (t.isEmpty()) return ""
        val p = t.indexOf(' ')
        return when {
            p in 1 until maxPerLine -> t.replaceFirst(" ", "\n")
            t.length > maxPerLine   -> t.substring(0, maxPerLine) + "\n" + t.substring(maxPerLine)
            else -> t
        }
    }

    private fun safeColor(resId: Int, fallback: Int): Int =
        try { ContextCompat.getColor(requireContext(), resId) } catch (_: Exception) { fallback }

    private fun openUrlSafely(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "열 수 있는 앱이 없어요.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openYouTubeSearch(query: String) {
        val webUri = Uri.Builder()
            .scheme("https")
            .authority("www.youtube.com")
            .appendPath("results")
            .appendQueryParameter("search_query", query)
            .build()

        val appIntent = Intent(Intent.ACTION_VIEW, webUri).setPackage("com.google.android.youtube")
        try {
            startActivity(appIntent)
        } catch (_: ActivityNotFoundException) {
            openUrlSafely(webUri.toString())
        }
    }
}
