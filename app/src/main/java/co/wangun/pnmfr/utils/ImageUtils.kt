package co.wangun.pnmfr.utils

import android.graphics.*
import co.wangun.pnmfr.model.FaceResult

object ImageUtils {

    //Rotate Bitmap
    @JvmStatic
    fun rotate(bmp: Bitmap, degrees: Float): Bitmap {
        var b = bmp
        if (degrees != 0f) {
            val m = Matrix()
            m.setRotate(
                degrees, b.width.toFloat() / 2,
                b.height.toFloat() / 2
            )
            val b2 = Bitmap.createBitmap(
                b, 0, 0, b.width,
                b.height, m, true
            )
            if (b != b2) {
                b.recycle()
                b = b2
            }
        }
        return b
    }

    private fun cropBitmap(bitmap: Bitmap, rect: Rect): Bitmap {
        val w = rect.right - rect.left
        val h = rect.bottom - rect.top
        val ret = Bitmap.createBitmap(w, h, bitmap.config)
        val canvas = Canvas(ret)
        canvas.drawBitmap(bitmap, -rect.left.toFloat(), -rect.top.toFloat(), null)
        bitmap.recycle()
        return ret
    }

    @JvmStatic
    fun cropFace(face: FaceResult, bitmap: Bitmap, rotate: Int): Bitmap {
        var bmp: Bitmap
        val eyesDis = face.eyesDistance()
        val mid = PointF()
        face.getMidPoint(mid)
        val rect = Rect(
            (mid.x - eyesDis * 1.20f).toInt(),
            (mid.y - eyesDis * 0.55f).toInt(),
            (mid.x + eyesDis * 1.20f).toInt(),
            (mid.y + eyesDis * 1.85f).toInt()
        )
        var config: Bitmap.Config? = Bitmap.Config.RGB_565
        if (bitmap.config != null) config = bitmap.config
        bmp = bitmap.copy(config, true)
        when (rotate) {
            90 -> bmp = rotate(bmp, 90f)
            180 -> bmp = rotate(bmp, 180f)
            270 -> bmp = rotate(bmp, 270f)
        }
        bmp = cropBitmap(bmp, rect)
        return bmp
    }
}