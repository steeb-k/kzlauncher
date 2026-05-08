package app.olauncher.helper

import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.View
import android.widget.TextView

private val RAINBOW_COLORS = intArrayOf(
    0xFFFF3B30.toInt(),
    0xFFFF9500.toInt(),
    0xFFFFD60A.toInt(),
    0xFF34C759.toInt(),
    0xFF0A84FF.toInt(),
    0xFF5856D6.toInt(),
    0xFFFF3B30.toInt(),
    0xFFFF9500.toInt(),
    0xFFFFD60A.toInt(),
    0xFF34C759.toInt(),
    0xFF0A84FF.toInt(),
    0xFF5856D6.toInt()
)

private val RAINBOW_POSITIONS = floatArrayOf(
    0f,
    0.1f,
    0.2f,
    0.3f,
    0.4f,
    0.5f,
    0.6f,
    0.7f,
    0.8f,
    0.9f,
    0.95f,
    1f
)

private data class RainbowFrame(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

private fun resolveRainbowFrame(rootView: View, target: View): RainbowFrame {
    val rootLocation = IntArray(2)
    val targetLocation = IntArray(2)
    rootView.getLocationOnScreen(rootLocation)
    target.getLocationOnScreen(targetLocation)

    val rootWidth = if (rootView.width > 0) rootView.width else rootView.resources.displayMetrics.widthPixels
    val rootHeight = if (rootView.height > 0) rootView.height else rootView.resources.displayMetrics.heightPixels

    return RainbowFrame(
        startX = (rootLocation[0] - targetLocation[0]).toFloat(),
        startY = (rootLocation[1] - targetLocation[1]).toFloat(),
        endX = (rootLocation[0] + rootWidth - targetLocation[0]).toFloat(),
        endY = (rootLocation[1] + rootHeight - targetLocation[1]).toFloat()
    )
}

fun TextView.applyRainbowShader(rootView: View) {
    val frame = resolveRainbowFrame(rootView, this)
    paint.shader = LinearGradient(
        frame.startX,
        frame.startY,
        frame.endX,
        frame.endY,
        RAINBOW_COLORS,
        RAINBOW_POSITIONS,
        Shader.TileMode.CLAMP
    )
    invalidate()
}

fun TextView.clearRainbowShader() {
    if (paint.shader != null) {
        paint.shader = null
        invalidate()
    }
}
