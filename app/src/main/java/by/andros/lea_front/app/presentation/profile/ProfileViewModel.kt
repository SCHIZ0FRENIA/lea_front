package by.andros.lea_front.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.app.data.AnswerCount
import by.andros.lea_front.app.data.SessionStats
import by.andros.lea_front.app.data.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val sessionStats: SessionStats? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val statisticsRepository: StatisticsRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()
    
    init {
        loadStatistics()
    }
    
    private fun loadStatistics() {
        viewModelScope.launch {
            _state.value = ProfileState(isLoading = true)
            
            statisticsRepository.getSessionStats()
                .catch { e -> 
                    _state.value = ProfileState(
                        isLoading = false,
                        error = "Failed to load statistics: ${e.message}"
                    )
                }
                .collect { stats ->
                    _state.value = ProfileState(
                        isLoading = false,
                        sessionStats = stats
                    )
                }
        }
    }
} 