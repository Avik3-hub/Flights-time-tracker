package com.example.flightlog.ui

import android.net.Uri
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
import com.example.flightlog.data.db.DutyDao
import com.example.flightlog.data.db.ExcelImporter
import com.example.flightlog.data.db.FlightDao
import com.example.flightlog.data.db.TariffEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTariff: TariffEntity,
    flightDao: FlightDao,
    dutyDao: DutyDao,
    onSaveTariff: (TariffEntity) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var effectiveYear by remember { mutableIntStateOf(currentTariff.effectiveFromYear) }
    var effectiveMonth by remember { mutableIntStateOf(currentTariff.effectiveFromMonth) }

    var landRate by remember(currentTariff) { mutableStateOf(currentTariff.landHourlyRate.toString()) }
    var seaRate by remember(currentTariff) { mutableStateOf(currentTariff.seaHourlyRate.toString()) }
    var dutyRate by remember(currentTariff) { mutableStateOf(currentTariff.dutyDayRate.toString()) }

    // Лаунчер открытия файлов Android
    val importExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val importResult = ExcelImporter.importFromExcel(context, selectedUri)

                    if (importResult.flights.isNotEmpty()) {
                        flightDao.insertFlights(importResult.flights)
                    }
                    if (importResult.duties.isNotEmpty()) {
                        dutyDao.insertDuties(importResult.duties)
                    }

                    withContext(Dispatchers.Main) {
                        val msg = "Успешно импортировано: рейсов — ${importResult.flights.size}, дежурств — ${importResult.duties.size}"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Throwable) { // Перехватывает любые системные ошибки и исключения
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Ошибка при импорте файла: ${e.localizedMessage ?: e.javaClass.simpleName}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки тарифов и бэкап") },
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
            // Кнопка импорта
            OutlinedButton(
                onClick = {
                    importExcelLauncher.launch(
                        arrayOf(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "application/vnd.ms-excel",
                            "*/*"
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Импортировать данные из Excel (.xlsx)")
            }

            HorizontalDivider()

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
