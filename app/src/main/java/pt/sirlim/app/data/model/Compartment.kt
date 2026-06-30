package pt.sirlim.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class Compartment(
    val id: String? = null,
    val name: String,
    @SerialName("group_id")
    val groupId: String? = null,
    val description: String? = null,
    @SerialName("qr_code_key")
    val qrCodeKey: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null
)
