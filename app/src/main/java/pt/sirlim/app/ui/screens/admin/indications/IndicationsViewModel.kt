package pt.sirlim.app.ui.screens.admin.indications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.*
import pt.sirlim.app.data.remote.SupabaseManager

class IndicationsViewModel : ViewModel() {
    private val _indications = MutableStateFlow<List<Indication>>(emptyList())
    val indications: StateFlow<List<Indication>> = _indications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchIndications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = SupabaseManager.client.postgrest["indications"]
                    .select().decodeList<Indication>()
                _indications.value = result
            } catch (e: Exception) {
                _error.value = "Erro ao carregar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // FUNÇÃO RESTAURADA PARA O UTILIZADOR
    fun fetchUserIndications(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val userAssoc = SupabaseManager.client.postgrest["indication_users"]
                    .select { filter { filter("user_id", FilterOperator.EQ, userId) } }
                    .decodeList<IndicationUser>()
                
                if (userAssoc.isNotEmpty()) {
                    val indIds = userAssoc.map { it.indicationId }
                    val result = SupabaseManager.client.postgrest["indications"]
                        .select {
                            filter { filter("id", FilterOperator.IN, indIds) }
                        }.decodeList<Indication>()
                    _indications.value = result
                } else {
                    _indications.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveIndication(
        indication: Indication, 
        selectedUserIds: List<String>, 
        selectedTaskIds: List<String>, 
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val usersToProcess = if (indication.id == null) selectedUserIds else listOf(selectedUserIds.first())

                usersToProcess.forEach { userId ->
                    val indId = if (indication.id == null) {
                        val res = SupabaseManager.client.postgrest["indications"].insert(indication) {
                            select()
                        }.decodeSingle<Indication>()
                        res.id!!
                    } else {
                        SupabaseManager.client.postgrest["indications"].update(indication) {
                            filter { Indication::id eq (indication.id) }
                        }
                        indication.id
                    }

                    SupabaseManager.client.postgrest["indication_users"].delete { filter { filter("indication_id", FilterOperator.EQ, indId) } }
                    SupabaseManager.client.postgrest["indication_users"].insert(mapOf("indication_id" to indId, "user_id" to userId))

                    try {
                        SupabaseManager.client.postgrest["indication_tasks"].delete { filter { filter("indication_id", FilterOperator.EQ, indId) } }
                        if (selectedTaskIds.isNotEmpty()) {
                            val taskAssoc = selectedTaskIds.map { mapOf("indication_id" to indId, "task_id" to it) }
                            SupabaseManager.client.postgrest["indication_tasks"].insert(taskAssoc)
                        }
                    } catch (e: Exception) {
                        println("Erro (não fatal) nas tarefas: ${e.message}")
                    }
                }

                fetchIndications()
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Erro Supabase: ${e.message}"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun getIndicationUsers(indicationId: String): List<String> {
        return try {
            val res = SupabaseManager.client.postgrest["indication_users"]
                .select { filter { filter("indication_id", FilterOperator.EQ, indicationId) } }
            res.decodeList<IndicationUser>().map { it.userId }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getIndicationTasks(indicationId: String): List<String> {
        return try {
            val res = SupabaseManager.client.postgrest["indication_tasks"]
                .select { filter { filter("indication_id", FilterOperator.EQ, indicationId) } }
            res.decodeList<IndicationTask>().map { it.taskId }
        } catch (e: Exception) { emptyList() }
    }

    fun deleteIndication(indicationId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Com o ON DELETE CASCADE/SET NULL no SQL, basta apagar a indicação
                SupabaseManager.client.postgrest["indications"].delete {
                    filter { filter("id", FilterOperator.EQ, indicationId) }
                }
                fetchIndications()
                onResult(true)
            } catch (e: Exception) {
                _error.value = e.message
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}

@kotlinx.serialization.Serializable
data class IndicationUser(
    @kotlinx.serialization.SerialName("indication_id") val indicationId: String,
    @kotlinx.serialization.SerialName("user_id") val userId: String
)

@kotlinx.serialization.Serializable
data class IndicationTask(
    @kotlinx.serialization.SerialName("indication_id") val indicationId: String,
    @kotlinx.serialization.SerialName("task_id") val taskId: String
)
