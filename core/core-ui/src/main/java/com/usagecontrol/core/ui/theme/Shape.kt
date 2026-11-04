package com.usagecontrol.core.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Rows and sheets use 12dp corners; chips are fully round. */
val RowShape = RoundedCornerShape(12.dp)
val SheetShape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
val ChipShape = CircleShape

val DoormanShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RowShape,
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp),
)
