package by.andros.lea_front.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun RowButton(
    name: String = "Default name",
    buttonText: String = "Default button",
    onButtonClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            fontSize = 24.sp,
            letterSpacing = 0.5.sp,
        )
        TextButton(
            onClick = onButtonClick
        ) {
            Text(
                text = buttonText,
                fontSize = 18.sp,
                letterSpacing = 0.5.sp,
            )
        }
    }
}