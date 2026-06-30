package pt.sirlim.app.ui.screens.admin.consultations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.*
import pt.sirlim.app.data.remote.SupabaseManager
import pt.sirlim.app.ui.screens.admin.groups_compartments.CompartmentTask
import pt.sirlim.app.ui.screens.admin.indications.IndicationTask
import pt.sirlim.app.ui.screens.admin.indications.IndicationUser
import pt.sirlim.app.ui.screens.user.consultations.CleaningPerformedTask
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AdminConsultationsViewModel : ViewModel() {
    private val _cleanings = MutableStateFlow<List<Cleaning>>(emptyList())
    val cleanings: StateFlow<List<Cleaning>> = _cleanings

    private val _compartments = MutableStateFlow<Map<String, Compartment>>(emptyMap())
    val compartments: StateFlow<Map<String, Compartment>> = _compartments

    private val _groups = MutableStateFlow<Map<String, Group>>(emptyMap())
    val groups: StateFlow<Map<String, Group>> = _groups

    private val _users = MutableStateFlow<Map<String, User>>(emptyMap())
    val users: StateFlow<Map<String, User>> = _users

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val grpList = SupabaseManager.client.postgrest["groups"].select().decodeList<Group>()
                _groups.value = grpList.associateBy { it.id ?: "" }

                val compList = SupabaseManager.client.postgrest["compartments"].select().decodeList<Compartment>()
                _compartments.value = compList.associateBy { it.id ?: "" }

                val userList = SupabaseManager.client.postgrest["users"].select().decodeList<User>()
                _users.value = userList.associateBy { it.id ?: "" }

                fetchAllCleanings()
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchAllCleanings() {
        try {
            val cleaningList = SupabaseManager.client.postgrest["cleanings"]
                .select().decodeList<Cleaning>()
            _cleanings.value = cleaningList.sortedByDescending { it.startTime }
        } catch (e: Exception) {
            _error.value = "Erro ao carregar limpezas: ${e.message}"
        }
    }

    fun deleteCleaning(cleaningId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                SupabaseManager.client.postgrest["cleanings"].delete {
                    filter { filter("id", FilterOperator.EQ, cleaningId) }
                }
                fetchAllCleanings()
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Erro ao apagar: ${e.message}"
                onResult(false)
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
            val list = response.decodeList<CleaningPerformedTask>()
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

    fun generateFullBackup(onResult: (SirlimBackup?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val users = SupabaseManager.client.postgrest["users"].select().decodeList<User>()
                val groups = SupabaseManager.client.postgrest["groups"].select().decodeList<Group>()
                val compartments = SupabaseManager.client.postgrest["compartments"].select().decodeList<Compartment>()
                val tasks = SupabaseManager.client.postgrest["tasks"].select().decodeList<Task>()
                val cleanings = SupabaseManager.client.postgrest["cleanings"].select().decodeList<Cleaning>()
                val indications = SupabaseManager.client.postgrest["indications"].select().decodeList<Indication>()
                
                val indicationUsers = SupabaseManager.client.postgrest["indication_users"].select().decodeList<IndicationUser>()
                val indicationTasks = SupabaseManager.client.postgrest["indication_tasks"].select().decodeList<IndicationTask>()
                val compartmentTasks = SupabaseManager.client.postgrest["compartment_tasks"].select().decodeList<CompartmentTask>()
                val cleaningPerformedTasks = SupabaseManager.client.postgrest["cleaning_performed_tasks"].select().decodeList<CleaningPerformedTask>()

                val backup = SirlimBackup(
                    exportDate = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    users = users,
                    groups = groups,
                    compartments = compartments,
                    tasks = tasks,
                    cleanings = cleanings,
                    indications = indications,
                    indicationUsers = indicationUsers,
                    indicationTasks = indicationTasks,
                    compartmentTasks = compartmentTasks,
                    cleaningPerformedTasks = cleaningPerformedTasks
                )
                onResult(backup)
            } catch (e: Exception) {
                _error.value = "Erro no Backup: ${e.message}"
                onResult(null)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
