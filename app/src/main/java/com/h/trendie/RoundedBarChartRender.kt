package com.h.trendie

import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import com.github.mikephil.charting.animation.ChartAnimator
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import com.github.mikephil.charting.renderer.BarChartRenderer
import com.github.mikephil.charting.utils.ViewPortHandler

class RoundedBarChartRenderer(
    chart: BarChart,
    animator: ChartAnimator,
    viewPortHandler: ViewPortHandler
) : BarChartRenderer(chart, animator, viewPortHandler) {

    override fun drawDataSet(c: Canvas, dataSet: IBarDataSet, index: Int) {
        val trans = mChart.getTransformer(dataSet.axisDependency)

        val buffer = mBarBuffers[index]
        buffer.setPhases(mAnimator.phaseX, mAnimator.phaseY)
        buffer.setDataSet(index)
        buffer.setInverted(mChart.isInverted(dataSet.axisDependency))
        buffer.setBarWidth(mChart.barData.barWidth)
        buffer.feed(dataSet)

        trans.pointValuesToPixel(buffer.buffer)

        val radius = 24f
        val path = Path()
        val rect = RectF()
        val radiiTopOnly = floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f)

        for (j in 0 until buffer.buffer.size step 4) {
            if (!mViewPortHandler.isInBoundsLeft(buffer.buffer[j + 2])) continue
            if (!mViewPortHandler.isInBoundsRight(buffer.buffer[j])) break

            mRenderPaint.color = dataSet.getColor(j / 4)

            val left = buffer.buffer[j]
            val top = buffer.buffer[j + 1]
            val right = buffer.buffer[j + 2]
            val bottom = buffer.buffer[j + 3]

            rect.set(left, top, right, bottom)
            path.reset()
            path.addRoundRect(rect, radiiTopOnly, Path.Direction.CW)
            c.drawPath(path, mRenderPaint)

            if (dataSet.barBorderWidth > 0f) {
                c.drawRoundRect(rect, radius, radius, mBarBorderPaint)
            }
        }
    }
}