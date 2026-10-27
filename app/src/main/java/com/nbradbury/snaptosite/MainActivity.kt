package com.nbradbury.snaptosite

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.nbradbury.snaptosite.ui.SnapApp
import com.nbradbury.snaptosite.ui.SnapTheme
import com.nbradbury.snaptosite.wp.AppPasswordAuth

class MainActivity : ComponentActivity() {
    private val viewModel: SnapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) handleAuthCallback(intent)
        setContent { SnapTheme { SnapApp(viewModel) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthCallback(intent)
    }

    private fun handleAuthCallback(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.toString().startsWith(AppPasswordAuth.CALLBACK)) viewModel.onAuthCallback(uri)
    }
}
