package com.bunde.learning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.bunde.learning.ui.navigation.BundeNavHost
import com.bunde.learning.ui.theme.BundeBackground
import com.bunde.learning.ui.theme.BundeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BundeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BundeBackground
                ) {
                    BundeNavHost()
                }
            }
        }
    }
}
