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
    var aircraftNumber by remember { mutableStateOf("") }
    var captain by remember { mutableStateOf("") }
    var missionNumber by remember { mutableStateOf("") }
    var landTimeInput by remember { mutableStateOf("") } // "03:35"
    var seaTimeInput by remember { mutableStateOf("") }  // "00:00"

    val summary: FlightSummary = remember(flights, tariff) {
        CalculationEngine.calculateSummary(flights, emptyList(), tariff)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Учет налета", fontWeight = FontWeight.Bold) },
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
                SummaryCard(summary = summary)
            }

            // 2. Форма добавления полета
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
                                    // Сброс полей ввода
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

            // 4. Элементы списка
            items(flights) { flight ->
                FlightRowItem(flight = flight, onDelete = { onDeleteFlight(flight) })
            }
        }
    }
}

@Composable
fun SummaryCard(summary: FlightSummary) {
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
                text = "Итоговая выплата",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "${summary.grandTotalMoney} ₽",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Общий налет", fontSize = 12.sp)
                    Text(
                        text = "${totalHours}ч ${totalMins}м",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Column {
                    Text("Налет Земля", fontSize = 12.sp)
                    Text(
                        text = "${landHours}ч ${landMins}м",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Column {
                    Text("Полетных дней", fontSize = 12.sp)
                    Text(
                        text = "${summary.totalFlightDays} дн.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
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
