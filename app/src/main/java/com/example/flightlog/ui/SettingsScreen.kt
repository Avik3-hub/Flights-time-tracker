package com.example.flightlog.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.data.export.ExcelExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTariff: TariffEntity,
    flights: List<FlightEntity>,
    dutyRecord: DutyEntity?,
    onSaveTariff: (TariffEntity) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    var effectiveYear by remember { mutableIntStateOf(currentTariff.effectiveFromYear) }
    var effectiveMonth by remember { mutableIntStateOf(currentTariff.effectiveFromMonth) }

    var landRate by remember(currentTariff) { mutableStateOf(currentTariff.landHourlyRate.toString()) }
    var seaRate by remember(currentTariff) { mutableStateOf(currentTariff.seaHourlyRate.toString()) }
    var dutyRate by remember(currentTariff) { mutableStateOf(currentTariff.dutyDayRate.toString()) }

    val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
) { uri ->
    uri?.let {
        val success = ExcelExporter.exportToExcel(
            context = context,
            uri = it,
            flights = flights,
            duty = dutyRecord,
            tariff = currentTariff
        )
        if (success) {
            Toast.makeText(context, "Отчет сохранен в XLSX!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Ошибка при сохранении", Toast.LENGTH_LONG).show()
        }
    }
}

Button(
    onClick = { 
        exportLauncher.launch("Отчет_налет_${effectiveYear}_${effectiveMonth}.xlsx") 
    },
    modifier = Modifier.fillMaxWidth(),
    colors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondary
    )
) {
    Text("Выгрузить отчет в Excel (.xlsx)")
}


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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = effectiveYear.toString(),
                    onValueChange = { effectiveYear = it.toIntOrNull() ?: effectiveYear },
                    label = { Text("Год ввода") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = effectiveMonth.toString(),
                    onValueChange = { effectiveMonth = it.toIntOrNull() ?: effectiveMonth },
                    label = { Text("Месяц (1-12)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

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

            HorizontalDivider()

            Button(
                onClick = { 
                    exportLauncher.launch("Отчет_налет_${effectiveYear}_${effectiveMonth}.xlsx") 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Выгрузить отчет в Excel (.xlsx)")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val updated = TariffEntity(
                        effectiveFromYear = effectiveYear,
                        effectiveFromMonth = effectiveMonth,
                        landHourlyRate = landRate.toDoubleOrNull() ?: currentTariff.landHourlyRate,
                        seaHourlyRate = seaRate.toDoubleOrNull() ?: currentTariff.seaHourlyRate,
                        dutyDayRate = dutyRate.toDoubleOrNull() ?: currentTariff.dutyDayRate
                    )
                    onSaveTariff(updated)
                    onBackClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Сохранить тариф", fontWeight = FontWeight.Bold)
            }
        }
    }
}
