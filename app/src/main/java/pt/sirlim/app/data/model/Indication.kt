package pt.sirlim.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Indication(
    val id: String? = null,
    @SerialName("compartment_id")
    val compartmentId: String,
    val urgency: Urgency,
    val instructions: String? = null,
    @SerialName("scheduled_date")
    val scheduledDate: String,
    @SerialName("is_completed")
    val isCompleted: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
enum class Urgency {
    @SerialName("ALTA") ALTA,
    @SerialName("MEDIA") MEDIA,
    @SerialName("BAIXA") BAIXA
}
