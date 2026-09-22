package com.example.flightlog.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flightlog.R
import com.example.flightlog.data.db.DutyEntity
import com.example.flightlog.data.db.FlightEntity
import com.example.flightlog.data.db.TariffEntity
import com.example.flightlog.domain.CalculationEngine
import com.example.flightlog.domain.MonthlyReport
import com.example.flightlog.ui.theme.AppTheme
import com.example.flightlog.ui.theme.CockpitFont
import com.example.flightlog.ui.theme.LocalCockpitTheme
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
    OverviewContent(
        report = report, flights = monthlyFlights, dutyDays = duty?.dutyDays ?: 0,
        year = year, month = month, onPeriodChange = onPeriodChange,
        onOpenJournal = onOpenJournal, onEditFlight = onEditFlight, onAddFlight = onAddFlight
    )
}

/** The same production content is also used by the screenshot check. No demo data is stored. */
@Composable
fun OverviewContent(
    report: MonthlyReport,
    flights: List<FlightEntity>,
    dutyDays: Int,
    year: Int,
    month: Int,
    onPeriodChange: (Int, Int) -> Unit,
    onOpenJournal: () -> Unit,
    onEditFlight: (FlightEntity) -> Unit,
    onAddFlight: () -> Unit
) {
    val theme = LocalCockpitTheme.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp).padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when (theme) {
            AppTheme.CLASSIC -> {
                CockpitPanel {
                    MonthSelector(year, month, onPeriodChange)
                    Box(Modifier.fillMaxWidth().height(100.dp)) {
                        HelicopterArtwork(
                            Modifier.fillMaxWidth(0.52f).height(52.dp).align(Alignment.TopEnd),
                            alpha = 0.55f
                        )
                        Column(Modifier.fillMaxWidth(0.64f).align(Alignment.BottomStart)) {
                            Text("Налёт за месяц", fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TotalReadout(report.totalMinutes, MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Metrics(report, dutyDays)
                }
            }
            AppTheme.BLUE -> {
                Box(Modifier.fillMaxWidth().height(74.dp)) {
                    BlueHorizon(Modifier.matchParentSize())
                    HelicopterArtwork(
                        Modifier.fillMaxWidth(0.90f).height(74.dp).align(Alignment.CenterEnd)
                    )
                }
                CockpitPanel(padding = 4) { MonthSelector(year, month, onPeriodChange) }
                CockpitPanel(
                    gradient = listOf(Color(0xFF096FC5), Color(0xFF034F9D)),
                    border = Color(0xFF1267B3)
                ) {
                    Text("Налёт за месяц", color = Color.White, fontFamily = CockpitFont,
                        fontSize = 18.sp)
                    TotalReadout(report.totalMinutes, Color.White)
                    Metrics(report, dutyDays)
                }
            }
            else -> {
                HelicopterArtwork(Modifier.fillMaxWidth().height(96.dp), alpha = 0.46f)
                MonthSelector(year, month, onPeriodChange)
                CockpitPanel {
                    Text("Налёт за месяц", color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp)
                    TotalReadout(report.totalMinutes, MaterialTheme.colorScheme.primary)
                }
                Metrics(report, dutyDays)
            }
        }

        RecentFlightsPanel(flights, onEditFlight, onOpenJournal, onAddFlight)

        if (theme != AppTheme.AMOLED) {
            Button(
                onClick = onAddFlight,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(26.dp))
                Spacer(Modifier.width(8.dp))
                Text("Полёт", fontFamily = CockpitFont, fontSize = 22.sp)
            }
        }
        // Payment remains accessible without displacing the logbook below the fold.
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onOpenJournal).padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Расчётная выплата", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("С дежурствами, после НДФЛ", fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(String.format(Locale.getDefault(), "%,.2f ₽", report.totalPayment),
                fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
internal fun HelicopterArtwork(modifier: Modifier = Modifier, alpha: Float = 1f) {
    val blue = LocalCockpitTheme.current == AppTheme.BLUE
    // Multiplication preserves shading and panel detail; it is not a flat icon tint.
    val filter = if (blue) ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        0.17f, 0f, 0f, 0f, 0f,
        0f, 0.52f, 0f, 0f, 0f,
        0f, 0f, 0.90f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f
    ))) else null
    Image(
        painterResource(R.drawable.mi171_artwork),
        contentDescription = "Ми-171",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        alpha = alpha,
        colorFilter = filter
    )
}

