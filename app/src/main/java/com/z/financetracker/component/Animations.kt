package com.z.financetracker.component

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.composed
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay

// ─────────────────────────────────────────────────────────────────────────────
// 1. FADE + SLIDE IN  — wrap any composable to animate it appearing on screen
//
//    Usage:
//      FadeSlideIn { YourCard() }
//      FadeSlideIn(delayMs = 200) { YourCard() }   // stagger multiple items
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun FadeSlideIn(
    delayMs: Int = 0,
    fromBottom: Boolean = true,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(400)) +
                slideInVertically(tween(400, easing = EaseOutCubic)) {
                    if (fromBottom) it / 2 else -it / 2
                }
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. COUNTER ANIMATION — animates a number counting up from 0
//
//    Usage:
//      AnimatedCounter(value = balance, prefix = "$")
//      AnimatedCounter(value = 1234.50, prefix = "HK$", decimalPlaces = 2)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedCounter(
    value: Double,
    durationMs: Int = 1200,
    content: @Composable (displayed: Double) -> Unit
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(durationMs, easing = EaseOutCubic),
        label = "counter"
    )
    content(animatedValue.toDouble())
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. SHIMMER LOADING — skeleton placeholder while data loads
//
//    Usage:
//      if (isLoading) ShimmerBox(height = 80.dp)
//      ShimmerBox(height = 20.dp, width = 120.dp)  // fixed width
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    width: Dp? = null,
    cornerRadius: Dp = 8.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "shimmer"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE2E8F0),
            Color(0xFFF8FAFC),
            Color(0xFFE2E8F0)
        ),
        start = Offset(shimmerOffset - 300f, 0f),
        end = Offset(shimmerOffset + 300f, 0f)
    )

    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .background(shimmerBrush, RoundedCornerShape(cornerRadius))
    )
}

// Shimmer card — shows a full card skeleton
@Composable
fun ShimmerCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ShimmerBox(height = 16.dp, width = 140.dp)
            ShimmerBox(height = 32.dp, width = 200.dp)
            Spacer(Modifier.height(4.dp))
            ShimmerBox(height = 12.dp)
            ShimmerBox(height = 12.dp, width = 180.dp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. PRESS SCALE — button/card shrinks slightly when pressed
//
//    Usage:
//      Box(modifier = Modifier.pressScale().clickable { ... }) { ... }
//      Card(modifier = Modifier.pressScale(0.97f)) { ... }
// ─────────────────────────────────────────────────────────────────────────────
fun Modifier.pressScale(
    targetScale: Float = 0.95f
): Modifier = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) targetScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale"
    )
    this
        .scale(scale)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {}
        )
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. PULSE — glowing pulse effect (great for balance total or alerts)
//
//    Usage:
//      Box(modifier = Modifier.pulse(color = Color(0xFF10B981))) { ... }
// ─────────────────────────────────────────────────────────────────────────────
fun Modifier.pulse(
    color: Color = Color(0xFF2563EB),
    durationMs: Int = 1500
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMs, easing = EaseInOutSine),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    this.drawBehind {
        drawCircle(
            color = color.copy(alpha = alpha * 0.25f),
            radius = size.minDimension / 2 + 12.dp.toPx()
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. BOUNCE IN — spring bounce when an item first appears
//
//    Usage:
//      BounceIn { Icon(...) }
//      BounceIn(delayMs = 300) { FloatingActionButton(...) }
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BounceIn(
    delayMs: Int = 0,
    content: @Composable () -> Unit
) {
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        triggered = true
    }
    val scale by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceIn"
    )
    Box(modifier = Modifier.scale(scale)) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. STAGGERED LIST — animates list items in one by one
//
//    Usage:
//      items.forEachIndexed { index, item ->
//          StaggeredItem(index = index) {
//              TransactionRow(item)
//          }
//      }
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun StaggeredItem(
    index: Int,
    staggerMs: Int = 60,
    content: @Composable () -> Unit
) {
    FadeSlideIn(delayMs = index * staggerMs) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. ANIMATED PROGRESS BAR — smooth fill animation
//
//    Usage:
//      AnimatedProgressBar(progress = 0.72f, color = Color(0xFF2563EB))
//      AnimatedProgressBar(progress = spent / budget, color = Color(0xFF10B981))
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF2563EB),
    backgroundColor: Color = Color(0xFFF1F5F9),
    height: Dp = 8.dp,
    cornerRadius: Dp = 4.dp,
    durationMs: Int = 900
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMs, easing = EaseOutCubic),
        label = "progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(backgroundColor, RoundedCornerShape(cornerRadius))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(height)
                .background(color, RoundedCornerShape(cornerRadius))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 9. SHAKE — shake a field to indicate an error
//
//    Usage:
//      var shakeError by remember { mutableStateOf(false) }
//      TextField(modifier = Modifier.shake(shakeError) { shakeError = false })
//      shakeError = true  // trigger it
// ─────────────────────────────────────────────────────────────────────────────
fun Modifier.shake(
    enabled: Boolean,
    onFinished: () -> Unit = {}
): Modifier = composed {
    val offsetX by animateFloatAsState(
        targetValue = if (enabled) 1f else 0f,
        animationSpec = if (enabled) keyframes {
            durationMillis = 500
            0f at 0
            -12f at 80
            12f at 160
            -10f at 240
            10f at 320
            -6f at 400
            0f at 500
        } else spring(),
        label = "shake",
        finishedListener = { if (enabled) onFinished() }
    )
    this.offset(x = offsetX.dp)
}

// ─────────────────────────────────────────────────────────────────────────────
// 10. TYPEWRITER TEXT — text appears character by character
//
//    Usage:
//      TypewriterText("Welcome to FinanceTracker", fontSize = 24.sp)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TypewriterText(
    text: String,
    delayPerChar: Long = 40L,
    content: @Composable (displayed: String) -> Unit
) {
    var displayed by remember { mutableStateOf("") }
    LaunchedEffect(text) {
        displayed = ""
        text.forEach { char ->
            delay(delayPerChar)
            displayed += char
        }
    }
    content(displayed)
}
