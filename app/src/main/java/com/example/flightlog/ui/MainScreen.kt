package com.example.flightlog.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.example.flightlog.ui.theme.AppTheme
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
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
import com.example.flightlog.data.export.ExcelImporter
import com.example.flightlog.domain.CalculationEngine
import com.example.flightlog.domain.MonthlyReport
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    flights: List<FlightEntity>,
    dutyRecords: List<DutyEntity>,
    tariffs: List<TariffEntity>,
    selectedTheme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onAddFlight: (FlightEntity) -> Unit,
    onUpdateFlight: (FlightEntity) -> Unit,
    onDeleteFlight: (Long) -> Unit,
    onSaveDuty: (DutyEntity) -> Unit,
    onSaveTariff: (TariffEntity) -> Unit = {},
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val versionName = remember {
        try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "" }
        catch (_: Exception) { "" }
    }
    var page by rememberSaveable { mutableIntStateOf(0) }
    val stateHolder = rememberSaveableStateHolder()
    val now = remember { Calendar.getInstance() }
    var overviewYear by rememberSaveable { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var overviewMonth by rememberSaveable { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }
    var flightToEdit by remember { mutableStateOf<FlightEntity?>(null) }
    var flightToDelete by remember { mutableStateOf<FlightEntity?>(null) }
    var showThemes by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = page != 0) { page = 0 }

    CockpitScaffold(page = page, onPageChange = { page = it }) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            // Fixed action does not cover the last journal row on small screens.
            if (page == 1) {
                OutlinedButton(
                    onClick = { page = 2 },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Полёт", fontWeight = FontWeight.SemiBold)
                }
            }
            Box(Modifier.weight(1f)) {
                stateHolder.SaveableStateProvider(page) {
                    when (page) {
                        0 -> OverviewScreen(
                            flights = flights, dutyRecords = dutyRecords, tariffs = tariffs,
                            year = overviewYear, month = overviewMonth,
                            onPeriodChange = { year, month -> overviewYear = year; overviewMonth = month },
                            onOpenJournal = {
                                stateHolder.removeState(1)
                                page = 1
                            },
                            onEditFlight = { flightToEdit = it },
                            onAddFlight = { page = 2 }
                        )
                        1 -> StatisticsTabScreen(
                            flights = flights, dutyRecords = dutyRecords, tariffs = tariffs,
                            initialYear = overviewYear, initialMonth = overviewMonth,
                            onEditFlight = { flightToEdit = it },
                            onDeleteFlight = { id -> flightToDelete = flights.find { it.id == id } },
                            onImportSuccess = { importedFlights, importedDuties, importedTariffs ->
                                importedFlights.forEach { onAddFlight(it) }
                                importedDuties.forEach { onSaveDuty(it) }
                                importedTariffs.forEach { onSaveTariff(it) }
                            }
                        )
                        2 -> InputTabScreen(
                            flights = flights, dutyRecords = dutyRecords,
                            onAddFlight = onAddFlight, onSaveDuty = onSaveDuty
                        )
                        3 -> MoreScreen(
                            selectedTheme = selectedTheme,
                            onThemeClick = { showThemes = true },
                            onTariffsClick = onSettingsClick,
                            versionName = versionName
                        )
                    }
                }
            }
        }
    }
    if (showThemes) {
        ThemePickerDialog(
            selected = selectedTheme,
            onSelect = { onSelectTheme(it); showThemes = false },
            onDismiss = { showThemes = false }
        )
    }
    flightToEdit?.let { flight ->
        EditFlightDialog(
            flight = flight,
            onDismiss = { flightToEdit = null },
            onSave = { onUpdateFlight(it); flightToEdit = null }
        )
    }
    flightToDelete?.let { flight ->
        AlertDialog(
            onDismissRequest = { flightToDelete = null },
            title = { Text("Удалить полёт?") },
            text = { Text("${formatDate(flight.dateTimestamp)} · ${flight.aircraftNumber}") },
            confirmButton = {
                TextButton(onClick = { onDeleteFlight(flight.id); flightToDelete = null }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { flightToDelete = null }) { Text("Отмена") }
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
    var selectedDateMillis by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var aircraftNumber by rememberSaveable { mutableStateOf("") }
    var captain by rememberSaveable { mutableStateOf("") }
    var missionNumber by rememberSaveable { mutableStateOf("") }
    var landHours by rememberSaveable { mutableIntStateOf(0) }
    var landMinutes by rememberSaveable { mutableIntStateOf(0) }
    var showLandTimePicker by rememberSaveable { mutableStateOf(false) }
    var seaHours by rememberSaveable { mutableIntStateOf(0) }
    var seaMinutes by rememberSaveable { mutableIntStateOf(0) }
    var showSeaTimePicker by rememberSaveable { mutableStateOf(false) }

    // Состояние для отображения диалога успешного сохранения
    var showSaveSuccessDialog by rememberSaveable { mutableStateOf(false) }

    val aircraftOptions = remember(flights) {
        flights.map { it.aircraftNumber }.filter { it.isNotBlank() }.distinct()
    }
    var aircraftExpanded by rememberSaveable { mutableStateOf(false) }
    val filteredAircrafts = remember(aircraftNumber, aircraftOptions) {
        if (aircraftNumber.isBlank()) aircraftOptions
        else aircraftOptions.filter { it.contains(aircraftNumber, ignoreCase = true) }
    }

    val captainOptions = remember(flights) {
        flights.map { it.captain }.filter { it.isNotBlank() }.distinct()
    }
    var captainExpanded by rememberSaveable { mutableStateOf(false) }
    val filteredCaptains = remember(captain, captainOptions) {
        if (captain.isBlank()) captainOptions
        else captainOptions.filter { it.contains(captain, ignoreCase = true) }
    }

    val currentCal = remember { Calendar.getInstance() }
    var dutyYear by rememberSaveable { mutableIntStateOf(currentCal.get(Calendar.YEAR)) }
    var dutyMonth by rememberSaveable { mutableIntStateOf(currentCal.get(Calendar.MONTH) + 1) }

    val existingDuty = remember(dutyRecords, dutyYear, dutyMonth) {
        dutyRecords.find { it.year == dutyYear && it.month == dutyMonth }
    }
    var dutyDaysInput by rememberSaveable(existingDuty, dutyYear, dutyMonth) {
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

                    // Поле: Дата полета (кликабельно целиком)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatDate(selectedDateMillis),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Дата полета") },
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { showDatePicker = true }
                        )
                    }

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

                    // Поля времени Земля и Море (кликабельны целиком + иконка часов)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = String.format("%02d:%02d", landHours, landMinutes),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Земля (ЧЧ:ММ)") },
                                trailingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = "Выбрать время")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showLandTimePicker = true }
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = String.format("%02d:%02d", seaHours, seaMinutes),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Море (ЧЧ:ММ)") },
                                trailingIcon = {
                                    Icon(Icons.Default.Schedule, contentDescription = "Выбрать время")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showSeaTimePicker = true }
                            )
                        }
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

                                // Включаем отображение диалога при успехе
                                showSaveSuccessDialog = true
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
                        var yearExpanded by rememberSaveable { mutableStateOf(false) }
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
                        var monthExpanded by rememberSaveable { mutableStateOf(false) }
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

    // Всплывающее окно подтверждения сохранения полета
    if (showSaveSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSaveSuccessDialog = false },
            text = {
                Text(
                    text = "Полет добавлен.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showSaveSuccessDialog = false }
                ) {
                    Text("Ок")
                }
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
    initialYear: Int,
    initialMonth: Int,
    onEditFlight: (FlightEntity) -> Unit,
    onDeleteFlight: (Long) -> Unit,
    onImportSuccess: (List<FlightEntity>, List<DutyEntity>, List<TariffEntity>) -> Unit = { _, _, _ -> }
) {
    var selectedYear by rememberSaveable { mutableIntStateOf(initialYear) }
    var selectedMonth by rememberSaveable { mutableIntStateOf(initialMonth) }
    var startDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var endDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var startDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var endDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    val daysOptions = remember { listOf(null) + (1..31).toList() }

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

    val filteredFlights = remember(flights, selectedYear, selectedMonth, startDay, endDay) {
        flights.filter { flight ->
            val cal = Calendar.getInstance().apply { timeInMillis = flight.dateTimestamp }
            val yearMatches = cal.get(Calendar.YEAR) == selectedYear
            val monthMatches = if (selectedMonth == 0) true else (cal.get(Calendar.MONTH) + 1) == selectedMonth
            
            val flightDay = cal.get(Calendar.DAY_OF_MONTH)
            val startDayMatches = startDay == null || flightDay >= startDay!!
            val endDayMatches = endDay == null || flightDay <= endDay!!
            yearMatches && monthMatches && startDayMatches && endDayMatches
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

    val report: MonthlyReport = remember(filteredFlights, selectedDuty, tariffs) {
        CalculationEngine.calculateReport(
            flights = filteredFlights,
            duties = listOfNotNull(selectedDuty),
            tariffs = tariffs
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
                        var yearExpanded by rememberSaveable { mutableStateOf(false) }
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
                        var monthExpanded by rememberSaveable { mutableStateOf(false) }
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = startDropdownExpanded,
                            onExpandedChange = { startDropdownExpanded = !startDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = startDay?.let { "$it число" } ?: "С начала",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("День с") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startDropdownExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = startDropdownExpanded,
                                onDismissRequest = { startDropdownExpanded = false }
                            ) {
                                daysOptions.forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day?.let { "$it число" } ?: "С начала") },
                                        onClick = {
                                            startDay = day
                                            startDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        ExposedDropdownMenuBox(
                            expanded = endDropdownExpanded,
                            onExpandedChange = { endDropdownExpanded = !endDropdownExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = endDay?.let { "$it число" } ?: "До конца",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("День по") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endDropdownExpanded) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = endDropdownExpanded,
                                onDismissRequest = { endDropdownExpanded = false }
                            ) {
                                daysOptions.forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day?.let { "$it число" } ?: "До конца") },
                                        onClick = {
                                            endDay = day
                                            endDropdownExpanded = false
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
                allTariffs = tariffs,
                allDuties = dutyRecords,
                selectedYear = selectedYear,
                selectedMonth = selectedMonth,
                onImportSuccess = onImportSuccess
            )
        }
        item {
            Text(
                text = "Полеты за выбранный период (${filteredFlights.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(filteredFlights, key = { it.id }) { flight ->
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
    allTariffs: List<TariffEntity>,
    allDuties: List<DutyEntity>,
    selectedYear: Int,
    selectedMonth: Int,
    onImportSuccess: (List<FlightEntity>, List<DutyEntity>, List<TariffEntity>) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showImportConfirmation by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            val success = ExcelExporter.exportToExcel(
                context = context,
                uri = it,
                flights = monthlyFlights,
                duties = allDuties,
                activeTariff = currentTariff,
                allTariffs = allTariffs
            )
            if (success) {
                Toast.makeText(context, "Отчет сохранен в Excel!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Ошибка при сохранении", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val importResult = ExcelImporter.importFromExcel(context, selectedUri)
                    withContext(Dispatchers.Main) {
                        if (importResult.flights.isNotEmpty() || importResult.duties.isNotEmpty() || importResult.tariffs.isNotEmpty()) {
                            onImportSuccess(importResult.flights, importResult.duties, importResult.tariffs)
                        }
                        val msg = "Импортировано: рейсов — ${importResult.flights.size}, дежурств — ${importResult.duties.size}, тарифов — ${importResult.tariffs.size}"
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Throwable) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Ошибка при импорте: ${e.localizedMessage ?: e.javaClass.simpleName}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
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
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showImportConfirmation = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Импортировать из Excel (.xlsx)")
            }
        }
    }

    if (showImportConfirmation) {
        AlertDialog(
            onDismissRequest = { showImportConfirmation = false },
            title = { Text("Импорт данных") },
            text = {
                Text("Убедитесь, что выбираете файл Excel, ранее выгруженный из этого приложения, версии не ниже 1.1.0 (версия приложения указана сверху на главной). Найденные полеты, дежурства и тарифы будут добавлены в базу.\n\nПродолжить?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImportConfirmation = false
                        importLauncher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel",
                                "*/*"
                            )
                        )
                    }
                ) {
                    Text("Продолжить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirmation = false }) {
                    Text("Отмена")
                }
            }
        )
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(dateStr, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "№ ВС: ${flight.aircraftNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
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
            flight.missionNumber?.takeIf { it.isNotBlank() }?.let {
                Text("Задание: $it", style = MaterialTheme.typography.bodyMedium)
            }
            Text("КВС: ${flight.captain.ifBlank { "Не указан" }}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                (flight.landTimeMinutes + flight.seaTimeMinutes).minutesToHoursAndMinutes(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Земля: ${flight.landTimeMinutes.minutesToHoursAndMinutes()}\n" +
                    "Море: ${flight.seaTimeMinutes.minutesToHoursAndMinutes()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    var dateMillis by rememberSaveable { mutableLongStateOf(flight.dateTimestamp) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var aircraftNumber by rememberSaveable { mutableStateOf(flight.aircraftNumber) }
    var captain by rememberSaveable { mutableStateOf(flight.captain) }
    var missionNumber by rememberSaveable { mutableStateOf(flight.missionNumber ?: "") }
    var landHours by rememberSaveable { mutableIntStateOf(flight.landTimeMinutes / 60) }
    var landMinutes by rememberSaveable { mutableIntStateOf(flight.landTimeMinutes % 60) }
    var showLandTimePicker by rememberSaveable { mutableStateOf(false) }
    var seaHours by rememberSaveable { mutableIntStateOf(flight.seaTimeMinutes / 60) }
    var seaMinutes by rememberSaveable { mutableIntStateOf(flight.seaTimeMinutes % 60) }
    var showSeaTimePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Редактирование полета") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formatDate(dateMillis),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата") },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = "Выбрать дату")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }

                OutlinedTextField(
                    value = aircraftNumber,
                    onValueChange = { aircraftNumber = it },
                    label = { Text("№ ВС") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = missionNumber,
                    onValueChange = { missionNumber = it },
                    label = { Text("Задание") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = captain,
                    onValueChange = { captain = it },
                    label = { Text("ФИО КВС") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = String.format("%02d:%02d", landHours, landMinutes),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Земля") },
                        trailingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = "Выбрать время")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showLandTimePicker = true }
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = String.format("%02d:%02d", seaHours, seaMinutes),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Море") },
                        trailingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = "Выбрать время")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showSeaTimePicker = true }
                    )
                }
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
