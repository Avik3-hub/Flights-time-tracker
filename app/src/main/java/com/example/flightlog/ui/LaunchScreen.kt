package com.example.flightlog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flightlog.R
import com.example.flightlog.ui.theme.CockpitFont

@Composable
fun LaunchScreen() {
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Image(painterResource(R.drawable.mi171_artwork), "Ми-8АМТ",
                Modifier.fillMaxWidth().aspectRatio(3f), contentScale = ContentScale.Fit)
            Text("СЧЕТЧИК НАЛЕТА", fontFamily = CockpitFont, fontSize = 25.sp,
                letterSpacing = 3.sp, color = Color(0xFFBDBAB5))
        }
    }
}
