package pt.sirlim.app.ui.screens.admin.application.accounts

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import pt.sirlim.app.data.model.User
import pt.sirlim.app.data.remote.SupabaseManager
import java.util.UUID

class AccountsViewModel : ViewModel() {
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun saveUser(user: User, imageUri: Uri?, contentResolver: ContentResolver?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                var finalUser = user
                
                // Upload image if exists
                if (imageUri != null && contentResolver != null) {
                    val bytes = contentResolver.openInputStream(imageUri)?.readBytes()
                    if (bytes != null) {
                        val fileName = "profile_${UUID.randomUUID()}.jpg"
                        val bucket = SupabaseManager.client.storage["images"]
                        bucket.upload(fileName, bytes)
                        val publicUrl = bucket.publicUrl(fileName)
                        finalUser = user.copy(photoUrl = publicUrl)
                    }
                }

                if (finalUser.id == null) {
                    SupabaseManager.client.postgrest["users"].insert(finalUser)
                } else {
                    SupabaseManager.client.postgrest["users"].update(finalUser) {
                        filter {
                            User::id eq finalUser.id
                        }
                    }
                }
                onResult(true)
            } catch (e: Exception) {
                _error.value = "Erro ao guardar utilizador: ${e.message}"
                onResult(false)
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun deleteUser(user: User, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            _error.value = null
            try {
                val userId = user.id ?: return@launch
                val response = SupabaseManager.client.postgrest["cleanings"]
                    .select {
                        filter {
                            filter("user_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, userId)
                        }
                    }
                
                if (response.data != "[]") {
                    _error.value = "Não é possível apagar: utilizador tem registos de limpeza associados."
                    onResult(false)
                } else {
                    SupabaseManager.client.postgrest["users"].delete {
                        filter {
                            User::id eq userId
                        }
                    }
                    onResult(true)
                }
            } catch (e: Exception) {
                _error.value = "Erro ao apagar utilizador: ${e.message}"
                onResult(false)
            } finally {
                _isDeleting.value = false
            }
        }
    }
}
