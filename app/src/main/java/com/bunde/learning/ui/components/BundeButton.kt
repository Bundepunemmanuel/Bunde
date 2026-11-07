package com.bunde.learning.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.ui.theme.BundeAccent
import com.bunde.learning.ui.theme.BundeBorder
import com.bunde.learning.ui.theme.BundeSurface
import com.bunde.learning.ui.theme.BundeTextPrimary

@Composable
fun BundePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BundeAccent,
            contentColor = BundeSurface,
            disabledContainerColor = BundeBorder,
            disabledContentColor = BundeSurface
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun BundeSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = BundeTextPrimary
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BundeBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = BundeSurface,
            contentColor = textColor
        )
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
