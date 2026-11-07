package com.bunde.learning.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bunde.learning.ui.theme.BundeBorder
import com.bunde.learning.ui.theme.BundeSurface

@Composable
fun BundeCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = BundeSurface,
    borderColor: Color = BundeBorder,
    borderWidth: Dp = 1.dp,
    cornerRadius: Dp = 12.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = backgroundColor,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = 0.dp // 0dp elevation avoids GPU overdraw on 2GB RAM devices
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}