@Composable
private fun BlueHorizon(modifier: Modifier) {
    Canvas(modifier) {
        val mountains = Path().apply {
            moveTo(0f, size.height * 0.83f)
            lineTo(size.width * 0.14f, size.height * 0.60f)
            lineTo(size.width * 0.25f, size.height * 0.76f)
            lineTo(size.width * 0.48f, size.height * 0.56f)
            lineTo(size.width * 0.66f, size.height * 0.72f)
            lineTo(size.width * 0.84f, size.height * 0.51f)
            lineTo(size.width, size.height * 0.68f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(mountains, Color(0xFFE1EFFB))
    }
}

@Composable
private fun MonthSelector(year: Int, month: Int, onChange: (Int, Int) -> Unit) {
    val period = YearMonth.of(year, month)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            val value = period.minusMonths(1); onChange(value.year, value.monthValue)
        }) { Icon(Icons.Default.ChevronLeft, "Предыдущий месяц") }
        Text(
            "${MonthNames[month - 1]} $year", modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            fontFamily = CockpitFont, fontSize = 18.sp
        )
        IconButton(onClick = {
            val value = period.plusMonths(1); onChange(value.year, value.monthValue)
        }) { Icon(Icons.Default.ChevronRight, "Следующий месяц") }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun TotalReadout(minutes: Int, color: Color) {
    val hours = (minutes / 60).toString()
    val remainder = String.format(Locale.getDefault(), "%02d", minutes % 60)
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        fun readout(scale: Float) = buildAnnotatedString {
            withStyle(SpanStyle(fontSize = (64f * scale).sp)) { append(hours) }
            withStyle(SpanStyle(fontSize = (24f * scale).sp)) { append(" ч ") }
            withStyle(SpanStyle(fontSize = (64f * scale).sp)) { append(remainder) }
            withStyle(SpanStyle(fontSize = (24f * scale).sp)) { append(" мин") }
        }
        val style = TextStyle(fontFamily = CockpitFont, color = color, letterSpacing = 0.sp)
        val size = measurer.measure(readout(1f), style = style, softWrap = false).size.width
        val width = with(LocalDensity.current) { maxWidth.toPx() }
        val scale = minOf(1f, width / size.coerceAtLeast(1))
        Text(readout(scale), style = style, maxLines = 1, softWrap = false,
            modifier = Modifier.padding(vertical = 4.dp))
    }
}

