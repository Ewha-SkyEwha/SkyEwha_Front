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
import com.h.trendie.network.ApiClient
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val vm by viewModels<HomeViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(com.h.trendie.MockHomeRepository()) as T
            }
        }
    }

    private lateinit var risingAdapter: KeywordRisingAdapter
    private lateinit var rvRising: RecyclerView

    private lateinit var videosAdapter: VideosAdapter
    private lateinit var barChart: BarChart
    private var chartCard: MaterialCardView? = null

    override fun onViewCreated(v: View, s: Bundle?) {
        super.onViewCreated(v, s)

        // ----- view refs -----
        rvRising = v.findViewById(R.id.rvRising)
        barChart = v.findViewById(R.id.barChartHashtags)
        chartCard = (barChart.parent as? MaterialCardView)

        barChart.renderer = RoundedBarChartRenderer(
            barChart,
            barChart.animator,
            barChart.viewPortHandler
        )

        // ----- 급상승 키워드(그리드) -----
        risingAdapter = KeywordRisingAdapter()
        rvRising.apply {
            layoutManager = GridLayoutManager(context, 2)
            adapter = risingAdapter
            itemAnimator = null
            setHasFixedSize(false)
        }


        // ----- 인기 동영상 가로 스크롤 -----
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
                val snap = st.data ?: run {
                    renderEmpty()
                    return@collectLatest
                }
                bind(snap)
            }
        }
    }

    private fun bind(snap: HomeSnapshot) {

        val txt = safeColor(R.color.colorPrimaryText, Color.DKGRAY)
        val grid = safeColor(R.color.colorGrey, 0xFF9E9E9E.toInt())

        // ---------- 해시태그 차트 ----------
        val items = snap.hashtagsTop10.orEmpty()
        if (items.isEmpty()) {
            chartCard?.isVisible = false
        } else {
            chartCard?.isVisible = true

            val topIndex = items.withIndex().maxByOrNull { it.value.count }?.index ?: 0
            val first = safeColor(R.color.chart_bar_first, Color.parseColor("#4F7EFF"))
            val rest  = safeColor(R.color.chart_bar_default, Color.parseColor("#C7D1FF"))

            val barColors = items.mapIndexed { i, _ -> if (i == topIndex) first else rest }
            val entries = items.mapIndexed { i, h ->
                BarEntry(i.toFloat(), h.count.coerceAtLeast(0).toFloat(), h.tag)
            }

            val set = BarDataSet(entries, "해시태그").apply {
                colors = barColors
                setDrawValues(true)
                valueTextSize = 10f
                val txt = safeColor(R.color.colorPrimaryText, Color.DKGRAY)
                setValueTextColors(items.mapIndexed { i, _ -> if (i == topIndex) first else txt })

                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(e: BarEntry?): String {
                        if (e == null) return ""
                        val crown = if (e.x.toInt() == topIndex) "👑 " else ""
                        return crown + e.y.toInt().toString()
                    }
                }
                highLightColor = Color.TRANSPARENT
            }

            barChart.apply {
                data = BarData(set).apply { barWidth = 0.32f }
                description.isEnabled = false
                legend.isEnabled = false
                axisRight.isEnabled = false
                axisLeft.axisMinimum = 0f

                setFitBars(true)
                setMinOffset(16f)
                setExtraOffsets(12f, 8f, 12f, 16f)

                val labels2 = items.map { wrapLabel2Lines(it.tag ?: "", maxPerLine = 6) }

                xAxis.apply {
                    textColor = txt
                    axisLineColor = grid
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
                    axisLineColor = grid
                    gridColor = grid
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
                        val tag = (e as? BarEntry)?.data as? String ?: return
                        if (tag.isNotBlank()) {
                            openUrlSafely("https://www.youtube.com/results?search_query=$tag")
                        }
                    }
                    override fun onNothingSelected() {}
                })

                notifyDataSetChanged()
                animateY(800)
                invalidate()
            }
        }

        // ---------- 급상승 키워드 ----------
        risingAdapter.submit(snap.risingTop10.orEmpty())

        // ---------- 인기 동영상 ----------
        videosAdapter.submit(snap.popularVideos.orEmpty())
    }

    private fun renderEmpty() {
        chartCard?.isVisible = false
        risingAdapter.submit(emptyList())
        videosAdapter.submit(emptyList())
        if (::barChart.isInitialized) {
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
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "열 수 있는 앱이 없어요.", Toast.LENGTH_SHORT).show()
        }
    }
}