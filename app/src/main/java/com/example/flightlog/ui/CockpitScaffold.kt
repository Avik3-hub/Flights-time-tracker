package com.example.flightlog.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CockpitScaffold(
    page: Int,
    onPageChange: (Int) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (page) { 1 -> "Журнал полётов"; 2 -> "Новый полёт"; 3 -> "Ещё"; else -> "Счётчик налёта" },
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = { Box(Modifier.padding(end = 14.dp)) { ThemeIndicator() } },
                navigationIcon = {
                    if (page == 2) {
                        IconButton(onClick = { onPageChange(0) }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад к обзору")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup()) {
                    listOf(0 to "Обзор", 1 to "Журнал", 3 to "Ещё").forEach { (index, title) ->
                        val selected = page == index
                        val tint = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        Column(Modifier.weight(1f).selectable(selected = selected, role = Role.Tab,
                            onClick = { onPageChange(index) }).padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(when(index) {
                                0 -> Icons.Outlined.Home
                                1 -> Icons.Outlined.Description
                                else -> Icons.Default.MoreHoriz
                            }, null, Modifier.size(25.dp), tint = tint)
                            Text(title, fontSize = 13.sp, lineHeight = 18.sp, color = tint)
                            Box(Modifier.width(28.dp).height(2.dp).background(
                                if (selected) tint else androidx.compose.ui.graphics.Color.Transparent))
                        }
                    }
                }
            }
        },
        content = content
    )
}
