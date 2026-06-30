package pt.sirlim.app.ui.screens.admin.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.Task
import pt.sirlim.app.data.remote.SupabaseManager

class TasksViewModel : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchTasks()
    }

    fun fetchTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = SupabaseManager.client.postgrest["tasks"]
                    .select()
                    .decodeList<Task>()
                // Ordenação Alfabética solicitada
                _tasks.value = result.sortedBy { it.name.lowercase() }
            } catch (e: Exception) {
                _error.value = "Erro ao carregar tarefas: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveTask(task: Task, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (task.id == null) {
                    SupabaseManager.client.postgrest["tasks"].insert(task)
                } else {
                    SupabaseManager.client.postgrest["tasks"].update(task) {
                        filter { Task::id eq task.id }
                    }
                }
                fetchTasks()
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Erro ao guardar tarefa: ${e.message}"
                onResult(false)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteTask(task: Task, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val taskId = task.id ?: return@launch
                SupabaseManager.client.postgrest["tasks"].delete {
                    filter { Task::id eq taskId }
                }
                fetchTasks()
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Erro ao apagar tarefa: ${e.message}"
                onResult(false)
            }
        }
    }
}
