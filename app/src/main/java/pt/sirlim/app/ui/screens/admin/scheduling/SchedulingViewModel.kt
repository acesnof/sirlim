package pt.sirlim.app.ui.screens.admin.scheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.*
import pt.sirlim.app.data.remote.SupabaseManager
import java.time.DayOfWeek
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

class SchedulingViewModel : ViewModel() {
    private val _plans = MutableStateFlow<List<WeeklyPlan>>(emptyList())
    val plans: StateFlow<List<WeeklyPlan>> = _plans

    private val _planItems = MutableStateFlow<List<WeeklyPlanCompartment>>(emptyList())
    val planItems: StateFlow<List<WeeklyPlanCompartment>> = _planItems

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _userAssignments = MutableStateFlow<List<UserWeeklyPlan>>(emptyList())
    val userAssignments: StateFlow<List<UserWeeklyPlan>> = _userAssignments

    fun fetchPlans() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _plans.value = SupabaseManager.client.postgrest["weekly_plans"].select().decodeList<WeeklyPlan>()
                fetchUserAssignments()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchUserAssignments() {
        viewModelScope.launch {
            try {
                _userAssignments.value = SupabaseManager.client.postgrest["user_weekly_plans"]
                    .select().decodeList<UserWeeklyPlan>()
            } catch (e: Exception) {}
        }
    }

    fun fetchPlanDetails(planId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _planItems.value = SupabaseManager.client.postgrest["weekly_plan_compartments"]
                    .select { filter { filter("plan_id", FilterOperator.EQ, planId) } }
                    .decodeList<WeeklyPlanCompartment>()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun savePlan(id: String? = null, name: String, selectedUserIds: List<String>, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val planId = if (id == null) {
                    val res = SupabaseManager.client.postgrest["weekly_plans"].insert(WeeklyPlan(name = name)) {
                        select()
                    }.decodeSingle<WeeklyPlan>()
                    res.id!!
                } else {
                    SupabaseManager.client.postgrest["weekly_plans"].update({
                        WeeklyPlan::name setTo name
                    }) { filter { WeeklyPlan::id eq id } }
                    id
                }

                // Gestão de utilizadores: remover quem estava e colocar os novos
                // Mas a regra é 1 plano por user. 
                // Primeiro: remover este plano de todos os utilizadores que o tinham
                SupabaseManager.client.postgrest["user_weekly_plans"].delete {
                    filter { filter("plan_id", FilterOperator.EQ, planId) }
                }

                // Segundo: Inserir as novas atribuições (Supabase vai dar erro se o user já tiver outro plano se mudarmos a PK, 
                // por isso vamos remover o user de qualquer plano antes)
                if (selectedUserIds.isNotEmpty()) {
                    selectedUserIds.forEach { uid ->
                        SupabaseManager.client.postgrest["user_weekly_plans"].delete {
                            filter { filter("user_id", FilterOperator.EQ, uid) }
                        }
                    }
                    val assignments = selectedUserIds.map { UserWeeklyPlan(it, planId) }
                    SupabaseManager.client.postgrest["user_weekly_plans"].insert(assignments)
                }

                fetchPlans()
                onResult(true)
            } catch (e: Exception) {
                _error.value = e.message
                onResult(false)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePlan(planId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                SupabaseManager.client.postgrest["weekly_plans"].delete {
                    filter { filter("id", FilterOperator.EQ, planId) }
                }
                fetchPlans()
                onResult(true)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun addItem(item: WeeklyPlanCompartment) {
        viewModelScope.launch {
            try {
                SupabaseManager.client.postgrest["weekly_plan_compartments"].insert(item)
                fetchPlanDetails(item.plan_id)
            } catch (e: Exception) {}
        }
    }

    fun removeItem(itemId: String, planId: String) {
        viewModelScope.launch {
            try {
                SupabaseManager.client.postgrest["weekly_plan_compartments"].delete {
                    filter { filter("id", FilterOperator.EQ, itemId) }
                }
                fetchPlanDetails(planId)
            } catch (e: Exception) {}
        }
    }

    fun assignToUser(userId: String, planId: String) {
        viewModelScope.launch {
            try {
                SupabaseManager.client.postgrest["user_weekly_plans"].upsert(UserWeeklyPlan(userId, planId))
            } catch (e: Exception) {}
        }
    }

    // Lógica para o Utilizador: Verificar se já limpou esta semana
    suspend fun checkIfCleanedThisWeek(compartmentId: String): Boolean {
        return try {
            val now = ZonedDateTime.now()
            val lastMonday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().toString()
            
            val res = SupabaseManager.client.postgrest["cleanings"]
                .select {
                    filter {
                        filter("compartment_id", FilterOperator.EQ, compartmentId)
                        filter("start_time", FilterOperator.GTE, lastMonday)
                    }
                    limit(1)
                }
            res.data != "[]"
        } catch (e: Exception) {
            false
        }
    }
}
