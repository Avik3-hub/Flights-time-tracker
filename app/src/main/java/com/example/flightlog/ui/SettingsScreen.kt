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
    var effectiveYear by remember { mutableIntStateOf(currentTariff.effectiveFromYear) }
    var effectiveMonth by remember { mutableIntStateOf(currentTariff.effectiveFromMonth) }

    var landRate by remember(currentTariff) { mutableStateOf(currentTariff.landHourlyRate.toString()) }
    var seaRate by remember(currentTariff) { mutableStateOf(currentTariff.seaHourlyRate.toString()) }
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
    val context = LocalContext.current
val coroutineScope = rememberCoroutineScope()

// 1. Лаунчер для открытия проводника Android и получения URI файла
val importExcelLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
) { uri: Uri? ->
    uri?.let { selectedUri ->
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Вызываем написанный парсер
                val importResult = ExcelImporter.importFromExcel(context, selectedUri)

                // Сохраняем полученные списки в БД Room
                if (importResult.flights.isNotEmpty()) {
                    flightDao.insertFlights(importResult.flights)
                }
                if (importResult.duties.isNotEmpty()) {
                    dutyDao.insertDuties(importResult.duties)
                }

                // Уведомляем пользователя на главном потоке
                withContext(Dispatchers.Main) {
                    val msg = "Успешно импортировано: рейсов — ${importResult.flights.size}, дежурств — ${importResult.duties.size}"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Ошибка при импорте файла: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}

// 2. Элемент интерфейса (кнопка) для запуска
Button(
    onClick = {
        // Запрашиваем выбор файлов Excel
        importExcelLauncher.launch(
            arrayOf(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/vnd.ms-excel",
                "*/*"
            )
        )
    },
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)
) {
    Text("Импортировать резервную копию (.xlsx)")
}

}
