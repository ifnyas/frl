package co.wangun.pnmfr.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.StrictMode
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


object BmpConverter {

    @Throws(IllegalArgumentException::class)
    fun convert(base64Str: String): Bitmap {

        val decodedBytes = Base64.decode(
            base64Str.substring(base64Str.indexOf(",") + 1), Base64.DEFAULT
        )
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

    @JvmStatic
    fun convert(bitmap: Bitmap): String {

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    @JvmStatic
    fun saveImage(bmp: Bitmap, name: String, context: Context) {

        val sessionManager = SessionManager(context)
        val path = sessionManager.getPath()
        val myDir = File(path!!)
        myDir.mkdirs()

        val fileName = "$name.jpg"
        val file = File(myDir, fileName)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun downloadImage(link: String, context: Context) {

        // TODO: please change this to async
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        // not recommended method

        try {
            val url = URL(link)
            val connection = url
                .openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            val input = connection.inputStream
            val bmp = BitmapFactory.decodeStream(input)
            saveImage(bmp, "register", context)
            Log.d("BmpConverter", "image saved")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}