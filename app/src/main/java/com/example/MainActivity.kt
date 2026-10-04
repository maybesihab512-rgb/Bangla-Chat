package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.CipherAppContainer
import com.example.ui.navigation.CipherNavHost
import com.example.ui.theme.CyberBgDark
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CipherAppContainer.initialize(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberBgDark
                ) {
                    CipherNavHost()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            CipherAppContainer.getInstance().authRepository.updatePresence(true)
        } catch (e: Exception) {
            // Ignore if container not yet ready
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            CipherAppContainer.getInstance().authRepository.updatePresence(false)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            CipherAppContainer.getInstance().authRepository.updatePresence(false)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
