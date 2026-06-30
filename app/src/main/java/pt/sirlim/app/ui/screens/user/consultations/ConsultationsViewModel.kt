package pt.sirlim.app.ui.screens.user.consultations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.*
import pt.sirlim.app.data.remote.SupabaseManager

class ConsultationsViewModel : ViewModel() {
    private val _cleanings = MutableStateFlow<List<Cleaning>>(emptyList())
    val cleanings: StateFlow<List<Cleaning>> = _cleanings

    private val _compartments = MutableStateFlow<Map<String, Compartment>>(emptyMap())
    val compartments: StateFlow<Map<String, Compartment>> = _compartments

    private val _groups = MutableStateFlow<Map<String, Group>>(emptyMap())
    val groups: StateFlow<Map<String, Group>> = _groups

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchUserHistory(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Fetch groups and compartments
                val grpList = SupabaseManager.client.postgrest["groups"].select().decodeList<Group>()
                _groups.value = grpList.associateBy { it.id ?: "" }

                val compList = SupabaseManager.client.postgrest["compartments"].select().decodeList<Compartment>()
                _compartments.value = compList.associateBy { it.id ?: "" }

                // 2. Fetch cleanings for this user
                val cleaningList = SupabaseManager.client.postgrest["cleanings"]
                    .select {
                        filter { filter("user_id", FilterOperator.EQ, userId) }
                    }.decodeList<Cleaning>()
                
                _cleanings.value = cleaningList.sortedByDescending { it.startTime }
            } catch (e: Exception) {
                _error.value = "Erro ao carregar histórico: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getPerformedTasks(cleaningId: String): List<Task> {
        return try {
            val response = SupabaseManager.client.postgrest["cleaning_performed_tasks"]
                .select {
                    filter { filter("cleaning_id", FilterOperator.EQ, cleaningId) }
                }
            val list = response.decodeList<pt.sirlim.app.ui.screens.user.consultations.CleaningPerformedTask>()
            val taskIds = list.map { it.taskId }
            
            if (taskIds.isNotEmpty()) {
                SupabaseManager.client.postgrest["tasks"]
                    .select {
                        filter { filter("id", FilterOperator.IN, taskIds) }
                    }.decodeList<Task>()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

@kotlinx.serialization.Serializable
data class CleaningPerformedTask(
    @kotlinx.serialization.SerialName("cleaning_id")
    val cleaningId: String,
    @kotlinx.serialization.SerialName("task_id")
    val taskId: String
)
