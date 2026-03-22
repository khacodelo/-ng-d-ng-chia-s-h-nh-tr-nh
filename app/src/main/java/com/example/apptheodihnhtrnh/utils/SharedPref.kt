import android.content.Context

object SharedPref {

    fun saveToken(context: Context, token: String) {
        val pref = context.getSharedPreferences("app", Context.MODE_PRIVATE)
        pref.edit().putString("token", token).apply()
    }
}