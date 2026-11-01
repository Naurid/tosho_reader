package com.naurid.tosho.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naurid.tosho.ui.theme.DarkBackground
import com.naurid.tosho.ui.theme.DarkGlassBackground
import com.naurid.tosho.ui.theme.DarkTextPrimary
import com.naurid.tosho.ui.theme.LightBackground
import com.naurid.tosho.ui.theme.LightGlassBackground
import com.naurid.tosho.ui.theme.LightTextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SideBarItem(
    icon: ImageVector,
    title: String,
    currentWidth: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    itemIndex: Int = 0,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    glassBackground: Color = if (isDarkTheme) DarkGlassBackground else LightGlassBackground,
    iconRotation: Float = 0f,
    popOutOnClick: Boolean = true
){
    val scope = rememberCoroutineScope()

    val clickScale = remember { Animatable(1f) }
    val translateY = remember { Animatable(100f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        clickScale.snapTo(1f)
        delay(itemIndex * 35L)
        launch {
            alphaAnim.animateTo(1f, tween(200))
        }
        translateY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val defaultContent = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val selectedBg = if (isDarkTheme) DarkTextPrimary else LightTextPrimary
    val selectedContent = if (isDarkTheme) DarkBackground else LightBackground

    val insideColor = if (isSelected) selectedBg else Color.Transparent
    val contentColor = if (isSelected) selectedContent else defaultContent

    // The icon's mask matches the frosted glass background
    val iconMaskColor = if (isSelected) insideColor else glassBackground

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .graphicsLayer {
                scaleX = clickScale.value
                scaleY = clickScale.value
                translationY = translateY.value
                alpha = alphaAnim.value
            }
            .clickable {
                scope.launch {
                    if (popOutOnClick) {
                        clickScale.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(stiffness = Spring.StiffnessHigh)
                        )
                        onClick()
                        clickScale.snapTo(1f)
                    } else {
                        clickScale.animateTo(0.85f, spring(stiffness = Spring.StiffnessHigh))
                        onClick()
                        clickScale.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
                        )
                    }
                }
            },
        color = insideColor
    ) {
        Box(
            modifier = Modifier.padding(start = 10.dp),
            contentAlignment = Alignment.CenterStart
        ){
            UnsquishingLabel(
                icon = icon,
                text = title,
                currentWidth = currentWidth,
                contentColor = contentColor,

                iconRotation = iconRotation
            )
        }
    }
}
