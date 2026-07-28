package com.gallery.app.ui.vault.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VaultLockScreen(
    isPinConfigured: Boolean,
    isBiometricAvailable: Boolean,
    onPinSubmitted: (String) -> Unit,
    onBiometricClick: () -> Unit,
    errorMessage: String? = null
) {
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStage by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayTitle = when {
        !isPinConfigured && !isConfirmStage -> "Buat 4-Digit PIN Vault"
        !isPinConfigured && isConfirmStage -> "Konfirmasi PIN Vault"
        else -> "Masukkan PIN Vault"
    }

    val activeError = errorMessage ?: localError

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = displayTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // PIN Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentLength = enteredPin.length
                repeat(4) { index ->
                    val isFilled = index < currentLength
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (activeError != null) {
                Text(
                    text = activeError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Numpad 3x4
            val numbers = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("BIO", "0", "DEL")
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                numbers.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { item ->
                            when (item) {
                                "BIO" -> {
                                    if (isBiometricAvailable && isPinConfigured) {
                                        IconButton(
                                            onClick = onBiometricClick,
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Biometrik",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(64.dp))
                                    }
                                }

                                "DEL" -> {
                                    IconButton(
                                        onClick = {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                                localError = null
                                            }
                                        },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                }

                                else -> {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += item
                                                    localError = null

                                                    if (enteredPin.length == 4) {
                                                        if (!isPinConfigured) {
                                                            if (!isConfirmStage) {
                                                                confirmPin = enteredPin
                                                                enteredPin = ""
                                                                isConfirmStage = true
                                                            } else {
                                                                if (enteredPin == confirmPin) {
                                                                    onPinSubmitted(enteredPin)
                                                                } else {
                                                                    localError = "PIN tidak cocok. Ulangi."
                                                                    enteredPin = ""
                                                                    confirmPin = ""
                                                                    isConfirmStage = false
                                                                }
                                                            }
                                                        } else {
                                                            onPinSubmitted(enteredPin)
                                                        }
                                                    }
                                                }
                                            }
                                    ) {
                                        Text(
                                            text = item,
                                            fontSize = 24.sp,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
