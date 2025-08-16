package com.h.trendie

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val vm by viewModels<HomeViewModel> {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(c: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(MockHomeRepository(requireContext())) as T
            }
        }
    }

    private lateinit var risingAdapter: RisingAdapter
    private lateinit var videosAdapter: VideosAdapter
    private lateinit var barChart: com.github.mikephil.charting.charts.BarChart

    override fun onViewCreated(v: View, s: Bundle?) {
        risingAdapter = RisingAdapter()
        videosAdapter = VideosAdapter()

        v.findViewById<RecyclerView>(R.id.rvRising).apply {
            layoutManager = GridLayoutManager(context, 2) // 2열
            adapter = risingAdapter
        }
        v.findViewById<RecyclerView>(R.id.rvVideos).apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = videosAdapter
        }

        barChart = v.findViewById(R.id.barChartHashtags)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            vm.state.collect { st -> st.data?.let { bind(it) } }
        }
    }

    private fun bind(snap: HomeSnapshot) {
        val ctx = requireContext()

        // 8개만 보이게 하려면 주석 해제
        val items = snap.hashtagsTop10 /* .take(8) */

        // 1) 1등(최대 count) 인덱스
        val topIndex = items.withIndex().maxByOrNull { it.value.count }?.index ?: 0

        // 2) 색상
        val first = ContextCompat.getColor(ctx, R.color.chart_bar_first)     // #3F618C
        val rest  = ContextCompat.getColor(ctx, R.color.chart_bar_default)   // #9FBCDB
        val barColors = items.mapIndexed { i, _ -> if (i == topIndex) first else rest }

        // 3) 엔트리 (클릭용 tag data에 저장)
        val entries = items.mapIndexed { i, h ->
            com.github.mikephil.charting.data.BarEntry(i.toFloat(), h.count.toFloat(), h.tag)
        }

        // 4) DataSet (값 라벨엔 1등만 👑)
        val set = com.github.mikephil.charting.data.BarDataSet(entries, "해시태그").apply {
            colors = barColors
            setDrawValues(true)
            valueTextSize = 10f
            val valueTextColors = items.mapIndexed { i, _ -> if (i == topIndex) first else android.graphics.Color.DKGRAY }
            setValueTextColors(valueTextColors)
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getBarLabel(e: com.github.mikephil.charting.data.BarEntry?): String {
                    if (e == null) return ""
                    val crown = if (e.x.toInt() == topIndex) "👑 " else ""
                    return crown + e.y.toInt().toString()
                }
            }
            highLightColor = android.graphics.Color.TRANSPARENT
        }
        val data = com.github.mikephil.charting.data.BarData(set).apply { barWidth = 0.32f }

        // 5) 차트 공통
        barChart.data = data
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.axisRight.isEnabled = false
        barChart.axisLeft.axisMinimum = 0f

        // 6) X축: 2줄 라벨 + 스크롤 안정화 (중복/밀림 방지)
        val labels2 = items.map { wrapLabel2Lines(it.tag, maxPerLine = 6) }
        val x = barChart.xAxis
        x.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        x.setDrawGridLines(false)
        x.granularity = 1f                // 정수 간격만 사용
        x.isGranularityEnabled = true
        x.setLabelCount(5, false)         // 강제하지 않음(스크롤 안정)
        x.labelRotationAngle = 0f
        x.textSize = 9f
        x.yOffset = 8f
        x.axisMinimum = -0.5f             // 막대 중심이 0..n-1
        x.axisMaximum = items.size - 0.5f
        x.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels2)

        // 7) 둥근 막대 렌더러
        barChart.renderer = RoundedBarChartRenderer(barChart, barChart.animator, barChart.viewPortHandler)

        // 8) 가로 스크롤
        barChart.setDragEnabled(true)
        barChart.setScaleXEnabled(false)
        barChart.setScaleYEnabled(false)
        barChart.setVisibleXRangeMaximum(5f)
        barChart.moveViewToX(0f)

        // 9) 막대 클릭 시 유튜브 검색으로 이동
        barChart.setOnChartValueSelectedListener(object :
            com.github.mikephil.charting.listener.OnChartValueSelectedListener {
            override fun onValueSelected(e: com.github.mikephil.charting.data.Entry?, h: com.github.mikephil.charting.highlight.Highlight?) {
                e ?: return
                val tag = e.data as? String ?: return
                startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://www.youtube.com/results?search_query=$tag")))
            }
            override fun onNothingSelected() {}
        })

        barChart.notifyDataSetChanged()
        barChart.animateY(800)
        barChart.invalidate()

        // 리스트
        risingAdapter.submit(snap.risingTop10)
        videosAdapter.submit(snap.popularVideos)
    }

    // 공백 있으면 공백에서 줄바꿈, 없으면 글자 수 기준 2줄로
    private fun wrapLabel2Lines(s: String, maxPerLine: Int = 6): String {
        val t = s.trim()
        val p = t.indexOf(' ')
        return when {
            p in 1 until maxPerLine -> t.replaceFirst(" ", "\n")
            t.length > maxPerLine   -> t.substring(0, maxPerLine) + "\n" + t.substring(maxPerLine)
            else -> t
        }
    }
}
