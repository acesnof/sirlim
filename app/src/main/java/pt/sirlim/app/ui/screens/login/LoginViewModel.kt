package pt.sirlim.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.User
import pt.sirlim.app.data.model.UserRole
import pt.sirlim.app.data.remote.SupabaseManager

class LoginViewModel : ViewModel() {
    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchUsers()
    }

    fun fetchUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = SupabaseManager.client.postgrest["users"]
                    .select()
                    .decodeList<User>()
                
                // Ordenação solicitada: ADMIN -> USER -> VIEWER
                val sortedUsers = result.filter { it.isActive }.sortedWith(compareBy(
                    { when(it.role) {
                        UserRole.ADMIN -> 0
                        UserRole.USER -> 1
                        UserRole.VIEWER -> 2
                    }},
                    { it.username }
                ))
                
                if (sortedUsers.isEmpty()) {
                    _error.value = "A lista de utilizadores está vazia."
                } else {
                    _users.value = sortedUsers
                }
            } catch (e: Exception) {
                _error.value = "Erro ao carregar utilizadores: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyPin(user: User, pin: String): Boolean {
        return user.pin == pin
    }
}
