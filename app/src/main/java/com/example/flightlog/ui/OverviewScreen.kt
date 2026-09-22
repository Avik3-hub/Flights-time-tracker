package com.example.flightlog.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flightlog.R
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.domain.CalculationEngine
import java.time.YearMonth
import java.util.Calendar
import java.util.Locale

private val MonthNames = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

@Composable
fun OverviewScreen(
    flights: List<FlightEntity>,
    dutyRecords: List<DutyEntity>,
    tariffs: List<TariffEntity>,
    year: Int,
    month: Int,
    onPeriodChange: (Int, Int) -> Unit,
    onOpenJournal: () -> Unit,
    onEditFlight: (FlightEntity) -> Unit,
    onAddFlight: () -> Unit
) {
    val period = YearMonth.of(year, month)
    // Use the same calendar interpretation and calculation engine as the journal.
    val monthlyFlights = remember(flights, year, month) {
        flights.filter { flight ->
            val cal = Calendar.getInstance().apply { timeInMillis = flight.dateTimestamp }
            cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) + 1 == month
        }.sortedByDescending { it.dateTimestamp }
    }
    val duty = remember(dutyRecords, year, month) {
        dutyRecords.find { it.year == year && it.month == month }
    }
    val report = remember(monthlyFlights, duty, tariffs) {
        CalculationEngine.calculateReport(monthlyFlights, listOfNotNull(duty), tariffs)
    }
    val compact = LocalDensity.current.fontScale > 1.2f
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val previous = period.minusMonths(1)
                    onPeriodChange(previous.year, previous.monthValue)
                }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий месяц")
                }
                Text(
                    "${MonthNames[month - 1]} $year",
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = {
                    val next = period.plusMonths(1)
                    onPeriodChange(next.year, next.monthValue)
                }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Следующий месяц")
                }
            }
        }
        item {
            InstrumentCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "МИ-171",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(16.dp))
                    Icon(
                        painter = painterResource(R.drawable.mi171_silhouette),
                        contentDescription = "Силуэт вертолёта Ми-171",
                        modifier = Modifier.weight(1f).height(76.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Налёт за месяц", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val total = flightTimeDigits(report.totalMinutes)
                    // Leave room for long totals and Android's enlarged fonts.
                    val scale = LocalDensity.current.fontScale
                    val availableFont = maxWidth.value / (total.length * 0.68f * scale)
                    Text(
                        total,
                        fontSize = minOf(56f, availableFont).sp,
                        lineHeight = minOf(64f, availableFont * 1.15f).sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text("часы : минуты", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                if (compact) {
                    Metric("Земля", flightTimeDigits(report.totalLandMinutes))
                    Metric("Море", flightTimeDigits(report.totalSeaMinutes))
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Земля", flightTimeDigits(report.totalLandMinutes), Modifier.weight(1f))
                        Metric("Море", flightTimeDigits(report.totalSeaMinutes), Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            InstrumentCard {
                Text("За выбранный месяц", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (compact) {
                    Metric("Дежурства", "${duty?.dutyDays ?: 0} дн.")
                    Metric("Полётных дней", report.totalFlightDays.toString())
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Metric("Дежурства", "${duty?.dutyDays ?: 0} дн.", Modifier.weight(1f))
                        Metric("Полётных дней", report.totalFlightDays.toString(), Modifier.weight(1f))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text("Расчётная выплата", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    String.format(Locale.getDefault(), "%,.2f ₽", report.totalPayment),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("С дежурствами, после вычета 13% НДФЛ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Column {
                Text("Последние полёты", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onOpenJournal, contentPadding = PaddingValues(vertical = 8.dp)) {
                    Text("Журнал за месяц (${monthlyFlights.size})")
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }
        }
        if (monthlyFlights.isEmpty()) {
            item {
                InstrumentCard {
                    Text("За этот месяц полётов пока нет", style = MaterialTheme.typography.titleMedium)
                    Text("Добавьте полёт или выберите другой месяц. Импорт Excel доступен в журнале.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onAddFlight) { Text("Добавить первый полёт") }
                }
            }
        } else {
            items(monthlyFlights.take(5), key = { it.id }) { flight ->
                RecentFlight(flight, onClick = { onEditFlight(flight) })
            }
        }
    }
}

@Composable
private fun InstrumentCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentFlight(flight: FlightEntity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(formatDate(flight.dateTimestamp), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("№ ВС: ${flight.aircraftNumber}", style = MaterialTheme.typography.titleMedium)
            Text(
                flightTimeDigits(flight.landTimeMinutes + flight.seaTimeMinutes) + " · ч:мин",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            flight.missionNumber?.takeIf { it.isNotBlank() }?.let {
                Text("Задание: $it", style = MaterialTheme.typography.bodySmall)
            }
            if (flight.captain.isNotBlank()) {
                Text("КВС: ${flight.captain}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun flightTimeDigits(minutes: Int): String =
    String.format(Locale.getDefault(), "%d:%02d", minutes / 60, minutes % 60)
