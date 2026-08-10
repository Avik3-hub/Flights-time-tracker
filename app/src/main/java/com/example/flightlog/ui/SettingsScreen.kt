package com.example.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flightlog.data.db.TariffEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    tariff: TariffEntity,
    onSaveTariff: (TariffEntity) -> Unit
) {
    var landRateText by remember(tariff) { mutableStateOf(tariff.landHourRate.toString()) }
    var seaRateText by remember(tariff) { mutableStateOf(tariff.seaHourRate.toString()) }
    var dutyRateText by remember(tariff) { mutableStateOf(tariff.dutyDayRate.toString()) }
    var flightDayRateText by remember(tariff) { mutableStateOf(tariff.flightDayRate.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тарифы и настройки", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = landRateText,
                onValueChange = { landRateText = it },
                label = { Text("Тариф за час (Земля), ₽") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = seaRateText,
                onValueChange = { seaRateText = it },
                label = { Text("Тариф за час (Море), ₽") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = dutyRateText,
                onValueChange = { dutyRateText = it },
                label = { Text("Тариф за день дежурства / Варандей, ₽") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = flightDayRateText,
                onValueChange = { flightDayRateText = it },
                label = { Text("Тариф за полетный день, ₽") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    val updatedTariff = tariff.copy(
                        landHourRate = landRateText.toDoubleOrNull() ?: 0.0,
                        seaHourRate = seaRateText.toDoubleOrNull() ?: 0.0,
                        dutyDayRate = dutyRateText.toDoubleOrNull() ?: 0.0,
                        flightDayRate = flightDayRateText.toDoubleOrNull() ?: 0.0
                    )
                    onSaveTariff(updatedTariff)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить тарифы")
            }
        }
    }
}
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Фиксированные выплаты (руб/день)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = flightDayRate,
                        onValueChange = { flightDayRate = it },
                        label = { Text("Полетный день (руб)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = dutyRate,
                        onValueChange = { dutyRate = it },
                        label = { Text("Дежурство (руб)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }

            Button(
                onClick = {
                    val updated = currentTariff.copy(
                        landHourRate = landRate.toDoubleOrNull() ?: currentTariff.landHourRate,
                        seaHourRate = seaRate.toDoubleOrNull() ?: currentTariff.seaHourRate,
                        dutyDayRate = dutyRate.toDoubleOrNull() ?: currentTariff.dutyDayRate,
                        flightDayRate = flightDayRate.toDoubleOrNull() ?: currentTariff.flightDayRate
                    )
                    onSaveTariff(updated)
                    isSavedShow = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить тарифы")
            }

            if (isSavedShow) {
                Text(
                    text = "Тарифы успешно обновлены!",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
