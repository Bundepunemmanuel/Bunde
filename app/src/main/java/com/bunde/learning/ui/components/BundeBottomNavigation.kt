package com.bunde.learning.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bunde.learning.ui.navigation.Screen
import com.bunde.learning.ui.theme.BundeAccent
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeBorder
import com.bunde.learning.ui.theme.BundeTextSecondary

sealed class NavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : NavItem(Screen.Home.route, "Home", Icons.Outlined.Home)
    object MyLearning : NavItem(Screen.MyLearning.route, "My Learning", Icons.Outlined.AutoStories)
    object Revision : NavItem(Screen.Revision.route, "Revision", Icons.Outlined.Sync)
    object Profile : NavItem(Screen.Profile.route, "Profile", Icons.Outlined.Person)
}

@Composable
fun BundeBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        NavItem.Home,
        NavItem.MyLearning,
        NavItem.Revision,
        NavItem.Profile
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BundeBackground)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(BundeBorder)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val color = if (isSelected) BundeAccent else BundeTextSecondary
                val fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (!isSelected) {
                                onNavigate(item.route)
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = fontWeight,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
