package pt.sirlim.app.ui.screens.user.cleaning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.*
import pt.sirlim.app.data.remote.SupabaseManager
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CleaningViewModel : ViewModel() {
    private val _compartment = MutableStateFlow<Compartment?>(null)
    val compartment: StateFlow<Compartment?> = _compartment

    private val _groupName = MutableStateFlow<String?>(null)
    val groupName: StateFlow<String?> = _groupName

    private val _lastCleaningInfo = MutableStateFlow<String?>(null)
    val lastCleaningInfo: StateFlow<String?> = _lastCleaningInfo

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _existingIndication = MutableStateFlow<Indication?>(null)
    val existingIndication: StateFlow<Indication?> = _existingIndication

    private val _secondsElapsed = MutableStateFlow(0)
    val secondsElapsed: StateFlow<Int> = _secondsElapsed

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused

    private val _pauseSeconds = MutableStateFlow(0)
    val pauseSeconds: StateFlow<Int> = _pauseSeconds

    private val _startTime = MutableStateFlow<ZonedDateTime?>(null)
    val startTime: StateFlow<ZonedDateTime?> = _startTime

    private var timerJob: Job? = null

    fun loadCompartmentByQr(qrKey: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val compResult = SupabaseManager.client.postgrest["compartments"]
                    .select {
                        filter { filter("qr_code_key", FilterOperator.EQ, qrKey) }
                    }.decodeSingleOrNull<Compartment>()
                
                if (compResult == null) {
                    _error.value = "QR Code inválido ou compartimento não encontrado."
                    return@launch
                }
                _compartment.value = compResult
                checkForDailyIndication(compResult.id!!, userId)
                processCompartmentData(compResult)
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun checkForDailyIndication(compId: String, userId: String) {
        try {
            val today = LocalDate.now().toString()
            val response = SupabaseManager.client.postgrest["indications"]
                .select {
                    filter {
                        filter("compartment_id", FilterOperator.EQ, compId)
                        filter("scheduled_date", FilterOperator.EQ, today)
                        filter("is_completed", FilterOperator.EQ, false)
                    }
                }.decodeList<Indication>()
            
            if (response.isNotEmpty()) {
                val indIds = response.map { it.id!! }
                val userAssoc = SupabaseManager.client.postgrest["indication_users"]
                    .select {
                        filter {
                            filter("user_id", FilterOperator.EQ, userId)
                            filter("indication_id", FilterOperator.IN, indIds)
                        }
                    }.decodeList<pt.sirlim.app.ui.screens.admin.indications.IndicationUser>()
                
                if (userAssoc.isNotEmpty()) {
                    _existingIndication.value = response.find { it.id == userAssoc.first().indicationId }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadCompartmentById(compId: String, indicationId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val compResult = SupabaseManager.client.postgrest["compartments"]
                    .select {
                        filter { filter("id", FilterOperator.EQ, compId) }
                    }.decodeSingleOrNull<Compartment>()
                _compartment.value = compResult
                if (indicationId != null) {
                    val ind = SupabaseManager.client.postgrest["indications"]
                        .select { filter { filter("id", FilterOperator.EQ, indicationId) } }
                        .decodeSingleOrNull<Indication>()
                    _existingIndication.value = ind
                }
                compResult?.let { processCompartmentData(it) }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun processCompartmentData(comp: Compartment) {
        if (comp.groupId != null) {
            val group = SupabaseManager.client.postgrest["groups"]
                .select { filter { filter("id", FilterOperator.EQ, comp.groupId) } }
                .decodeSingleOrNull<Group>()
            _groupName.value = group?.name
        } else {
            _groupName.value = "Sem Grupo"
        }
        fetchLastCleaning(comp.id!!)
        fetchTasks(comp.id)
    }

    private suspend fun fetchLastCleaning(compId: String) {
        try {
            val response = SupabaseManager.client.postgrest["cleanings"]
                .select {
                    filter { filter("compartment_id", FilterOperator.EQ, compId) }
                    order("start_time", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    limit(1)
                }
            
            if (response.data != "[]") {
                val lastCleaning = response.decodeSingle<Cleaning>()
                val userRes = SupabaseManager.client.postgrest["users"]
                    .select { filter { filter("id", FilterOperator.EQ, lastCleaning.userId) } }
                    .decodeSingleOrNull<User>()
                
                val date = ZonedDateTime.parse(lastCleaning.startTime)
                    .withZoneSameInstant(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                
                // NOVO: Mostrar Nome Completo se disponível
                val cleanerName = if (userRes?.fullName.isNullOrBlank()) userRes?.username else userRes?.fullName
                _lastCleaningInfo.value = "Última: $date por $cleanerName"
            } else {
                _lastCleaningInfo.value = "Sem histórico de limpezas."
            }
        } catch (e: Exception) {
            _lastCleaningInfo.value = null
        }
    }

    private suspend fun fetchTasks(compId: String) {
        val tasksIdsResponse = SupabaseManager.client.postgrest["compartment_tasks"]
            .select {
                filter { filter("compartment_id", FilterOperator.EQ, compId) }
            }
        val taskIds = tasksIdsResponse.decodeList<pt.sirlim.app.ui.screens.admin.groups_compartments.CompartmentTask>()
        
        if (taskIds.isNotEmpty()) {
            val tasksResult = SupabaseManager.client.postgrest["tasks"]
                .select {
                    filter { filter("id", FilterOperator.IN, taskIds.map { it.taskId }) }
                }.decodeList<Task>()
            _tasks.value = tasksResult
        } else {
            _tasks.value = emptyList()
        }
    }

    fun startCleaning() {
        _startTime.value = ZonedDateTime.now(ZoneId.systemDefault())
        _secondsElapsed.value = 0
        _pauseSeconds.value = 0
        _isPaused.value = false
        startTimer()
    }

    fun resumeTimer() {
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_isPaused.value) {
                    _secondsElapsed.value++
                } else {
                    _pauseSeconds.value++
                }
            }
        }
    }

    fun togglePause() {
        _isPaused.value = !_isPaused.value
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    fun saveCleaning(userId: String, selectedTaskIds: List<String>, observations: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val start = _startTime.value ?: ZonedDateTime.now(ZoneId.systemDefault())
                val end = ZonedDateTime.now(ZoneId.systemDefault())
                
                val cleaning = Cleaning(
                    userId = userId,
                    compartmentId = _compartment.value?.id ?: "",
                    indicationId = _existingIndication.value?.id,
                    startTime = start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    endTime = end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    pauseDurationSeconds = _pauseSeconds.value,
                    observations = observations
                )

                val result = SupabaseManager.client.postgrest["cleanings"].insert(cleaning) {
                    select()
                }.decodeSingle<Cleaning>()

                _existingIndication.value?.id?.let { indId ->
                    SupabaseManager.client.postgrest["indications"].update({
                        Indication::isCompleted setTo true
                    }) {
                        filter { Indication::id eq indId }
                    }
                }

                if (selectedTaskIds.isNotEmpty()) {
                    val tasksPerformed = selectedTaskIds.map { taskId ->
                        mapOf("cleaning_id" to result.id, "task_id" to taskId)
                    }
                    SupabaseManager.client.postgrest["cleaning_performed_tasks"].insert(tasksPerformed)
                }
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Erro ao guardar limpeza: ${e.message}"
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
