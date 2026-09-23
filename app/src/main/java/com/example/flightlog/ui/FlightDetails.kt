package com.example.flightlog.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier

fun displayAircraftNumber(raw: String): String {
    val value = raw.trim()
    if (value.isEmpty()) return "—"
    val number = value.replace(Regex("^(RA|РА)[\\s-]*", RegexOption.IGNORE_CASE), "")
    return if (number.isNotEmpty() && number.all { it.isDigit() }) "RA-$number" else value
}

@Composable
internal fun FlightTypeField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange,
        label = { Text("Тип полёта · необязательно") },
        placeholder = { Text("Пассажирский") },
        supportingText = { Text("Например: санитарный, тренировочный") },
        singleLine = true, modifier = Modifier.fillMaxWidth())
}
