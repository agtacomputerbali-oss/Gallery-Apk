package com.gallery.app.ui.permission

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PermissionScreen(
    viewModel: PermissionViewModel,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val requiredPermissions = viewModel.getRequiredPermissions()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val isAnyGranted = result.values.any { it }
        viewModel.onPermissionResult(
            isGranted = isAnyGranted,
            shouldShowRationale = false
        )
    }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionStatus(context)
    }

    LaunchedEffect(permissionState) {
        if (permissionState is PermissionState.Granted) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Izin Akses Foto Diperlukan",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Aplikasi Gallery memerlukan izin untuk membaca media foto di perangkat Anda agar dapat menampilkan album dan foto.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                permissionLauncher.launch(requiredPermissions)
            }
        ) {
            Text(text = "Izinkan Akses")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                context.startActivity(intent)
            }
        ) {
            Text(text = "Buka Pengaturan")
        }
    }
}
