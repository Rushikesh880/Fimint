package com.moneytracker.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.moneytracker.app.data.AppDatabase
import com.moneytracker.app.ui.DashboardScreen
import com.moneytracker.app.ui.DashboardViewModel
import com.moneytracker.app.ui.DashboardViewModelFactory

class MainActivity : ComponentActivity() {

    private val db by lazy { AppDatabase.getDatabase(this) }
    private val viewModel: DashboardViewModel by viewModels {
        DashboardViewModelFactory(db.transactionDao())
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val readGranted = permissions[Manifest.permission.READ_SMS] ?: false

        if (receiveGranted && readGranted) {
            Toast.makeText(this, "SMS Permissions Granted!", Toast.LENGTH_SHORT).show()
            viewModel.scanInbox(this)
        } else {
            Toast.makeText(
                this,
                "SMS Permissions are required to parse money transactions automatically.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = androidx.compose.ui.graphics.Color(0xFF10B981),
                    secondary = androidx.compose.ui.graphics.Color(0xFF6366F1),
                    background = androidx.compose.ui.graphics.Color(0xFF090D16),
                    surface = androidx.compose.ui.graphics.Color(0xFF121826),
                    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A2234),
                    error = androidx.compose.ui.graphics.Color(0xFFF43F5E),
                    onBackground = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
                    onSurface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
                    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF94A3B8)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val hasReceiveSms = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val hasReadSms = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasReceiveSms || !hasReadSms) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS
                )
            )
        }
    }
}
