package com.example.flightlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.flightlog.domain.MonthlyReport
import com.example.flightlog.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

internal val AviationPhrases = listOf(
    "Надёжность в высоте", "Небо любит точность", "Курс на горизонт",
    "Дом там, где ждут", "Каждый вылет — история", "Высота объединяет людей",
    "Спокойствие на каждом курсе", "Земля становится ближе", "В небе своя тишина",
    "Вместе над облаками", "За горизонтом новый день", "Работа выше облаков",
    "Крылья держатся на доверии", "У каждого неба характер", "Точность в каждом движении",
    "Полетели навстречу рассвету", "Возвращаемся к своим", "Небо начинается с земли",
    "Маршрут сквозь облака", "Ветер меняет пейзаж", "Высота расширяет взгляд",
    "Вертолёт связывает берега", "Север начинается с неба", "На связи с землёй",
    "Над тайгой новый рассвет", "В кабине общий ритм", "Курс на возвращение",
    "Память хранит каждый вылет", "Впереди знакомый горизонт", "Небо ближе, чем кажется",
    "Дорога через облака", "Горизонт зовёт вперёд"
)

@Composable
internal fun ThemeIndicator() {
    val theme = LocalCockpitTheme.current
    Surface(shape = RoundedCornerShape(10.dp), color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50)))
            Text(theme.title, fontFamily = CabinFont, fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun AircraftHeader() {
    val inspection = LocalInspectionMode.current
    var index by rememberSaveable {
        mutableIntStateOf(if (inspection) 0 else ((System.currentTimeMillis() / 45_000) % AviationPhrases.size).toInt())
    }
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    if (!inspection) LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { delay(45_000); index = (index + 1) % AviationPhrases.size }
        }
    }
    val theme = LocalCockpitTheme.current
    Column {
        HelicopterArtwork(Modifier.fillMaxWidth().height(100.dp),
            alpha = if (theme == AppTheme.BLUE) 0.90f else 0.48f)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text("Ми-8АМТ", fontSize = 11.sp, letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(AviationPhrases[index], Modifier.width(96.dp), fontSize = 10.sp,
                lineHeight = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun MonthlyTimePanel(report: MonthlyReport, compact: Boolean = false) {
    val blue = LocalCockpitTheme.current == AppTheme.BLUE
    val foreground = if (blue) Color.White else MaterialTheme.colorScheme.primary
    CockpitPanel(modifier = if (compact) Modifier.heightIn(min = 132.dp) else Modifier,
        gradient = if (blue) listOf(Color(0xFF096FC5), Color(0xFF034F9D)) else null) {
        Text("Налёт за месяц", fontSize = 15.sp, lineHeight = 20.sp,
            color = if (blue) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
        if (compact) {
            Text("%d:%02d".format(report.totalMinutes / 60, report.totalMinutes % 60),
                fontFamily = CockpitFont, fontSize = 44.sp, lineHeight = 54.sp, color = foreground, maxLines = 1)
            Text("часы : минуты", fontSize = 10.sp, lineHeight = 14.sp, color = foreground.copy(alpha = 0.7f))
        } else TotalReadout(report.totalMinutes, foreground)
    }
}

@Composable
internal fun FinancePanel(report: MonthlyReport, compact: Boolean = false) {
    CockpitPanel(modifier = if (compact) Modifier.heightIn(min = 132.dp) else Modifier,
        padding = if (compact) 10 else 14, spacing = 5) {
        Text("Расчётная выплата", fontSize = if (compact) 14.sp else 17.sp,
            lineHeight = if (compact) 18.sp else 23.sp)
        FinanceRow("Дежурства", report.dutyPayment, compact)
        FinanceRow("Полёты", report.landPayment + report.seaPayment, compact)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        FinanceRow("Всего", report.totalPayment, compact, total = true)
        Text("После НДФЛ", fontSize = 9.sp, lineHeight = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FinanceRow(label: String, amount: Double, compact: Boolean, total: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, Modifier.weight(0.8f), fontSize = if (compact) 10.sp else 14.sp,
            lineHeight = if (compact) 14.sp else 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(String.format(Locale("ru", "RU"), "%,.2f ₽", amount), Modifier.weight(1.5f),
            fontFamily = if (total) CockpitFont else CabinFont,
            fontSize = if (compact) 12.sp else 17.sp, maxLines = 2,
            lineHeight = if (compact) 16.sp else 23.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            color = if (total) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
