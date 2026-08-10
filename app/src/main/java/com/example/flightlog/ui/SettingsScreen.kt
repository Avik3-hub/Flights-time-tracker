package com.example.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flightlog.data.db.TariffEntity

@Composable
fun SettingsScreen(
    currentTariff: TariffEntity,
    onSaveTariff: (TariffEntity) -> Unit
) {
    var landRate by remember(currentTariff) { mutableStateOf(currentTariff.landHourlyRate.toString()) }
    var seaRate by remember(currentTariff) { mutableStateOf(currentTariff.seaHourlyRate.toString()) }
    var dutyRate by remember(currentTariff) { mutableStateOf(currentTariff.dutyDayRate.toString()) }
    var flightDayRate by remember(currentTariff) { mutableStateOf(currentTariff.flightDayRate.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Настройки тарифов", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = landRate,
            onValueChange = { landRate = it },
            label = { Text("Оплата за час (земля), руб.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = seaRate,
            onValueChange = { seaRate = it },
            label = { Text("Оплата за час (море), руб.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = dutyRate,
            onValueChange = { dutyRate = it },
            label = { Text("Оплата за день дежурства, руб.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = flightDayRate,
            onValueChange = { flightDayRate = it },
            label = { Text("Оплата за полетный день, руб.") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val updatedTariff = TariffEntity(
                    id = 1,
                    landHourlyRate = landRate.toDoubleOrNull() ?: 0.0,
                    seaHourlyRate = seaRate.toDoubleOrNull() ?: 0.0,
                    dutyDayRate = dutyRate.toDoubleOrNull() ?: 0.0,
                    flightDayRate = flightDayRate.toDoubleOrNull() ?: 0.0
                )
                onSaveTariff(updatedTariff)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить тарифы")
        }
    }
}
