package pt.sirlim.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class WeeklyPlan(
    val id: String? = null,
    val name: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class WeeklyPlanCompartment(
    val id: String? = null,
    @SerialName("plan_id")
    val plan_id: String,
    @SerialName("compartment_id")
    val compartmentId: String,
    @SerialName("day_of_week")
    val dayOfWeek: Int, // 1=Seg, 7=Dom
    val instructions: String? = null
)

@Serializable
data class UserWeeklyPlan(
    @SerialName("user_id")
    val userId: String,
    @SerialName("plan_id")
    val planId: String
)
