package pt.sirlim.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Cleaning(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("compartment_id")
    val compartmentId: String,
    @SerialName("indication_id")
    val indicationId: String? = null,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String? = null,
    @SerialName("pause_duration_seconds")
    val pauseDurationSeconds: Int = 0,
    val observations: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
