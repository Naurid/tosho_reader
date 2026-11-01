package com.naurid.tosho.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun UnsquishingLabel(
    icon: ImageVector,
    text: String,
    currentWidth: Dp,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = Color.Transparent,
    iconRotation: Float = 0f,
    reducedThreshold: Dp = 64.dp,
    fullThreshold: Dp = 190.dp
){
    if (text.isEmpty()) return

    val firstProgress = (currentWidth / reducedThreshold).coerceIn(0f, 1f)

    val lastProgress = if (currentWidth > reducedThreshold) {
        ((currentWidth - reducedThreshold) / (fullThreshold - reducedThreshold)).coerceIn(0f, 1f)
    } else 0f

    val font = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = (-1.5).sp,
        color = contentColor
    )

    val iconBoxWidth = 36.dp
    val textGap = 8.dp

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // Words: layer 1, slides out from physically BEHIND the icon
        if (lastProgress > 0f) {
            Box(
                modifier = Modifier
                    .zIndex(1f)
                    .padding(start = iconBoxWidth + textGap)
                    .clipToBounds()
            ) {
                Text(
                    text = text.uppercase(),
                    style = font,
                    maxLines = 1,
                    softWrap = false,
                    color = contentColor,
                    modifier = Modifier.graphicsLayer {
                        // Starts tucked under the icon and slides out to its resting place
                        translationX = -(1f - lastProgress) * (iconBoxWidth + textGap).toPx()
                        alpha = lastProgress
                    }
                )
            }
        }

        // Icon: layer 2 with opaque background, masks words as they emerge from underneath
        Box(
            modifier = Modifier
                .zIndex(2f)
                .size(iconBoxWidth)
                .background(backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier
                    .size(34.dp) // Fills the height of the item
                    .graphicsLayer(
                        scaleX = 0.25f + (0.75f * firstProgress),
                        rotationZ = iconRotation,
                        transformOrigin = TransformOrigin(0.5f, 0.5f)
                    )
            )
        }
    }
}
