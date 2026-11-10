package com.example.parentapp.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.parentapp.core.designsystem.theme.ParentsAppTheme
import com.example.parentapp.core.designsystem.theme.StatusColors

/** Semantic tone for a [StatusBadge]. Maps to the theme's extended status colours. */
enum class BadgeTone { SUCCESS, WARNING, DANGER, INFO, NEUTRAL }

@Composable
fun StatusBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
) {
    val colors: StatusColors = with(ParentsAppTheme.extendedColors) {
        when (tone) {
            BadgeTone.SUCCESS -> success
            BadgeTone.WARNING -> warning
            BadgeTone.DANGER -> danger
            BadgeTone.INFO -> info
            BadgeTone.NEUTRAL -> neutral
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.onContainer,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(colors.container)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
