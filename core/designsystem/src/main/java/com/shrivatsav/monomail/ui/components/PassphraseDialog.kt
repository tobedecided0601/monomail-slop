package com.shrivatsav.monomail.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Asks for a passphrase. With [confirm] it asks twice and requires a minimum length,
 * for creating a backup; without it, it is a single field for unlocking one.
 */
@Composable
fun PassphraseDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirm: Boolean,
    onSubmit: (CharArray) -> Unit,
    onDismiss: () -> Unit
) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    val tooShort = confirm && first.isNotEmpty() && first.length < MIN_LENGTH
    val mismatch = confirm && second.isNotEmpty() && first != second
    val valid = if (confirm) first.length >= MIN_LENGTH && first == second else first.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(message)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = tooShort,
                    supportingText = if (tooShort) ({ Text("At least $MIN_LENGTH characters") }) else null
                )
                if (confirm) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = second,
                        onValueChange = { second = it },
                        label = { Text("Repeat passphrase") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = mismatch,
                        supportingText = if (mismatch) ({ Text("Passphrases do not match") }) else null
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = { onSubmit(first.toCharArray()) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private const val MIN_LENGTH = 8
