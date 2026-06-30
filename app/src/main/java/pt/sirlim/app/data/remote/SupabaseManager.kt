package pt.sirlim.app.data.remote

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseManager {
    const val PROJECT_URL = "https://kkdyjwojtfcjkcamiaxv.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_IdyXV7e_NIrJCpe_eUmU8w_hStACaeb"

    val client = createSupabaseClient(
        supabaseUrl = PROJECT_URL,
        supabaseKey = PUBLISHABLE_KEY
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
        install(Storage)
    }
}
