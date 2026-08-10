package com.example.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.domain.CalculationEngine
import com.example.flightlog.domain.FlightSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    flights: List<FlightEntity>,
    tariff: TariffEntity,
    onAddFlight: (FlightEntity) -> Unit,
    onDeleteFlight: (FlightEntity) -> Unit
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

    val flightSummary: FlightSummary = remember(flights, tariff) {
        CalculationEngine.calculateSummary(flights, emptyList(), tariff)
    }

    // Итоговый расчет: полеты + дежурства
    val dutyPay = dutyDays * tariff.dutyDayRate
    val totalPayout = flightSummary.grandTotalMoney + dutyPay

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Учет налета и командировок", fontWeight = FontWeight.Bold) },
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
                    summary = flightSummary,
                    dutyDays = dutyDays,
                    dutyPay = dutyPay,
                    totalPayout = totalPayout,
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
                                    val landMins = CalculationEngine.parseTimeStringToMinutes(landTimeInput)
                                    val seaMins = CalculationEngine.parseTimeStringToMinutes(seaTimeInput)
                                    
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
                FlightRowItem(flight = flight, onDelete = { onDeleteFlight(flight) })
            }
        }
    }
}

@Composable
fun SummaryCard(
    summary: FlightSummary,
    dutyDays: Int,
    dutyPay: Double,
    totalPayout: Double,
    dutyDaysInput: String,
    onDutyDaysChange: (String) -> Unit
) {
    val (totalHours, totalMins) = CalculationEngine.minutesToHoursAndMinutes(summary.totalMinutes)
    val (landHours, landMins) = CalculationEngine.minutesToHoursAndMinutes(summary.totalLandMinutes)

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
                text = "$totalPayout ₽",
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
                        text = "$dutyPay ₽",
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
                        text = "${totalHours}ч ${totalMins}м",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text("Налет Земля", fontSize = 12.sp)
                    Text(
                        text = "${landHours}ч ${landMins}м",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Column {
                    Text("Полетных дней", fontSize = 12.sp)
                    Text(
                        text = "${summary.totalFlightDays} дн.",
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
    val (landH, landM) = CalculationEngine.minutesToHoursAndMinutes(flight.landTimeMinutes)
    
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
                    text = "Земля: ${landH}ч ${landM}м",
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
// Универсальные функции перевода минут в формат "ЧЧ:ММ" для любых типов чисел
fun Number?.minutesToHoursAndMinutes(): String {
    val totalMinutes = this?.toInt() ?: 0
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%02d:%02d", hours, minutes)
}

fun Int?.minutesToHoursAndMinutes(): String {
    val totalMinutes = this ?: 0
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%02d:%02d", hours, minutes)
}

fun Long?.minutesToHoursAndMinutes(): String {
    val totalMinutes = (this ?: 0L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%02d:%02d", hours, minutes)
}
