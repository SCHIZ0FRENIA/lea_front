package by.andros.lea_front.app.presentation.admin

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import by.andros.lea_front.auth.data.UserDto
import by.andros.lea_front.auth.domain.AuthRepository
import by.andros.lea_front.auth.domain.GetAllUsersUseCase
import by.andros.lea_front.auth.domain.GrantAdminRoleUseCase
import by.andros.lea_front.auth.domain.RegisterAdminUseCase
import by.andros.lea_front.auth.domain.RevokeAdminRoleUseCase
import by.andros.lea_front.auth.domain.DeleteUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val newAdminLogin: String = "",
    val newAdminPassword: String = "",
    val userIdToManage: String = "",
    val users: List<UserDto> = emptyList(),
    val isUserListDialogVisible: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val userToDelete: UserDto? = null
)

sealed class AdminEvent {
    data class CreateAdminAccount(val login: String, val password: String) : AdminEvent()
    data class GrantAdminRole(val userId: String) : AdminEvent()
    data class RevokeAdminRole(val userId: String) : AdminEvent()
    data class UpdateNewAdminLogin(val login: String) : AdminEvent()
    data class UpdateNewAdminPassword(val password: String) : AdminEvent()
    data class UpdateUserIdToManage(val userId: String) : AdminEvent()
    data object ClearMessages : AdminEvent()
    data object Logout : AdminEvent()
    data object FetchUsers : AdminEvent()
    data object ShowUserListDialog : AdminEvent()
    data object HideUserListDialog : AdminEvent()
    data class CopyUserIdToClipboard(val userId: String) : AdminEvent()
    data class ShowDeleteUserConfirmation(val user: UserDto) : AdminEvent()
    data object HideDeleteUserConfirmation : AdminEvent()
    data class DeleteUser(val userId: String) : AdminEvent()
    
    sealed class Navigation {
        data object ToLogin : Navigation()
    }
}

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val registerAdminUseCase: RegisterAdminUseCase,
    private val grantAdminRoleUseCase: GrantAdminRoleUseCase,
    private val revokeAdminRoleUseCase: RevokeAdminRoleUseCase,
    private val getAllUsersUseCase: GetAllUsersUseCase,
    private val deleteUserUseCase: DeleteUserUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AdminState())
    val state: StateFlow<AdminState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AdminEvent.Navigation>()
    val events: SharedFlow<AdminEvent.Navigation> = _events.asSharedFlow()

    init {
        // Fetch users on initialization
        fetchUsers()
    }

    fun onEvent(event: AdminEvent) {
        when (event) {
            is AdminEvent.CreateAdminAccount -> createAdminAccount(event.login, event.password)
            is AdminEvent.GrantAdminRole -> grantAdminRole(event.userId)
            is AdminEvent.RevokeAdminRole -> revokeAdminRole(event.userId)
            is AdminEvent.UpdateNewAdminLogin -> _state.update { it.copy(newAdminLogin = event.login) }
            is AdminEvent.UpdateNewAdminPassword -> _state.update { it.copy(newAdminPassword = event.password) }
            is AdminEvent.UpdateUserIdToManage -> _state.update { it.copy(userIdToManage = event.userId) }
            is AdminEvent.ClearMessages -> _state.update { it.copy(error = null, successMessage = null) }
            is AdminEvent.Logout -> logout()
            is AdminEvent.FetchUsers -> fetchUsers()
            is AdminEvent.ShowUserListDialog -> _state.update { it.copy(isUserListDialogVisible = true) }
            is AdminEvent.HideUserListDialog -> _state.update { it.copy(isUserListDialogVisible = false) }
            is AdminEvent.CopyUserIdToClipboard -> {
                // This is just a marker event. The actual clipboard functionality will be handled in the UI
                _state.update { it.copy(userIdToManage = event.userId) }
            }
            is AdminEvent.ShowDeleteUserConfirmation -> {
                _state.update { it.copy(showDeleteConfirmDialog = true, userToDelete = event.user) }
            }
            is AdminEvent.HideDeleteUserConfirmation -> {
                _state.update { it.copy(showDeleteConfirmDialog = false, userToDelete = null) }
            }
            is AdminEvent.DeleteUser -> deleteUser(event.userId)
        }
    }

    private fun fetchUsers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            getAllUsersUseCase()
                .onSuccess { users ->
                    _state.update { it.copy(isLoading = false, users = users) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                    Log.e("AdminViewModel", "Error fetching users", error)
                }
        }
    }
    
    private fun createAdminAccount(login: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            registerAdminUseCase(login, password)
                .onSuccess {
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            successMessage = "Admin account created successfully: $login",
                            newAdminLogin = "",
                            newAdminPassword = ""
                        )
                    }
                    // Refresh the user list after creating a new admin
                    fetchUsers()
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to create admin account"
                        )
                    }
                    Log.e("AdminViewModel", "Error creating admin account", error)
                }
        }
    }

    private fun grantAdminRole(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            grantAdminRoleUseCase(userId)
                .onSuccess { response ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            successMessage = "Admin role granted successfully to ${response.login}",
                            userIdToManage = ""
                        )
                    }
                    // Refresh the user list after updating a role
                    fetchUsers()
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to grant admin role"
                        )
                    }
                    Log.e("AdminViewModel", "Error granting admin role", error)
                }
        }
    }
    
    private fun revokeAdminRole(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            revokeAdminRoleUseCase(userId)
                .onSuccess { response ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            successMessage = "Admin role revoked successfully from ${response.login}",
                            userIdToManage = ""
                        )
                    }
                    // Refresh the user list after updating a role
                    fetchUsers()
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to revoke admin role"
                        )
                    }
                    Log.e("AdminViewModel", "Error revoking admin role", error)
                }
        }
    }
    
    private fun deleteUser(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, successMessage = null) }
            
            deleteUserUseCase(userId)
                .onSuccess { response ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            successMessage = "User deleted successfully: ${response.userId}",
                            userIdToManage = "",
                            showDeleteConfirmDialog = false,
                            userToDelete = null
                        )
                    }
                    // Refresh the user list after deleting a user
                    fetchUsers()
                }
                .onFailure { error ->
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = error.message ?: "Failed to delete user",
                            showDeleteConfirmDialog = false,
                            userToDelete = null
                        )
                    }
                    Log.e("AdminViewModel", "Error deleting user", error)
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.clearAuthData()
            _events.emit(AdminEvent.Navigation.ToLogin)
        }
    }
} 