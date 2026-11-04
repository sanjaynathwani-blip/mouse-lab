package app.mouselab.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View

/**
 * The measuring box. A click captures the pointer (Android's equivalent of the browser's
 * pointer lock), after which the mouse reports raw relative counts, with no pointer
 * acceleration, through [onCapturedPointerEvent]. Every hardware report is passed on with
 * its own kernel timestamp, including the ones Android batched into one event's history.
 *
 * Right-click, Escape or losing capture stops measuring.
 */
class CapturePadView(context: Context) : View(context) {

    interface Listener {
        /** Capture started or stopped. */
        fun onCaptureChanged(captured: Boolean) {}

        /** One mouse report: relative movement in counts, and its time in milliseconds. */
        fun onReport(dx: Float, dy: Float, timeMs: Double) {}

        /** All reports of one delivered event have been passed on. */
        fun onBatchEnd() {}

        /** Input came from something other than a mouse, such as a touchpad. */
        fun onNonMouseInput() {}
    }

    var listener: Listener? = null

    /** Draw the path of the captured movement (used by the rotation tool). */
    var drawTrail = false

    /** Colour of the trail. */
    var trailColor: Int
        get() = trailPaint.color
        set(value) {
            trailPaint.color = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // Offsets from the centre of the box, so the trail survives resizes.
    private var cursorX = 0f
    private var cursorY = 0f
    private val trailX = FloatArray(TRAIL_POINTS)
    private val trailY = FloatArray(TRAIL_POINTS)
    private var trailStart = 0
    private var trailSize = 0
    private val path = Path()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        isClickable = true
    }

    fun startCapture() {
        if (hasPointerCapture()) return
        requestFocus()
        requestPointerCapture()
    }

    fun stopCapture() {
        if (hasPointerCapture()) releasePointerCapture()
    }

    fun clearTrail() {
        cursorX = 0f
        cursorY = 0f
        trailSize = 0
        trailStart = 0
        addTrailPoint()
        invalidate()
    }

    private var primaryDown = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // A primary click (or a tap) starts measuring; a right-click does not.
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN ->
                primaryDown = event.buttonState == 0 || event.buttonState and MotionEvent.BUTTON_PRIMARY != 0
            MotionEvent.ACTION_UP -> if (primaryDown) {
                primaryDown = false
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> primaryDown = false
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        startCapture()
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (hasPointerCapture() && keyCode == KeyEvent.KEYCODE_ESCAPE) {
            stopCapture()
            return true
        }
        if (!hasPointerCapture() && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_SPACE)) {
            startCapture()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onPointerCaptureChange(hasCapture: Boolean) {
        if (hasCapture && Build.VERSION.SDK_INT >= 30) {
            // Deliver every report as it arrives rather than once per frame.
            requestUnbufferedDispatch(InputDevice.SOURCE_MOUSE_RELATIVE)
        }
        listener?.onCaptureChanged(hasCapture)
    }

    override fun onCapturedPointerEvent(event: MotionEvent): Boolean {
        if (!event.isFromSource(InputDevice.SOURCE_MOUSE_RELATIVE)) {
            listener?.onNonMouseInput()
            return true
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_BUTTON_PRESS -> {
                if (event.actionButton == MotionEvent.BUTTON_SECONDARY) stopCapture()
            }
            MotionEvent.ACTION_MOVE -> {
                val l = listener ?: return true
                for (i in 0 until event.historySize) {
                    val dx = event.getHistoricalX(i)
                    val dy = event.getHistoricalY(i)
                    l.onReport(dx, dy, historicalTimeMs(event, i))
                    if (drawTrail) moveTrail(dx, dy)
                }
                l.onReport(event.x, event.y, timeMs(event))
                if (drawTrail) moveTrail(event.x, event.y)
                l.onBatchEnd()
                if (drawTrail) invalidate()
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (trailSize == 0) addTrailPoint()
    }

    override fun onDraw(canvas: Canvas) {
        if (!drawTrail || trailSize < 2) return
        val cx = width / 2f
        val cy = height / 2f
        path.rewind()
        for (k in 0 until trailSize) {
            val idx = (trailStart + k) % TRAIL_POINTS
            val x = cx + trailX[idx]
            val y = cy + trailY[idx]
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, trailPaint)
    }

    private fun moveTrail(dx: Float, dy: Float) {
        val halfW = width / 2f
        val halfH = height / 2f
        cursorX = (cursorX + dx * TRAIL_SCALE).coerceIn(-halfW, halfW)
        cursorY = (cursorY + dy * TRAIL_SCALE).coerceIn(-halfH, halfH)
        addTrailPoint()
    }

    private fun addTrailPoint() {
        val idx = (trailStart + trailSize) % TRAIL_POINTS
        trailX[idx] = cursorX
        trailY[idx] = cursorY
        if (trailSize < TRAIL_POINTS) trailSize++ else trailStart = (trailStart + 1) % TRAIL_POINTS
    }

    private companion object {
        const val TRAIL_POINTS = 600

        /** Screen pixels per mouse count when drawing the trail. */
        const val TRAIL_SCALE = 1f

        fun timeMs(event: MotionEvent): Double =
            if (Build.VERSION.SDK_INT >= 34) event.eventTimeNanos / 1e6 else event.eventTime.toDouble()

        fun historicalTimeMs(event: MotionEvent, pos: Int): Double =
            if (Build.VERSION.SDK_INT >= 34) event.getHistoricalEventTimeNanos(pos) / 1e6
            else event.getHistoricalEventTime(pos).toDouble()
    }
}
