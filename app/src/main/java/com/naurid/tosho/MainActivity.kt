package com.naurid.tosho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.naurid.tosho.ui.components.DynamicSideBar
import androidx.compose.foundation.isSystemInDarkTheme
import com.naurid.tosho.ui.theme.ToshoTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            ToshoTheme(darkTheme = isDarkTheme) {
                ToshoApp(
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

@Composable
fun ToshoApp(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
){
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    var selectedRoute by remember { mutableStateOf("Home") }

    val reducedThresholdDp = 64.dp
    val reducedThresholdPx = with(density) { reducedThresholdDp.toPx() }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val maxScreenWidthPx = constraints.maxWidth.toFloat()
        val drawerWidthAnim = remember { Animatable(0f) }
        val velocityTracker = remember { VelocityTracker() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(maxScreenWidthPx){
                    val edgeThresholdPx = maxScreenWidthPx / 2f

                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            velocityTracker.resetTracking()
                        },
                        onDragEnd = {
                            val velocity = velocityTracker.calculateVelocity().x
                            val current = drawerWidthAnim.value

                            scope.launch {
                                val targetWidth = when {
                                    // Fling Right: if below threshold, snap to threshold; if above, snap to full
                                    velocity > 1000f -> {
                                        if (current < reducedThresholdPx) reducedThresholdPx else maxScreenWidthPx
                                    }
                                    // Fling Left: if near full, snap to threshold; if near threshold, close
                                    velocity < -1000f -> {
                                        if (current > reducedThresholdPx * 1.5f) reducedThresholdPx else 0f
                                    }
                                    // Soft Release: snap to the nearest of the 3 points
                                    else -> {
                                        val midpoint1 = reducedThresholdPx / 2f
                                        val midpoint2 = (reducedThresholdPx + maxScreenWidthPx) / 2f

                                        when {
                                            current < midpoint1 -> 0f
                                            current < midpoint2 -> reducedThresholdPx
                                            else -> maxScreenWidthPx
                                        }
                                    }
                                }

                                drawerWidthAnim.animateTo(
                                    targetValue = targetWidth,
                                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            val isNearEdge = (change.position.x - dragAmount) <= edgeThresholdPx
                            val isAlreadyOpen = drawerWidthAnim.value > 0f

                            if (isNearEdge || isAlreadyOpen){
                                change.consume()
                                velocityTracker.addPosition(change.uptimeMillis, change.position)

                                scope.launch {
                                    val newWidth = (drawerWidthAnim.value + dragAmount)
                                        .coerceIn(0f, maxScreenWidthPx)
                                    drawerWidthAnim.snapTo(newWidth)
                                }
                            }
                        }
                    )
                }
        ) {
            val animatedScaffoldBg by androidx.compose.animation.animateColorAsState(
                targetValue = if (isDarkTheme) com.naurid.tosho.ui.theme.DarkBackground else com.naurid.tosho.ui.theme.LightBackground,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 400),
                label = "scaffoldBg"
            )
            val animatedTextColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (isDarkTheme) com.naurid.tosho.ui.theme.DarkTextPrimary else com.naurid.tosho.ui.theme.LightTextPrimary,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 400),
                label = "textColor"
            )

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = animatedScaffoldBg
            ) { innerPadding ->
                if (selectedRoute.equals("Home", ignoreCase = true) || selectedRoute.equals("Example1", ignoreCase = true)) {
                    com.naurid.tosho.ui.home.HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        isDarkTheme = isDarkTheme
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ){
                        Text(
                            text = selectedRoute.replaceFirstChar { it.uppercase() },
                            fontSize = 28.sp,
                            color = animatedTextColor
                        )
                    }
                }
            }

            val currentDrawerWidthDp = with(density) { drawerWidthAnim.value.toDp() }

            if (drawerWidthAnim.value > 0f){
                DynamicSideBar(
                    currentWidth = currentDrawerWidthDp,
                    selectedRoute = selectedRoute,
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    onNavigate = { route ->
                        selectedRoute = route

                        scope.launch {
                            drawerWidthAnim.animateTo(0f, spring(dampingRatio = .8f))
                        }
                    }
                )
            }
        }
    }
}
