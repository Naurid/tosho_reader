package com.naurid.tosho.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

val ExpressiveSquareShape = RoundedCornerShape(8.dp)

class SlantedRoundedCornerShape(
    private val slantOffsetDp: Dp = 10.dp,
    private val cornerRadiusDp: Dp = 8.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val slant = with(density) { slantOffsetDp.toPx() }
        val r = with(density) { cornerRadiusDp.toPx() }
        val w = size.width
        val h = size.height

        val path = Path().apply {
            // Top-left corner
            moveTo(slant + r, 0f)
            // Top edge
            lineTo(w - r, 0f)
            // Top-right corner
            quadraticTo(w, 0f, w - (slant * (r / h)), r)
            // Right slanted edge
            lineTo(w - slant + (slant * (r / h)), h - r)
            // Bottom-right corner
            quadraticTo(w - slant, h, w - slant - r, h)
            // Bottom edge
            lineTo(r, h)
            // Bottom-left corner
            quadraticTo(0f, h, slant * (r / h), h - r)
            // Left slanted edge
            lineTo(slant - (slant * (r / h)), r)
            // Close back to top-left
            quadraticTo(slant, 0f, slant + r, 0f)
            close()
        }
        return Outline.Generic(path)
    }
}

val ExpressiveSlantedShape = SlantedRoundedCornerShape(slantOffsetDp = 10.dp, cornerRadiusDp = 8.dp)