package com.z.financetracker.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.*

// ─────────────────────────────────────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────────────────────────────────────
enum class ToastType { SUCCESS, DELETE, ERROR, INFO, WARNING }

data class ToastData(
    val message: String,
    val type: ToastType,
    val durationMs: Long = 2200L
)

// ─────────────────────────────────────────────────────────────────────────────
// State
// ─────────────────────────────────────────────────────────────────────────────
class ToastState {
    var current by mutableStateOf<ToastData?>(null)
        private set

    suspend fun show(message: String, type: ToastType, durationMs: Long = 2200L) {
        current = ToastData(message, type, durationMs)
        delay(durationMs)
        current = null
    }

    suspend fun showSuccess(message: String)  = show(message, ToastType.SUCCESS, 2200L)
    suspend fun showDelete(message: String)   = show(message, ToastType.DELETE,  2000L)
    suspend fun showError(message: String)    = show(message, ToastType.ERROR,   2800L)
    suspend fun showInfo(message: String)     = show(message, ToastType.INFO,    2200L)
    suspend fun showWarning(message: String)  = show(message, ToastType.WARNING, 2400L)

    fun dismiss() { current = null }
}

@Composable
fun rememberToastState() = remember { ToastState() }

// ─────────────────────────────────────────────────────────────────────────────
// Host — place inside a BoxScope:
//   Box(Modifier.fillMaxSize()) { Screen(); ToastHost(toast) }
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BoxScope.ToastHost(state: ToastState) {
    AnimatedVisibility(
        visible = state.current != null,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 54.dp)
            .zIndex(100f),
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium
            ),
            initialScale = 0.55f
        ) + fadeIn(tween(120)),
        exit = scaleOut(tween(180), targetScale = 0.75f) + fadeOut(tween(150))
    ) {
        state.current?.let { ToastPill(it) { state.dismiss() } }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Pill card
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ToastPill(toast: ToastData, onDismiss: () -> Unit) {
    val color = toastColor(toast.type)

    Box(
        modifier = Modifier
            .shadow(20.dp, RoundedCornerShape(50.dp), ambientColor = color.copy(.25f), spotColor = color.copy(.3f))
            .background(Color(0xFF18181B), RoundedCornerShape(50.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() }
            .padding(horizontal = 20.dp, vertical = 11.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LottieIcon(type = toast.type, color = color, sizeDp = 26.dp)
            Text(
                toast.message,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lottie-style animated icon (canvas-drawn circle + mark)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun LottieIcon(type: ToastType, color: Color, sizeDp: Dp) {
    // Single 0→1 driver for the whole animation
    val phase = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        phase.animateTo(1f, tween(660, easing = EaseOutCubic))
    }

    // Circle draws in during [0, 0.55], mark draws in during [0.55, 1]
    val circleP = (phase.value / 0.55f).coerceIn(0f, 1f)
    val markP   = ((phase.value - 0.55f) / 0.45f).coerceIn(0f, 1f)

    androidx.compose.foundation.Canvas(modifier = Modifier.size(sizeDp)) {
        val sw = size.minDimension / 9.5f        // stroke width
        val r  = size.minDimension / 2f - sw      // circle radius
        val cx = size.width  / 2f
        val cy = size.height / 2f

        // ── animated circle arc ───────────────────────────────────
        drawArc(
            color      = color,
            startAngle = -90f,
            sweepAngle = 360f * circleP,
            useCenter  = false,
            style      = Stroke(sw, cap = StrokeCap.Round),
            topLeft    = Offset(cx - r, cy - r),
            size       = Size(r * 2f, r * 2f)
        )

        // ── mark inside circle ────────────────────────────────────
        if (markP > 0f) {
            when (type) {
                ToastType.SUCCESS             -> drawAnimatedCheck(cx, cy, r, color, sw, markP)
                ToastType.DELETE,
                ToastType.ERROR               -> drawAnimatedX(cx, cy, r, color, sw, markP)
                ToastType.INFO                -> drawAnimatedI(cx, cy, r, color, sw, markP)
                ToastType.WARNING             -> drawAnimatedBang(cx, cy, r, color, sw, markP)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DrawScope helpers — each animates a mark from progress 0→1
// ─────────────────────────────────────────────────────────────────────────────

/** Classic two-segment checkmark */
private fun DrawScope.drawAnimatedCheck(
    cx: Float, cy: Float, r: Float, color: Color, sw: Float, progress: Float
) {
    val p1 = Offset(cx - r * 0.48f, cy + r * 0.02f)
    val p2 = Offset(cx - r * 0.08f, cy + r * 0.44f)
    val p3 = Offset(cx + r * 0.52f, cy - r * 0.42f)
    drawSegmentedPath(listOf(p1, p2, p3), color, sw, progress)
}

/** Two crossing diagonals */
private fun DrawScope.drawAnimatedX(
    cx: Float, cy: Float, r: Float, color: Color, sw: Float, progress: Float
) {
    val h = r * 0.42f
    // first diagonal 0→0.5, second 0.5→1
    val p1 = progress / 0.5f
    val p2 = ((progress - 0.5f) / 0.5f).coerceIn(0f, 1f)
    val a = Offset(cx - h, cy - h); val b = Offset(cx + h, cy + h)
    val c = Offset(cx + h, cy - h); val d = Offset(cx - h, cy + h)
    drawLine(color, a, lerp(a, b, p1.coerceIn(0f, 1f)), sw, StrokeCap.Round)
    if (p2 > 0f) drawLine(color, c, lerp(c, d, p2), sw, StrokeCap.Round)
}

/** Dot + vertical bar = "i" */
private fun DrawScope.drawAnimatedI(
    cx: Float, cy: Float, r: Float, color: Color, sw: Float, progress: Float
) {
    // dot appears first
    if (progress > 0.3f) drawCircle(color, sw * 0.65f, Offset(cx, cy - r * 0.32f))
    val barProgress = ((progress - 0.3f) / 0.7f).coerceIn(0f, 1f)
    if (barProgress > 0f) {
        val top = Offset(cx, cy - r * 0.05f)
        val bot = Offset(cx, cy + r * 0.46f)
        drawLine(color, top, lerp(top, bot, barProgress), sw, StrokeCap.Round)
    }
}

/** Vertical bar + dot = "!" */
private fun DrawScope.drawAnimatedBang(
    cx: Float, cy: Float, r: Float, color: Color, sw: Float, progress: Float
) {
    val top = Offset(cx, cy - r * 0.46f)
    val bot = Offset(cx, cy + r * 0.08f)
    drawLine(color, top, lerp(top, bot, (progress / 0.75f).coerceIn(0f, 1f)), sw, StrokeCap.Round)
    if (progress > 0.8f) drawCircle(color, sw * 0.65f, Offset(cx, cy + r * 0.38f))
}

/** Walk along a polyline, drawing only up to `progress` of its total length */
private fun DrawScope.drawSegmentedPath(
    pts: List<Offset>, color: Color, sw: Float, progress: Float
) {
    if (pts.size < 2) return
    val segs = (0 until pts.size - 1).map { hypot(pts[it + 1].x - pts[it].x, pts[it + 1].y - pts[it].y) }
    val total = segs.sum()
    var remaining = progress * total
    segs.forEachIndexed { i, len ->
        if (remaining <= 0f) return
        val from = pts[i]
        val to   = pts[i + 1]
        val t    = (remaining / len).coerceIn(0f, 1f)
        drawLine(color, from, lerp(from, to, t), sw, StrokeCap.Round)
        remaining -= len
    }
}

private fun lerp(a: Offset, b: Offset, t: Float) =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

// ─────────────────────────────────────────────────────────────────────────────
// Color per type
// ─────────────────────────────────────────────────────────────────────────────
private fun toastColor(type: ToastType) = when (type) {
    ToastType.SUCCESS -> Color(0xFF10B981)
    ToastType.DELETE  -> Color(0xFFEF4444)
    ToastType.ERROR   -> Color(0xFFEF4444)
    ToastType.INFO    -> Color(0xFF3B82F6)
    ToastType.WARNING -> Color(0xFFF59E0B)
}
