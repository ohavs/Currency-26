package com.example.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import java.io.File

// Relative to tools/preview, where the tasks run.
private const val DIR = "../../app/src/main/res/font"

val Rubik = FontFamily(
    Font(File("$DIR/rubik_regular.ttf"), FontWeight.Normal),
    Font(File("$DIR/rubik_medium.ttf"), FontWeight.Medium),
    Font(File("$DIR/rubik_semibold.ttf"), FontWeight.SemiBold),
    Font(File("$DIR/rubik_bold.ttf"), FontWeight.Bold),
)
