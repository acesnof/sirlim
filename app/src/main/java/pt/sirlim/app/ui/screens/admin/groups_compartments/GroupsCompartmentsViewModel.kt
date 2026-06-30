package pt.sirlim.app.ui.screens.admin.groups_compartments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.Group
import pt.sirlim.app.data.model.Compartment
import pt.sirlim.app.data.model.Task
import pt.sirlim.app.data.remote.SupabaseManager
import java.text.Normalizer

class GroupsCompartmentsViewModel : ViewModel() {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private val _compartments = MutableStateFlow<List<Compartment>>(emptyList())
    val compartments: StateFlow<List<Compartment>> = _compartments

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val groupsResult = SupabaseManager.client.postgrest["groups"].select().decodeList<Group>()
                _groups.value = groupsResult.sortedBy { it.name }

                val compartmentsResult = SupabaseManager.client.postgrest["compartments"].select().decodeList<Compartment>()
                
                val sortedCompartments = compartmentsResult.sortedWith(compareBy(
                    { comp -> _groups.value.find { it.id == comp.groupId }?.name ?: "ZZZ" },
                    { it.name }
                ))
                _compartments.value = sortedCompartments

                val tasksResult = SupabaseManager.client.postgrest["tasks"].select().decodeList<Task>()
                _tasks.value = tasksResult.sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveGroup(group: Group, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (group.id == null) {
                    SupabaseManager.client.postgrest["groups"].insert(group)
                } else {
                    // Update explícito para evitar problemas com booleanos
                    SupabaseManager.client.postgrest["groups"].update({
                        Group::name setTo group.name
                        Group::isActive setTo group.isActive
                    }) {
                        filter { Group::id eq group.id }
                    }
                }
                fetchData()
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Erro ao guardar grupo: ${e.message}"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteGroup(group: Group, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val hasCompartments = _compartments.value.any { it.groupId == group.id }
                if (hasCompartments) {
                    _error.value = "Não é possível apagar: o grupo ainda tem compartimentos."
                    onResult(false)
                } else {
                    SupabaseManager.client.postgrest["groups"].delete {
                        filter { Group::id eq group.id!! }
                    }
                    fetchData()
                    onResult(true)
                }
            } catch (e: Exception) {
                _error.value = "Erro ao apagar grupo: ${e.message}"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveCompartment(compartment: Compartment, selectedTaskIds: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val compId = if (compartment.id == null) {
                    val insertResult = SupabaseManager.client.postgrest["compartments"].insert(compartment) {
                        select()
                    }.decodeSingle<Compartment>()
                    insertResult.id!!
                } else {
                    // UPDATE EXPLÍCITO: Garante que is_active (isActive) é enviado como true ou false
                    SupabaseManager.client.postgrest["compartments"].update({
                        Compartment::name setTo compartment.name
                        Compartment::groupId setTo compartment.groupId
                        Compartment::description setTo compartment.description
                        Compartment::qrCodeKey setTo compartment.qrCodeKey
                        Compartment::isActive setTo compartment.isActive // CRUCIAL
                    }) {
                        filter { Compartment::id eq compartment.id }
                    }
                    compartment.id
                }

                // Atualizar tarefas associadas
                SupabaseManager.client.postgrest["compartment_tasks"].delete {
                    filter { filter("compartment_id", FilterOperator.EQ, compId) }
                }

                if (selectedTaskIds.isNotEmpty()) {
                    val associations = selectedTaskIds.map { taskId ->
                        mapOf("compartment_id" to compId, "task_id" to taskId)
                    }
                    SupabaseManager.client.postgrest["compartment_tasks"].insert(associations)
                }

                fetchData()
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Erro ao guardar: ${e.message}"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCompartment(compartment: Compartment, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = SupabaseManager.client.postgrest["cleanings"]
                    .select {
                        filter { filter("compartment_id", FilterOperator.EQ, compartment.id!!) }
                        limit(1)
                    }
                
                if (response.data != "[]") {
                    _error.value = "Não é possível apagar: existem limpezas registadas neste compartimento."
                    onResult(false)
                } else {
                    SupabaseManager.client.postgrest["compartments"].delete {
                        filter { Compartment::id eq compartment.id!! }
                    }
                    fetchData()
                    onResult(true)
                }
            } catch (e: Exception) {
                _error.value = "Erro ao apagar: ${e.message}"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getCompartmentTasks(compartmentId: String): List<String> {
        return try {
            val response = SupabaseManager.client.postgrest["compartment_tasks"]
                .select {
                    filter { filter("compartment_id", FilterOperator.EQ, compartmentId) }
                }
            val list = response.decodeList<CompartmentTask>()
            list.map { it.taskId }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun generateQrCodeKey(compartmentName: String, groupName: String?): String {
        val base = if (!groupName.isNullOrBlank() && groupName != "Sem Grupo") {
            compartmentName.lowercase() + groupName.lowercase()
        } else {
            compartmentName.lowercase()
        }
        return Normalizer.normalize(base, Normalizer.Form.NFD)
            .replace("\\p{M}".toRegex(), "")
            .replace(" ", "")
    }
}

@kotlinx.serialization.Serializable
data class CompartmentTask(
    @kotlinx.serialization.SerialName("compartment_id")
    val compartmentId: String,
    @kotlinx.serialization.SerialName("task_id")
    val taskId: String
)
