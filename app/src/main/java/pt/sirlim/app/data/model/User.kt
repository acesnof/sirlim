package pt.sirlim.app.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class User(
    val id: String? = null,
    val username: String,
    val pin: String,
    val description: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("photo_url")
    val photoUrl: String? = null,
    val role: UserRole,
    @SerialName("is_active")
    val isActive: Boolean = true
)

@Serializable
enum class UserRole {
    @SerialName("ADMIN") ADMIN,
    @SerialName("USER") USER,
    @SerialName("VIEWER") VIEWER
}
