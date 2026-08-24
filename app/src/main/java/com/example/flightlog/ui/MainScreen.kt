package com.example.flightlog.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.data.export.ExcelExporter
import com.example.flightlog.domain.CalculationEngine
import com.example.flightlog.domain.MonthlyReport
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    flights: List<FlightEntity>,
    dutyRecords: List<DutyEntity>,
    tariffs: List<TariffEntity>,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onAddFlight: (FlightEntity) -> Unit,
    onUpdateFlight: (FlightEntity) -> Unit,
    onDeleteFlight: (Long) -> Unit,
    onSaveDuty: (DutyEntity) -> Unit,
    onSettingsClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    var flightToEdit by remember { mutableStateOf<FlightEntity?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Счетчик налета") },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Настройки"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Тарифы") },
                                onClick = {
                                    menuExpanded = false
                                    onSettingsClick()
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { 
                                    Text(if (isDarkTheme) "Светлая тема" else "Темная тема") 
                                },
                                onClick = {
                                    menuExpanded = false
                                    onToggleTheme()
                                }
                            )
                        }
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
                        tariffs = tariffs,
                        onEditFlight = { flightToEdit = it },
                        onDeleteFlight = onDeleteFlight,
                        onImportSuccess = { importedFlights, importedDuties ->
                            importedFlights.forEach { onAddFlight(it) }
                            importedDuties.forEach { onSaveDuty(it) }
                        }
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
    val context = LocalContext.current
    var selectedDateMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    var aircraftNumber by remember { mutableStateOf("") }
    var captain by remember { mutableStateOf("") }
    var missionNumber by remember { mutableStateOf("") }

    var landHours by remember { mutableIntStateOf(0) }
    var landMinutes by remember { mutableIntStateOf(0) }
    var showLandTimePicker by remember { mutableStateOf(false) }

    var seaHours by remember { mutableIntStateOf(0) }
    var seaMinutes by remember { mutableIntStateOf(0) }
    var showSeaTimePicker by remember { mutableStateOf(false) }

    // Список и состояние выпадающего меню для № ВС
    val aircraftOptions = remember(flights) {
        flights.map { it.aircraftNumber }.filter { it.isNotBlank() }.distinct()
    }
    var aircraftExpanded by remember { mutableStateOf(false) }
    val filteredAircrafts = remember(aircraftNumber, aircraftOptions) {
        if (aircraftNumber.isBlank()) aircraftOptions
        else aircraftOptions.filter { it.contains(aircraftNumber, ignoreCase = true) }
    }

    // Список и состояние выпадающего меню для ФИО КВС
    val captainOptions = remember(flights) {
        flights.map { it.captain }.filter { it.isNotBlank() }.distinct()
    }
    var captainExpanded by remember { mutableStateOf(false) }
    val filteredCaptains = remember(captain, captainOptions) {
        if (captain.isBlank()) captainOptions
        else captainOptions.filter { it.contains(captain, ignoreCase = true) }
    }

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

                    OutlinedTextField(
                        value = formatDate(selectedDateMillis),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата полета") },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = aircraftExpanded && filteredAircrafts.isNotEmpty(),
                            onExpandedChange = { aircraftExpanded = !aircraftExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = aircraftNumber,
                                onValueChange = {
                                    aircraftNumber = it
                                    aircraftExpanded = true
                                },
                                label = { Text("№ ВС") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            ExposedDropdownMenu(
                                expanded = aircraftExpanded && filteredAircrafts.isNotEmpty(),
                                onDismissRequest = { aircraftExpanded = false }
                            ) {
                                filteredAircrafts.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            aircraftNumber = option
                                            aircraftExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = missionNumber,
                            onValueChange = { missionNumber = it },
                            label = { Text("Задание") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

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
                            value = String.format("%02d:%02d", landHours, landMinutes),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Земля (ЧЧ:ММ)") },
                            trailingIcon = {
                                IconButton(onClick = { showLandTimePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Выбрать время")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showLandTimePicker = true }
                        )

                        OutlinedTextField(
                            value = String.format("%02d:%02d", seaHours, seaMinutes),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Море (ЧЧ:ММ)") },
                            trailingIcon = {
                                IconButton(onClick = { showSeaTimePicker = true }) {
                                    Icon(Icons.Default.DateRange, contentDescription = "Выбрать время")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showSeaTimePicker = true }
                        )
                    }

                    Button(
                        onClick = {
                            if (aircraftNumber.isNotBlank()) {
                                onAddFlight(
                                    FlightEntity(
                                        dateTimestamp = selectedDateMillis,
                                        aircraftNumber = aircraftNumber,
                                        captain = captain,
                                        missionNumber = missionNumber.ifBlank { null },
                                        landTimeMinutes = landHours * 60 + landMinutes,
                                        seaTimeMinutes = seaHours * 60 + seaMinutes
                                    )
                                )
                                aircraftNumber = ""
                                captain = ""
                                missionNumber = ""
                                landHours = 0
                                landMinutes = 0
                                seaHours = 0
                                seaMinutes = 0
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
                                val daysCount = dutyDaysInput.toIntOrNull() ?: 0
                                val dutyToSave = existingDuty?.copy(dutyDays = daysCount)
                                    ?: DutyEntity(
                                        month = dutyMonth,
                                        year = dutyYear,
                                        dutyDays = daysCount
                                    )
                                onSaveDuty(dutyToSave)
                                Toast.makeText(
                                    context,
                                    if (existingDuty != null) "Дежурство обновлено" else "Дежурство сохранено",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Text(if (existingDuty != null) "Обновить" else "Сохранить")
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showLandTimePicker) {
        TimeSelectionDialog(
            initialHour = landHours,
            initialMinute = landMinutes,
            onDismiss = { showLandTimePicker = false },
            onConfirm = { h, m ->
                landHours = h
                landMinutes = m
                showLandTimePicker = false
            }
        )
    }

    if (showSeaTimePicker) {
        TimeSelectionDialog(
            initialHour = seaHours,
            initialMinute = seaMinutes,
            onDismiss = { showSeaTimePicker = false },
            onConfirm = { h, m ->
                seaHours = h
                seaMinutes = m
                showSeaTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectionDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("ОК")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state = timePickerState)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsTabScreen(
    flights: List<FlightEntity>,
    dutyRecords: List<DutyEntity>,
    tariffs: List<TariffEntity>,
    onEditFlight: (FlightEntity) -> Unit,
    onDeleteFlight: (Long) -> Unit,
    onImportSuccess: (List<FlightEntity>, List<DutyEntity>) -> Unit = { _, _ -> }
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

    val selectedDuty = remember(dutyRecords, selectedYear, selectedMonth) {
        if (selectedMonth == 0) {
            val totalDays = dutyRecords.filter { it.year == selectedYear }.sumOf { it.dutyDays }
            DutyEntity(month = 0, year = selectedYear, dutyDays = totalDays)
        } else {
            dutyRecords.find { it.month == selectedMonth && it.year == selectedYear }
                ?: DutyEntity(month = selectedMonth, year = selectedYear, dutyDays = 0)
        }
    }

    val activeTariff = remember(tariffs, selectedYear, selectedMonth) {
        val monthForSearch = if (selectedMonth == 0) 12 else selectedMonth
        CalculationEngine.getActiveTariff(tariffs, selectedYear, monthForSearch)
            ?: TariffEntity(
                effectiveFromYear = 2025,
                effectiveFromMonth = 1,
                landHourlyRate = 879.57,
                seaHourlyRate = 0.0,
                dutyDayRate = 0.0
            )
    }

    val report: MonthlyReport = remember(filteredFlights, selectedDuty, activeTariff) {
        CalculationEngine.calculateMonthlyReport(
            filteredFlights,
            selectedDuty,
            activeTariff
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
            SummaryCard(
                report = report,
                dutyDays = selectedDuty.dutyDays,
                monthlyFlights = filteredFlights,
                monthlyDuty = selectedDuty,
                currentTariff = activeTariff,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth
            )
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
fun SummaryCard(
    report: MonthlyReport,
    dutyDays: Int,
    monthlyFlights: List<FlightEntity>,
    monthlyDuty: DutyEntity?,
    currentTariff: TariffEntity,
    selectedYear: Int,
    selectedMonth: Int
) {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            val success = ExcelExporter.exportToExcel(
                context = context,
                uri = it,
                flights = monthlyFlights,
                duty = monthlyDuty,
                tariff = currentTariff
            )
            if (success) {
                Toast.makeText(context, "Отчет сохранен в Excel!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Ошибка при сохранении", Toast.LENGTH_LONG).show()
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Итоговая выплата (включая дежурство и с вычетом 13% НДФЛ)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Text(
                text = "${String.format("%.2f", report.totalPayment)} ₽",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            if (dutyDays > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "За дежурство ($dutyDays дн.): ${String.format("%.2f", report.dutyPayment)} ₽",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Общий налет",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = report.totalMinutes.minutesToHoursAndMinutes(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Полетных дней",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "${report.totalFlightDays} дн.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Земля",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = report.totalLandMinutes.minutesToHoursAndMinutes(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Море",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = report.totalSeaMinutes.minutesToHoursAndMinutes(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { 
                    exportLauncher.launch("Отчет_налет_${selectedYear}_${selectedMonth}.xlsx") 
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Выгрузить отчет в Excel (.xlsx)")
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
    val dateStr = formatDate(flight.dateTimestamp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFlightDialog(
    flight: FlightEntity,
    onDismiss: () -> Unit,
    onSave: (FlightEntity) -> Unit
) {
    var dateMillis by remember { mutableLongStateOf(flight.dateTimestamp) }
    var showDatePicker by remember { mutableStateOf(false) }

    var aircraftNumber by remember { mutableStateOf(flight.aircraftNumber) }
    var captain by remember { mutableStateOf(flight.captain) }
    var missionNumber by remember { mutableStateOf(flight.missionNumber ?: "") }

    var landHours by remember { mutableIntStateOf(flight.landTimeMinutes / 60) }
    var landMinutes by remember { mutableIntStateOf(flight.landTimeMinutes % 60) }
    var showLandTimePicker by remember { mutableStateOf(false) }

    var seaHours by remember { mutableIntStateOf(flight.seaTimeMinutes / 60) }
    var seaMinutes by remember { mutableIntStateOf(flight.seaTimeMinutes % 60) }
    var showSeaTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование полета") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = formatDate(dateMillis),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Дата") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { showDatePicker = true }
                )
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
                    value = String.format("%02d:%02d", landHours, landMinutes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Земля") },
                    trailingIcon = {
                        IconButton(onClick = { showLandTimePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { showLandTimePicker = true }
                )
                OutlinedTextField(
                    value = String.format("%02d:%02d", seaHours, seaMinutes),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Море") },
                    trailingIcon = {
                        IconButton(onClick = { showSeaTimePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { showSeaTimePicker = true }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = flight.copy(
                        dateTimestamp = dateMillis,
                        aircraftNumber = aircraftNumber,
                        captain = captain,
                        missionNumber = missionNumber.ifBlank { null },
                        landTimeMinutes = landHours * 60 + landMinutes,
                        seaTimeMinutes = seaHours * 60 + seaMinutes
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis = it }
                        showDatePicker = false
                    }
                ) { Text("ОК") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showLandTimePicker) {
        TimeSelectionDialog(
            initialHour = landHours,
            initialMinute = landMinutes,
            onDismiss = { showLandTimePicker = false },
            onConfirm = { h, m ->
                landHours = h
                landMinutes = m
                showLandTimePicker = false
            }
        )
    }

    if (showSeaTimePicker) {
        TimeSelectionDialog(
            initialHour = seaHours,
            initialMinute = seaMinutes,
            onDismiss = { showSeaTimePicker = false },
            onConfirm = { h, m ->
                seaHours = h
                seaMinutes = m
                showSeaTimePicker = false
            }
        )
    }
}

fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    return dateFormat.format(Date(timestamp))
}

fun Number?.minutesToHoursAndMinutes(): String {
    val totalMinutes = this?.toInt() ?: 0
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return String.format("%d ч %02d мин", hours, minutes)
}
