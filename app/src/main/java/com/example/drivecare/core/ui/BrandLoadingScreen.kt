package com.example.drivecare.core.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.drivecare.R

@Composable
fun BrandLoadingScreen(error: Int? = null, onRetry: () -> Unit = {}) {
    val transition = rememberInfiniteTransition(label = "DriveCare loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "Progress orbit"
    )
    val breath by transition.animateFloat(
        initialValue = .96f,
        targetValue = 1.03f,
        animationSpec =
            infiniteRepeatable(
                tween(1200, easing = FastOutSlowInEasing),
                RepeatMode.Reverse
            ),
        label = "Logo breathing"
    )
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        Modifier.fillMaxSize().background(colors.background).background(
            Brush.radialGradient(
                listOf(colors.primaryContainer.copy(alpha = .65f), colors.background),
                radius = 1100f
            )
        ).safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        val logoSize = if (maxHeight < 400.dp) 128.dp else 184.dp
        Column(
            Modifier.widthIn(
                max = 460.dp
            ).fillMaxWidth().verticalScroll(rememberScrollState()).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(logoSize),
                contentAlignment = Alignment.Center
            ) {
                if (error ==
                    null
                ) {
                    Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
                        val inset = 6.dp.toPx()
                        drawCircle(
                            colors.primary.copy(alpha = .08f),
                            radius =
                                size.minDimension / 2 - inset,
                            style = Stroke(2.dp.toPx())
                        )
                        drawArc(
                            colors.primary.copy(alpha = .65f),
                            startAngle = -80f,
                            sweepAngle = 74f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size =
                                size.copy(
                                    width =
                                        size.width - 2 * inset,
                                    height = size.height - 2 * inset
                                ),
                            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                Image(
                    painterResource(R.drawable.drivecare_logo),
                    contentDescription = null,
                    modifier =
                        Modifier.fillMaxSize(.66f).graphicsLayer {
                            scaleX = breath
                            scaleY =
                                breath
                        }
                )
            }
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                color = colors.onBackground
            )
            Text(
                stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )
            if (error != null) {
                Text(stringResource(error), color = colors.error)
                Button(onClick = onRetry) { Text(stringResource(R.string.retry_loading)) }
            } else {
                Text(
                    stringResource(R.string.brand_loading),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )
            }
        }
    }
}
