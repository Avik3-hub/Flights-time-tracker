package com.example.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
// ВАЖНО: Делегаты getValue и setValue решают ошибки Property delegate и OutlinedTextField!
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.flightlog.data.db.TariffEntity

@Composable
fun SettingsScreen(
    currentTariff: TariffEntity,
    onSaveTariff: (TariffEntity) -> Unit
) {
    var landRateText by remember(currentTariff) { mutableStateOf(currentTariff.landHourlyRate.toString()) }
    var seaRateText by remember(currentTariff) { mutableStateOf(currentTariff.seaHourlyRate.toString()) }
    var dutyRateText by remember(currentTariff) { mutableStateOf(currentTariff.dutyDayRate.toString()) }
    var flightDayRateText by remember(currentTariff) { mutableStateOf(currentTariff.flightDayRate.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Настройка тарифов",
            style = MaterialTheme.typography.headlineMedium
        )

        OutlinedTextField(
            value = landRateText,
            onValueChange = { landRateText = it },
            label = { Text("Ставка за час (Земля), руб.") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = seaRateText,
            onValueChange = { seaRateText = it },
            label = { Text("Ставка за час (Море), руб.") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = dutyRateText,
            onValueChange = { dutyRateText = it },
            label = { Text("Ставка за день дежурства, руб.") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = flightDayRateText,
            onValueChange = { flightDayRateText = it },
            label = { Text("Ставка за полетный день, руб.") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                val newTariff = TariffEntity(
                    id = 1,
                    landHourlyRate = landRateText.toDoubleOrNull() ?: currentTariff.landHourlyRate,
                    seaHourlyRate = seaRateText.toDoubleOrNull() ?: currentTariff.seaHourlyRate,
                    dutyDayRate = dutyRateText.toDoubleOrNull() ?: currentTariff.dutyDayRate,
                    flightDayRate = flightDayRateText.toDoubleOrNull() ?: currentTariff.flightDayRate
                )
                onSaveTariff(newTariff)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Сохранить тарифы")
        }
    }
}
