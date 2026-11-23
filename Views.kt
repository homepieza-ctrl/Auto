package com.launcher.dd
import android.content.Context
import android.graphics.*
import android.view.View

class SpeedView(c: Context) : View(c) {
    private var shown = 0f; private var target = 0f
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 26f; strokeCap = Paint.Cap.ROUND }
    private val bg = Paint(arc).apply { color = 0x3300E5FF }
    private val num = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF22D3FF.toInt(); textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD }
    private val unit = Paint(num).apply { typeface = Typeface.DEFAULT }
    private val r = RectF()
    fun setSpeed(kmh: Float) { target = kmh; postInvalidateOnAnimation() }
    override fun onDraw(cv: Canvas) {
        shown += (target - shown) * 0.15f
        val s = minOf(width, height).toFloat(); val m = 30f
        r.set(width / 2f - s / 2 + m, height / 2f - s / 2 + m, width / 2f + s / 2 - m, height / 2f + s / 2 - m)
        arc.shader = SweepGradient(width / 2f, height / 2f, intArrayOf(0xFF00E5FF.toInt(), 0xFF3D5AFE.toInt(), 0xFFFF2DAA.toInt()), null)
        cv.drawArc(r, 135f, 270f, false, bg)
        cv.drawArc(r, 135f, 270f * (shown / 220f).coerceIn(0f, 1f), false, arc)
        num.textSize = s * 0.38f; unit.textSize = s * 0.1f
        cv.drawText(shown.toInt().toString(), width / 2f, height / 2f + s * 0.12f, num)
        cv.drawText("km/h", width / 2f, height / 2f + s * 0.28f, unit)
        if (Math.abs(target - shown) > 0.5f) postInvalidateOnAnimation()
    }
}

class RoadView(c: Context) : View(c) {
    private var speed = 0f; private var off = 0f
    private val p = Paint(Paint.ANTI_ALIAS_FLAG); private val path = Path(); private val rr = RectF()
    fun setSpeed(kmh: Float) { speed = kmh; postInvalidateOnAnimation() }
    override fun onDraw(cv: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val cx = w / 2
        p.color = 0xFF5BBF3A.toInt(); cv.drawRect(0f, 0f, w, h, p)
        path.reset(); path.moveTo(cx - 8, 0f); path.lineTo(cx + 8, 0f); path.lineTo(w * .85f, h); path.lineTo(w * .15f, h); path.close()
        p.color = 0xFF555555.toInt(); cv.drawPath(path, p)
        off = (off + speed * 0.12f) % 80f
        p.color = Color.WHITE; p.strokeWidth = 5f
        var y = off - 80f
        while (y < h) { cv.drawLine(cx, y, cx, y + 40f, p); y += 80f }
        val shake = if (speed > 1) Math.sin(System.nanoTime() / 6e7).toFloat() * 2f else 0f
        p.color = 0xFFF5A623.toInt(); rr.set(cx - 26 + shake, h * .6f, cx + 26 + shake, h * .6f + 100)
        cv.drawRoundRect(rr, 16f, 16f, p)
        p.color = 0xFF222222.toInt(); rr.set(cx - 20 + shake, h * .6f + 25, cx + 20 + shake, h * .6f + 50); cv.drawRoundRect(rr, 8f, 8f, p)
        if (speed > 1) postInvalidateOnAnimation()
    }
}