@Composable
private fun Metrics(report: MonthlyReport, dutyDays: Int) {
    val theme = LocalCockpitTheme.current
    val largeFont = LocalDensity.current.fontScale > 1.25f
    val metrics = listOf(
        Triple("Земля", flightTimeDigits(report.totalLandMinutes), Icons.Default.Landscape),
        Triple("Море", flightTimeDigits(report.totalSeaMinutes), Icons.Default.Waves),
        Triple("Дежурства", "$dutyDays дн.", Icons.Default.CalendarMonth)
    )
    if (largeFont) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.forEach { (label, value, icon) ->
                MetricTile(label, value, icon, theme, Modifier.fillMaxWidth())
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            metrics.forEach { (label, value, icon) ->
                MetricTile(label, value, icon, theme, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, icon: ImageVector, theme: AppTheme, modifier: Modifier) {
    val isBlue = theme == AppTheme.BLUE
    val fill = when (theme) {
        AppTheme.BLUE -> Color(0xFFEAF4FF)
        AppTheme.AMOLED -> Color(0xFF080A0C)
        else -> Color.Transparent
    }
    val text = if (isBlue) Color(0xFF0B3157) else MaterialTheme.colorScheme.primary
    val muted = if (isBlue) Color(0xFF315574) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = modifier,
        color = fill, shape = RoundedCornerShape(7.dp),
        border = if (theme == AppTheme.AMOLED) BorderStroke(1.dp, Color(0xFF202528)) else null
    ) {
        Column(Modifier.padding(horizontal = 7.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(icon, null, Modifier.size(15.dp), tint = muted)
                Text(label, fontSize = 11.sp, color = muted, maxLines = 1)
            }
            BoxWithConstraints {
                val fontScale = LocalDensity.current.fontScale
                val fontSize = minOf(25f, maxWidth.value / (value.length * 0.70f * fontScale))
                Text(value, fontFamily = CockpitFont, fontSize = fontSize.sp,
                    color = text, maxLines = 1, softWrap = false)
            }
        }
    }
}

@Composable
private fun RecentFlightsPanel(
    flights: List<FlightEntity>,
    onEdit: (FlightEntity) -> Unit,
    onJournal: () -> Unit,
    onAdd: () -> Unit
) {
    val night = LocalCockpitTheme.current == AppTheme.AMOLED
    CockpitPanel(padding = 0, spacing = 0) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text("Последние полёты", Modifier.weight(1f), fontSize = 15.sp,
                fontFamily = CockpitFont)
            if (night) {
                OutlinedButton(onClick = onAdd, shape = RoundedCornerShape(7.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                    Text("Полёт", fontSize = 12.sp)
                }
            } else {
                TextButton(onClick = onJournal, contentPadding = PaddingValues(4.dp)) {
                    Text("Все", fontSize = 12.sp)
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp))
                }
            }
        }
        if (flights.isEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("За этот месяц полётов пока нет", fontSize = 14.sp)
                TextButton(onClick = onAdd) { Text("Добавить полёт") }
            }
        } else {
            if (LocalDensity.current.fontScale <= 1.25f) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val muted = MaterialTheme.colorScheme.onSurfaceVariant
                    Text("ДАТА", Modifier.width(36.dp), fontSize = 9.sp, color = muted)
                    Text("БОРТ №", Modifier.width(82.dp), fontSize = 9.sp, color = muted)
                    Text("ЗАДАНИЕ", Modifier.weight(1f), fontSize = 9.sp, color = muted)
                    Text("НАЛЁТ", Modifier.width(50.dp), fontSize = 9.sp, color = muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
            flights.take(3).forEach { flight ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                CompactFlightRow(flight, onClick = { onEdit(flight) })
            }
        }
        if (night && flights.isNotEmpty()) {
            TextButton(onClick = onJournal, modifier = Modifier.align(Alignment.End)) {
                Text("Все полёты", fontSize = 12.sp)
                Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun CompactFlightRow(flight: FlightEntity, onClick: () -> Unit) {
    val date = formatDate(flight.dateTimestamp)
    val duration = flightTimeDigits(flight.landTimeMinutes + flight.seaTimeMinutes)
    val largeFont = LocalDensity.current.fontScale > 1.25f
    val blue = LocalCockpitTheme.current == AppTheme.BLUE
    val description = flight.missionNumber?.takeIf { it.isNotBlank() } ?: flight.captain
    if (!largeFont) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(date.take(5), Modifier.width(36.dp), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(flight.aircraftNumber, Modifier.width(82.dp), fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(description, Modifier.weight(1f), fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(duration, Modifier.width(50.dp), fontFamily = CockpitFont, fontSize = 17.sp,
                maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.End,
                color = if (blue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .semantics { contentDescription = "$date, ${flight.aircraftNumber}, $duration" }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(date.take(5), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(flight.aircraftNumber, fontSize = 14.sp,
                maxLines = if (largeFont) 2 else 1, overflow = TextOverflow.Ellipsis)
            if (description.isNotBlank()) {
                Text(description, fontSize = 11.sp, maxLines = if (largeFont) 2 else 1,
                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(duration, fontFamily = CockpitFont, fontSize = 18.sp,
            color = if (blue) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.ChevronRight, null, Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CockpitPanel(
    padding: Int = 14,
    spacing: Int = 8,
    gradient: List<Color>? = null,
    border: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable ColumnScope.() -> Unit
) {
    val fill = MaterialTheme.colorScheme.surface
    val colors = gradient ?: listOf(fill, fill.copy(alpha = 0.96f))
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, border)
    ) {
        Column(
            Modifier.background(Brush.linearGradient(colors)).padding(padding.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.dp),
            content = content
        )
    }
}

private fun flightTimeDigits(minutes: Int): String =
    String.format(Locale.getDefault(), "%d:%02d", minutes / 60, minutes % 60)
