/* package com.h.trendie

class RoundedBarChartRenderer {
    //그래프 둥글리기
    package com.h.trendie.ui

    import android.graphics.Canvas
    import android.graphics.Paint
    import android.graphics.Path
    import com.github.mikephil.charting.charts.BarChart
    import com.github.mikephil.charting.data.BarDataSet
    import com.github.mikephil.charting.renderer.BarChartRenderer
    import com.github.mikephil.charting.utils.ViewPortHandler

    class RoundedBarChartRenderer(
        chart: BarChart,
        animator: com.github.mikephil.charting.animation.ChartAnimator,
        viewPortHandler: ViewPortHandler,
        private val radius: Float = 20f  // ← 둥글기 정도
    ) : BarChartRenderer(chart, animator, viewPortHandler) {

        override fun drawDataSet(c: Canvas, dataSet: BarDataSet, index: Int) {
            val buffer = mBarBuffers[index]
            val paint = mRenderPaint
            paint.style = Paint.Style.FILL

            for (j in 0 until buffer.buffer.size step 4) {
                val left = buffer.buffer[j]
                val top = buffer.buffer[j + 1]
                val right = buffer.buffer[j + 2]
                val bottom = buffer.buffer[j + 3]

                val path = Path().apply {
                    addRoundRect(
                        left, top, right, bottom,
                        floatArrayOf(
                            radius, radius,   // 좌상
                            radius, radius,   // 우상
                            0f, 0f,           // 우하
                            0f, 0f            // 좌하
                        ),
                        Path.Direction.CW
                    )
                }
                c.drawPath(path, paint)
            }
        }
    }

} */