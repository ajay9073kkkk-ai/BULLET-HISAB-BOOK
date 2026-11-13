package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LedgerNumberFont

@Composable
fun PinLockScreen(
    onVerifyPin: (String) -> Boolean,
    modifier: Modifier = Modifier
) {
    var entered by remember { mutableStateOf("") }
    var wrongPinError by remember { mutableStateOf(false) }

    fun appendDigit(digit: String) {
        if (entered.length < 4) {
            val next = entered + digit
            entered = next
            wrongPinError = false
            if (next.length == 4) {
                val ok = onVerifyPin(next)
                if (!ok) {
                    wrongPinError = true
                    entered = ""
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .bulletGridBackground()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 340.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "ऐप लॉक है",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bullet Hisab Book",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "आगे बढ़ने के लिए 4-अंकीय पिन दर्ज करें",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < entered.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(
                                    color = if (filled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    shape = CircleShape
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (wrongPinError) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                if (wrongPinError) {
                    Text(
                        text = "गलत पिन! कृपया दोबारा प्रयास करें",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("wrong_pin_error")
                    )
                }

                // Numeric keypad
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowDigits ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            rowDigits.forEach { d ->
                                PinDigitButton(
                                    digit = d,
                                    onClick = { appendDigit(d) }
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.size(72.dp))
                        PinDigitButton(
                            digit = "0",
                            onClick = { appendDigit("0") }
                        )
                        IconButton(
                            onClick = {
                                if (entered.isNotEmpty()) {
                                    entered = entered.dropLast(1)
                                }
                            },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "मिटाएं"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinDigitButton(
    digit: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier
            .size(72.dp)
            .testTag("pin_key_$digit")
    ) {
        Text(
            text = digit,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = LedgerNumberFont,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
