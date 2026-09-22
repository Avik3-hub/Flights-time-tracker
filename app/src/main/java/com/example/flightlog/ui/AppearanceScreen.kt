package com.example.flightlog.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.flightlog.ui.theme.AppTheme

@Composable
fun ThemePickerDialog(selected: AppTheme, onSelect: (AppTheme) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оформление") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).selectableGroup()) {
                AppTheme.values().forEach { theme ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .selectable(selected = selected == theme, role = Role.RadioButton,
                                onClick = { onSelect(theme) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == theme, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(theme.title, style = MaterialTheme.typography.titleSmall)
                            Text(theme.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}

@Composable
fun MoreScreen(
    selectedTheme: AppTheme,
    onThemeClick: () -> Unit,
    onTariffsClick: () -> Unit,
    onDutyClick: () -> Unit,
    onJournalClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Настройки и данные", style = MaterialTheme.typography.headlineSmall)
        MoreAction("Оформление", selectedTheme.title, onThemeClick)
        MoreAction("Тарифы", "Ставки за полёты и дежурства", onTariffsClick)
        MoreAction("Дежурства", "Количество дней за месяц — под формой полёта", onDutyClick)
        MoreAction("Отчёты и Excel", "Фильтр периода, импорт и выгрузка — в журнале", onJournalClick)
        Text(
            "Режим «Как в системе» следует светлой или тёмной теме Android. " +
                "Автоматическое расписание задаётся в настройках телефона.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreAction(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
