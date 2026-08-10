package com.example.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.domain.CalculationEngine
import com.example.flightlog.domain.MonthlyReport

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    flights: List<FlightEntity>,
    dutyRecords: List<DutyEntity>,
    tariff: TariffEntity, // <--- Обязательно добавьте этот параметр сюда
    onAddFlight: (FlightEntity) -> Unit,
    onDeleteFlight: (Long) -> Unit,
    onSaveDuty: (DutyEntity) -> Unit,
    onSettingsClick: () -> Unit
) {
    // Поля ввода для полета
    var aircraftNumber by remember { mutableStateOf("") }
    var captain by remember { mutableStateOf("") }
    var missionNumber by remember { mutableStateOf("") }
    var landTimeInput by remember { mutableStateOf("") }
    var seaTimeInput by remember { mutableStateOf("") }

    // Поле ввода дней дежурства / командировки (Варандей)
    var dutyDaysInput by remember { mutableStateOf("") }
    val dutyDays = dutyDaysInput.toIntOrNull() ?: 0

    val dutyEntity = remember(dutyDays) {
        DutyEntity(month = 0, year = 0, dutyDays = dutyDays)
    }

    val monthlyReport: MonthlyReport = remember(flights, dutyEntity, tariff) {
        CalculationEngine.calculateMonthlyReport(flights, dutyEntity, tariff)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Счетчик налета") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки тарифов"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Сводная карточка отчета
            item {
                SummaryCard(
                    report = monthlyReport,
                    dutyDaysInput = dutyDaysInput,
                    onDutyDaysChange = { dutyDaysInput = it }
                )
            }

            // 2. Форма добавления нового полета
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Новая запись полета",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = aircraftNumber,
                                onValueChange = { aircraftNumber = it },
                                label = { Text("№ ВС") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = missionNumber,
                                onValueChange = { missionNumber = it },
                                label = { Text("Задание") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = captain,
                            onValueChange = { captain = it },
                            label = { Text("ФИО КВС") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = landTimeInput,
                                onValueChange = { landTimeInput = it },
                                label = { Text("Земля (ЧЧ:ММ)") },
                                placeholder = { Text("03:35") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = seaTimeInput,
                                onValueChange = { seaTimeInput = it },
                                label = { Text("Море (ЧЧ:ММ)") },
                                placeholder = { Text("00:00") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Button(
                            onClick = {
                                if (aircraftNumber.isNotBlank()) {
                                    val landMins = parseTimeStringToMinutes(landTimeInput)
                                    val seaMins = parseTimeStringToMinutes(seaTimeInput)
                                    
                                    onAddFlight(
                                        FlightEntity(
                                            dateTimestamp = System.currentTimeMillis(),
                                            aircraftNumber = aircraftNumber,
                                            captain = captain,
                                            missionNumber = missionNumber.ifBlank { null },
                                            landTimeMinutes = landMins,
                                            seaTimeMinutes = seaMins
                                        )
                                    )
                                    aircraftNumber = ""
                                    captain = ""
                                    missionNumber = ""
                                    landTimeInput = ""
                                    seaTimeInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Сохранить полет")
                        }
                    }
                }
            }

            // 3. Заголовок списка
            item {
                Text(
                    text = "История полетов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4. Список полетов
            items(flights) { flight ->
                FlightRowItem(flight = flight, onDelete = { onDeleteFlight(flight.id) })
            }
        }
    }
}

@Composable
fun SummaryCard(
    report: MonthlyReport,
    dutyDaysInput: String,
    onDutyDaysChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Общая выплата за месяц",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "${String.format("%.2f", report.totalPayment)} ₽",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // Блок ввода дней дежурства / Варандей
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedTextField(
                    value = dutyDaysInput,
                    onValueChange = onDutyDaysChange,
                    label = { Text("Дней дежурства (Варандей)") },
                    placeholder = { Text("0") },
                    modifier = Modifier.width(180.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text("За дежурства:", fontSize = 12.sp)
                    Text(
                        text = "${String.format("%.2f", report.dutyPayment)} ₽",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // Статистика налета
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Общий налет", fontSize = 12.sp)
                    Text(
                        text = report.totalMinutes.minutesToHoursAndMinutes(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text("Налет Земля", fontSize = 12.sp)
                    Text(
                        text = report.totalLandMinutes.minutesToHoursAndMinutes(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text("Полетных дней", fontSize = 12.sp)
                    Text(
                        text = "${report.totalFlightDays} дн.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FlightRowItem(flight: FlightEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "№ ВС: ${flight.aircraftNumber} ${flight.missionNumber?.let { "($it)" } ?: ""}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "КВС: ${flight.captain.ifBlank { "Не указан" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Земля: ${flight.landTimeMinutes.minutesToHoursAndMinutes()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// Универсальные функции перевода минут в формат строки
fun Number?.minutesToHoursAndMinutes(): String {
    val totalMinutes = this?.toInt() ?: 0
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%d ч %02d мин", hours, minutes)
}

fun Int?.minutesToHoursAndMinutes(): String {
    val totalMinutes = this ?: 0
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%d ч %02d мин", hours, minutes)
}

fun Long?.minutesToHoursAndMinutes(): String {
    val totalMinutes = (this ?: 0L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%d ч %02d мин", hours, minutes)
}

// Парсинг строки "ЧЧ:ММ" или "ЧЧ" в общее число минут
fun parseTimeStringToMinutes(timeString: String): Int {
    if (timeString.isBlank()) return 0
    val parts = timeString.split(":")
    return try {
        if (parts.size == 2) {
            val hours = parts[0].trim().toIntOrNull() ?: 0
            val minutes = parts[1].trim().toIntOrNull() ?: 0
            hours * 60 + minutes
        } else {
            val hours = timeString.trim().toIntOrNull() ?: 0
            hours * 60
        }
    } catch (e: Exception) {
        0
    }
}
