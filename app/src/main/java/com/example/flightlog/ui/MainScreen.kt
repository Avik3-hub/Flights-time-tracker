package com.example.flightlog.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    flights: List<FlightEntity>,
    dutyRecords: List<DutyEntity>,
    tariff: TariffEntity,
    onAddFlight: (FlightEntity) -> Unit,
    onUpdateFlight: (FlightEntity) -> Unit,
    onDeleteFlight: (Long) -> Unit,
    onSaveDuty: (DutyEntity) -> Unit,
    onSettingsClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    var flightToEdit by remember { mutableStateOf<FlightEntity?>(null) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(0) }
                    },
                    text = { Text("Ввод полета", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    },
                    text = { Text("Статистика и история", fontWeight = FontWeight.Bold) }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> InputTabScreen(
                        flights = flights,
                        dutyRecords = dutyRecords,
                        onAddFlight = onAddFlight,
                        onSaveDuty = onSaveDuty
                    )
                    1 -> StatisticsTabScreen(
                        flights = flights,
                        dutyRecords = dutyRecords,
                        tariff = tariff,
                        onEditFlight = { flightToEdit = it },
                        onDeleteFlight = onDeleteFlight
                    )
                }
            }
        }
    }

    flightToEdit?.let { flight ->
        EditFlightDialog(
            flight = flight,
            onDismiss = { flightToEdit = null },
            onSave = { updatedFlight ->
                onUpdateFlight(updatedFlight)
                flightToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputTabScreen(
    flights: List<FlightEntity>,
    dutyRecords: List<DutyEntity>,
    onAddFlight: (FlightEntity) -> Unit,
    onSaveDuty: (DutyEntity) -> Unit
) {
    var aircraftNumber by remember { mutableStateOf("") }
    var captain by remember { mutableStateOf("") }
    var missionNumber by remember { mutableStateOf("") }
    var landTimeInput by remember { mutableStateOf("") }
    var seaTimeInput by remember { mutableStateOf("") }

    // Автокомплит для КВС
    val captainOptions = remember(flights) {
        flights.map { it.captain }.filter { it.isNotBlank() }.distinct()
    }
    var captainExpanded by remember { mutableStateOf(false) }
    val filteredCaptains = remember(captain, captainOptions) {
        if (captain.isBlank()) captainOptions
        else captainOptions.filter { it.contains(captain, ignoreCase = true) }
    }

    // Состояние даты и дней для блока дежурств
    val currentCal = remember { Calendar.getInstance() }
    var dutyYear by remember { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var dutyMonth by remember { mutableIntStateOf(currentCal.get(Calendar.MONTH) + 1) }

    val existingDuty = remember(dutyRecords, dutyYear, dutyMonth) {
        dutyRecords.find { it.year == dutyYear && it.month == dutyMonth }
    }

    var dutyDaysInput by remember(existingDuty, dutyYear, dutyMonth) {
        mutableStateOf(existingDuty?.dutyDays?.toString() ?: "")
    }

    val monthNames = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )
    val yearsList = listOf(2024, 2025, 2026, 2027, 2028)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Блок добавления полета
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
                        fontWeight = FontWeight.Bold
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

                    // Поле КВС с подсказками (Автокомплит)
                    ExposedDropdownMenuBox(
                        expanded = captainExpanded && filteredCaptains.isNotEmpty(),
                        onExpandedChange = { captainExpanded = !captainExpanded }
                    ) {
                        OutlinedTextField(
                            value = captain,
                            onValueChange = {
                                captain = it
                                captainExpanded = true
                            },
                            label = { Text("ФИО КВС") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = captainExpanded && filteredCaptains.isNotEmpty(),
                            onDismissRequest = { captainExpanded = false }
                        ) {
                            filteredCaptains.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        captain = option
                                        captainExpanded = false
                                    }
                                )
                            }
                        }
                    }

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
                                onAddFlight(
                                    FlightEntity(
                                        dateTimestamp = System.currentTimeMillis(),
                                        aircraftNumber = aircraftNumber,
                                        captain = captain,
                                        missionNumber = missionNumber.ifBlank { null },
                                        landTimeMinutes = parseTimeStringToMinutes(landTimeInput),
                                        seaTimeMinutes = parseTimeStringToMinutes(seaTimeInput)
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

        // 2. Блок дежурств с выбором периода
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Дежурство (Варандей)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Выбор месяца и года для дежурства
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var yearExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = !yearExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = dutyYear.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Год") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false }
                            ) {
                                yearsList.forEach { y ->
                                    DropdownMenuItem(
                                        text = { Text(y.toString()) },
                                        onClick = {
                                            dutyYear = y
                                            yearExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        var monthExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = !monthExpanded },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            OutlinedTextField(
                                value = monthNames[dutyMonth - 1],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Месяц") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false }
                            ) {
                                monthNames.forEachIndexed { idx, mName ->
                                    DropdownMenuItem(
                                        text = { Text(mName) },
                                        onClick = {
                                            dutyMonth = idx + 1
                                            monthExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = dutyDaysInput,
                            onValueChange = { dutyDaysInput = it },
                            label = { Text("Дней дежурства") },
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val days = dutyDaysInput.toIntOrNull() ?: 0
                                onSaveDuty(
                                    DutyEntity(
                                        month = dutyMonth,
                                        year = dutyYear,
                                        dutyDays = days
                                    )
                                )
                            }
                        ) {
                            Text("Сохранить")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsTabScreen(
    flights: List<FlightEntity>,
    dutyRecords: List<DutyEntity>,
    tariff: TariffEntity,
    onEditFlight: (FlightEntity) -> Unit,
    onDeleteFlight: (Long) -> Unit
) {
    val currentCalendar = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(currentCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(currentCalendar.get(Calendar.MONTH) + 1) }

    val yearsList = remember(flights, dutyRecords) {
        val flightYears = flights.map {
            Calendar.getInstance().apply { timeInMillis = it.dateTimestamp }.get(Calendar.YEAR)
        }
        val dutyYears = dutyRecords.map { it.year }
        val years = (flightYears + dutyYears).distinct().sortedDescending().toMutableList()
        val currYr = Calendar.getInstance().get(Calendar.YEAR)
        if (!years.contains(currYr)) years.add(0, currYr)
        years
    }

    val monthNames = listOf(
        "Весь год", "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    val filteredFlights = remember(flights, selectedYear, selectedMonth) {
        flights.filter { flight ->
            val cal = Calendar.getInstance().apply { timeInMillis = flight.dateTimestamp }
            val yearMatches = cal.get(Calendar.YEAR) == selectedYear
            val monthMatches = if (selectedMonth == 0) true else (cal.get(Calendar.MONTH) + 1) == selectedMonth
            yearMatches && monthMatches
        }
    }

    // Расчет дней дежурств за выбранный месяц или за весь год
    val selectedDuty = remember(dutyRecords, selectedYear, selectedMonth) {
        if (selectedMonth == 0) {
            val totalDays = dutyRecords.filter { it.year == selectedYear }.sumOf { it.dutyDays }
            DutyEntity(month = 0, year = selectedYear, dutyDays = totalDays)
        } else {
            dutyRecords.find { it.month == selectedMonth && it.year == selectedYear }
                ?: DutyEntity(month = selectedMonth, year = selectedYear, dutyDays = 0)
        }
    }

    val report: MonthlyReport = remember(filteredFlights, selectedDuty, tariff) {
        CalculationEngine.calculateMonthlyReport(
            filteredFlights,
            selectedDuty,
            tariff
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Фильтр периода", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        var yearExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = yearExpanded,
                            onExpandedChange = { yearExpanded = !yearExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedYear.toString(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Год") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = yearExpanded,
                                onDismissRequest = { yearExpanded = false }
                            ) {
                                yearsList.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year.toString()) },
                                        onClick = {
                                            selectedYear = year
                                            yearExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        var monthExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = monthExpanded,
                            onExpandedChange = { monthExpanded = !monthExpanded },
                            modifier = Modifier.weight(1.3f)
                        ) {
                            OutlinedTextField(
                                value = monthNames[selectedMonth],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Месяц") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = monthExpanded,
                                onDismissRequest = { monthExpanded = false }
                            ) {
                                monthNames.forEachIndexed { index, monthName ->
                                    DropdownMenuItem(
                                        text = { Text(monthName) },
                                        onClick = {
                                            selectedMonth = index
                                            monthExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SummaryCard(report = report)
        }

        item {
            Text(
                text = "Полеты за выбранный период (${filteredFlights.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(filteredFlights) { flight ->
            FlightRowItem(
                flight = flight,
                onEdit = { onEditFlight(flight) },
                onDelete = { onDeleteFlight(flight.id) }
            )
        }
    }
}

@Composable
fun SummaryCard(report: MonthlyReport) {
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
                text = "${String.format("%.2f", report.totalPayment)} ₽",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (report.dutyDays > 0) {
                Text(
                    text = "За дежурства (${report.dutyDays} дн.): ${String.format("%.2f", report.dutyPayment)} ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Общий налет", fontSize = 12.sp)
                    Text(
                        text = report.totalMinutes.minutesToHoursAndMinutes(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text("Земля / Море", fontSize = 12.sp)
                    Text(
                        text = "${report.totalLandMinutes.minutesToHoursAndMinutes()} / ${report.totalSeaMinutes.minutesToHoursAndMinutes()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column {
                    Text("Полетных дней", fontSize = 12.sp)
                    Text(
                        text = "${report.totalFlightDays} дн.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FlightRowItem(
    flight: FlightEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(flight.dateTimestamp))

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dateStr | № ВС: ${flight.aircraftNumber} ${flight.missionNumber?.let { "($it)" } ?: ""}",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "КВС: ${flight.captain.ifBlank { "Не указан" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Земля: ${flight.landTimeMinutes.minutesToHoursAndMinutes()} | Море: ${flight.seaTimeMinutes.minutesToHoursAndMinutes()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Редактировать",
                        tint = MaterialTheme.colorScheme.primary
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
}

@Composable
fun EditFlightDialog(
    flight: FlightEntity,
    onDismiss: () -> Unit,
    onSave: (FlightEntity) -> Unit
) {
    var aircraftNumber by remember { mutableStateOf(flight.aircraftNumber) }
    var captain by remember { mutableStateOf(flight.captain) }
    var missionNumber by remember { mutableStateOf(flight.missionNumber ?: "") }
    var landTimeInput by remember { mutableStateOf(minutesToHoursString(flight.landTimeMinutes)) }
    var seaTimeInput by remember { mutableStateOf(minutesToHoursString(flight.seaTimeMinutes)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование полета") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = aircraftNumber,
                    onValueChange = { aircraftNumber = it },
                    label = { Text("№ ВС") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = missionNumber,
                    onValueChange = { missionNumber = it },
                    label = { Text("Задание") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = captain,
                    onValueChange = { captain = it },
                    label = { Text("ФИО КВС") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = landTimeInput,
                    onValueChange = { landTimeInput = it },
                    label = { Text("Земля (ЧЧ:ММ)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = seaTimeInput,
                    onValueChange = { seaTimeInput = it },
                    label = { Text("Море (ЧЧ:ММ)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = flight.copy(
                        aircraftNumber = aircraftNumber,
                        captain = captain,
                        missionNumber = missionNumber.ifBlank { null },
                        landTimeMinutes = parseTimeStringToMinutes(landTimeInput),
                        seaTimeMinutes = parseTimeStringToMinutes(seaTimeInput)
                    )
                    onSave(updated)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

fun minutesToHoursString(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    return String.format("%02d:%02d", h, m)
}

fun Number?.minutesToHoursAndMinutes(): String {
    val totalMinutes = this?.toInt() ?: 0
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%d ч %02d мин", hours, minutes)
}

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
