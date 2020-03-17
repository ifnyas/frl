package co.wangun.pnmfr.utils

import android.content.Context

class SessionManager(context: Context) {

    private val pref = context.getSharedPreferences("Preferences", 0)
    private val editor = pref.edit()

    // Put Session
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

    fun putPath(
        path: String
    ) {
        editor.putString("Path", path)
        editor.apply()
    }

    fun putConfidence(
        confidence: String
    ) {
        editor.putString("Confidence", confidence)
        editor.apply()
    }

    fun putStatus(
        status: Int
    ) {
        editor.putInt("Status", status)
        editor.apply()
    }

    fun putLoc(
        lat: String,
        lng: String
    ) {
        editor.putString("Lat", lat)
        editor.putString("Lng", lng)
        editor.apply()
    }

    // Get Session
    fun getPath(): String? {
        return pref.getString("Path", "No Path")
    }

    fun getName(): String? {
        return pref.getString("Name", "No Name")
    }

    fun getFace(): String? {
        return pref.getString("Face", "No Face")
    }

    fun getConfidence(): String? {
        return pref.getString("Confidence", "No Confidence")
    }

    fun getStatus(): Int? {
        return pref.getInt("Status", 0)
    }


    fun getLoc(loc: String): String? {
        val lat = pref.getString("Lat", "No Lat")
        val lng = pref.getString("Lng", "No Lng")

        return when (loc) {
            "lat" -> lat
            "lng" -> lng
            else -> "Lat: $lat, Lng: $lng"
        }
    }
}


