package com.naurid.tosho.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.naurid.tosho.ui.theme.DarkDivider
import com.naurid.tosho.ui.theme.DarkGlassBackground
import com.naurid.tosho.ui.theme.DarkGlassBorder
import com.naurid.tosho.ui.theme.LightDivider
import com.naurid.tosho.ui.theme.LightGlassBackground
import com.naurid.tosho.ui.theme.LightGlassBorder

@Composable
fun DynamicSideBar(
    currentWidth: Dp,
    selectedRoute: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
){
    // Translucent frosted glass background
    val animatedGlassBg by animateColorAsState(
        targetValue = if (isDarkTheme) DarkGlassBackground else LightGlassBackground,
        animationSpec = tween(durationMillis = 400),
        label = "glassBg"
    )

    // Glass reflection specular border on the trailing edge
    val animatedGlassBorder by animateColorAsState(
        targetValue = if (isDarkTheme) DarkGlassBorder else LightGlassBorder,
        animationSpec = tween(durationMillis = 400),
        label = "glassBorder"
    )

    val animatedDivider by animateColorAsState(
        targetValue = if (isDarkTheme) DarkDivider else LightDivider,
        animationSpec = tween(durationMillis = 400),
        label = "divider"
    )

    // Playful rotation animation when toggling theme
    val themeIconRotation by animateFloatAsState(
        targetValue = if (isDarkTheme) 360f else 0f,
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = Spring.StiffnessLow
        ),
        label = "themeIconRotation"
    )

    Column(
        modifier = modifier
            .width(currentWidth)
            .fillMaxHeight()
            .background(animatedGlassBg)
            .drawBehind {
                // Glass specular border on the right edge of the sliding bar
                drawLine(
                    color = animatedGlassBorder,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = 12.dp)
    ) {
        // TOP SECTION
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
        ) {
            val topItems = listOf("Home", "Example2", "Example3")

            topItems.forEachIndexed { index, title ->
                SideBarItem(
                    icon = bolt,
                    title = title,
                    currentWidth = currentWidth,
                    itemIndex = index,
                    isDarkTheme = isDarkTheme,
                    glassBackground = animatedGlassBg,
                    isSelected = selectedRoute == title,
                    onClick = { onNavigate(title) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            color = animatedDivider
        )

        // BOTTOM SECTION
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            val bottomItems = listOf("Local", "Downloaded", "Profile", "Settings")

            bottomItems.forEachIndexed { index, title ->
                SideBarItem(
                    icon = bolt,
                    title = title,
                    currentWidth = currentWidth,
                    itemIndex = index + 3,
                    isDarkTheme = isDarkTheme,
                    glassBackground = animatedGlassBg,
                    isSelected = selectedRoute.equals(title, ignoreCase = true),
                    onClick = { onNavigate(title.lowercase()) }
                )
            }

            // Theme Toggle
            SideBarItem(
                icon = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                title = if (isDarkTheme) "Light" else "Dark",
                currentWidth = currentWidth,
                itemIndex = bottomItems.size + 3,
                isDarkTheme = isDarkTheme,
                glassBackground = animatedGlassBg,
                iconRotation = themeIconRotation,
                isSelected = false,
                popOutOnClick = false,
                onClick = onToggleTheme
            )
        }
    }
}
