package co.wangun.pnmfr.utils

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

    fun putLoc(
        lat: String,
        long: String
    ) {
        editor.putString("Lat", lat)
        editor.putString("Long", long)
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

    fun getLoc(loc: String): String? {
        val lat = preferences.getString("Lat", "No Lat")
        val long = preferences.getString("Long", "No Long")

        return when (loc) {
            "lat" -> {
                lat
            }
            "long" -> {
                long
            }
            else -> {
                "Lat: $lat, Long: $long"
            }
        }
    }

    fun getMobile(i: Int): String? {
        return preferences.getString("Mobile$i", "No Mobile")
    }
}


