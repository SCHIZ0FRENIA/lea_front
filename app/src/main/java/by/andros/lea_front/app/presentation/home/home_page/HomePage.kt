package by.andros.lea_front.app.presentation.home.home_page

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import by.andros.lea_front.app.presentation.components.RowButton

@Composable
fun HomePage(
    viewModel: HomePageViewModel = hiltViewModel()
) {
    RowButton(
        name = "Public",
        buttonText = "Show all",
        onButtonClick = {},
    )
}