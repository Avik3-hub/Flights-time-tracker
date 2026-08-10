package com.example.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flightlog.data.db.TariffEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTariff: TariffEntity,
    onSaveTariff: (TariffEntity) -> Unit
) {
    var landRate by remember(currentTariff) { mutableStateOf(currentTariff.landHourRate.toString()) }
    var seaRate by remember(currentTariff) { mutableStateOf(currentTariff.seaHourRate.toString()) }
    var dutyRate by remember(currentTariff) { mutableStateOf(currentTariff.dutyDayRate.toString()) }
    var flightDayRate by remember(currentTariff) { mutableStateOf(currentTariff.flightDayRate.toString()) }

    var isSavedShow by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки тарифов", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ставки за налет (руб/час)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = landRate,
                        onValueChange = { landRate = it },
                        label = { Text("Час Земля (руб)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = seaRate,
                        onValueChange = { seaRate = it },
                        label = { Text("Час Море (руб)") },
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
