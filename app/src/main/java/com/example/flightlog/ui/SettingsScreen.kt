package com.example.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
    onSaveTariff: (TariffEntity) -> Unit,
    onBackClick: () -> Unit
) {
    var landRate by remember(currentTariff) { mutableStateOf(currentTariff.landHourRate.toString()) }
    var seaRate by remember(currentTariff) { mutableStateOf(currentTariff.seaHourRate.toString()) }
    var dutyRate by remember(currentTariff) { mutableStateOf(currentTariff.dutyDayRate.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки тарифов") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
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
                value = landRate,
                onValueChange = { landRate = it },
                label = { Text("Оплата за час (земля), руб.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = seaRate,
                onValueChange = { seaRate = it },
                label = { Text("Оплата за час (море), руб.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = dutyRate,
                onValueChange = { dutyRate = it },
                label = { Text("Оплата за день дежурства, руб.") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Text(
                text = "Примечание: все расчеты выполняются с автовычетом 13% НДФЛ.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val updated = currentTariff.copy(
                        landHourRate = landRate.toDoubleOrNull() ?: currentTariff.landHourRate,
                        seaHourRate = seaRate.toDoubleOrNull() ?: currentTariff.seaHourRate,
                        dutyDayRate = dutyRate.toDoubleOrNull() ?: currentTariff.dutyDayRate
                    )
                    onSaveTariff(updated)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить тарифы", fontWeight = FontWeight.Bold)
            }
        }
    }
}
