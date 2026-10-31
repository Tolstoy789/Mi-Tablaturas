package com.tolstoy.mitablaturas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tolstoy.mitablaturas.ui.MiTablaturasRoot
import com.tolstoy.mitablaturas.ui.theme.MiTablaturasTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiTablaturasTheme {
                MiTablaturasRoot()
            }
        }
    }
}
