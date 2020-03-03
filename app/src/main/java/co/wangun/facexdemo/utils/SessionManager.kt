package co.wangun.facexdemo.utils

import android.content.Context
import org.json.JSONArray

class SessionManager(context: Context) {

    private val preferences = context.getSharedPreferences("Preferences", 0)
    private val editor = preferences.edit()

    //Init Session
    //
    fun clearSession() {
        editor.clear()
        editor.apply()
    }

    fun putFace(
        face: String
    ) {
        editor.putString("Face", face)
        editor.apply()
    }

    fun putName(
        name: String
    ) {
        editor.putString("Name", name)
        editor.apply()
    }

    fun putUserDetail(
        mobiles: JSONArray,
        mobilesSize: Int
    ) {
        for (i in 0 until mobilesSize) {
            val mobile = mobiles.getString(i)
            editor.putString("Mobile$i", mobile)
        }
        editor.apply()
    }

    //Get Session
    //
    fun getName(): String? {
        return preferences.getString("Name", "No Name")
    }

    fun getFace(): String? {
        return preferences.getString("Face", "No Face")
    }

    fun getMobile(i: Int): String? {
        return preferences.getString("Mobile$i", "No Mobile")
    }
}


