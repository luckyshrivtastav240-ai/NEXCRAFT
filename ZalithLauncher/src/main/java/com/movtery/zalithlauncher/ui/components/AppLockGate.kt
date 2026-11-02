/*
 * NEXCRAFT security helpers.
 * Copyright (C) 2026 NEXCRAFT contributors.
 * Licensed under the GNU General Public License v3.0 or later.
 */
package com.movtery.zalithlauncher.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.AppLockStore

@Composable
fun AppLockGate(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    BackHandler(enabled = true) { }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(R.string.app_lock_gate_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.app_lock_gate_prompt))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 8 && it.all(Char::isDigit)) {
                            pin = it
                            error = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error,
                    label = { Text(stringResource(R.string.app_lock_current_pin)) }
                )
                if (error) Text(
                    stringResource(R.string.app_lock_incorrect),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (AppLockStore.verify(context, pin)) onUnlocked()
                    else error = true
                },
                enabled = pin.length in 4..8
            ) { Text(stringResource(R.string.app_lock_unlock)) }
        }
    )
}
