package co.wangun.pnmfr.utils

import android.app.Activity
import android.graphics.Point
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.Log
import android.view.Surface
import kotlin.math.abs

object DisplayUtils {

    private const val TAG = "DisplayUtils"

    // Gets the current display rotation in angles.
    @JvmStatic
    fun getDisplayRotation(activity: Activity): Int {
        val rotation = activity.windowManager.defaultDisplay
            .rotation
        when (rotation) {
            Surface.ROTATION_0 -> return 0
            Surface.ROTATION_90 -> return 90
            Surface.ROTATION_180 -> return 180
            Surface.ROTATION_270 -> return 270
        }
        return 0
    }

    @JvmStatic
    fun getDisplayOrientation(degrees: Int, cameraId: Int): Int {
        // See android.hardware.Camera.setDisplayOrientation for documentation.
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        return result
    }

    @JvmStatic
    fun getOptimalPreviewSize(
        currentActivity: Activity,
        sizes: List<Camera.Size>?,
        targetRatio: Double
    ): Camera.Size? {

        // Use a very small tolerance because we want an exact match.
        val aspectTolerance = 0.001
        if (sizes == null) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        // Because of bugs of overlay and layout, we sometimes will try to
        // layout the viewfinder in the portrait orientation and thus get the
        // wrong size of preview surface. When we change the preview size, the
        // new overlay will be created before the old one closed, which causes
        // an exception. For now, just get the screen size.
        val point =
            getDefaultDisplaySize(currentActivity, Point())
        val targetHeight = Math.min(point.x, point.y)

        // Try to find an size match aspect ratio and size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (abs(ratio - targetRatio) > aspectTolerance) continue
            if (abs(size.height - targetHeight) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height - targetHeight).toDouble()
            }
        }

        // Cannot find the one match the aspect ratio. This should not happen.
        // Ignore the requirement.
        if (optimalSize == null) {
            Log.w(TAG, "No preview size match the aspect ratio")
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height - targetHeight).toDouble()
                }
            }
        }
        return optimalSize
    }

    private fun getDefaultDisplaySize(
        activity: Activity,
        size: Point
    ): Point {
        val d = activity.windowManager.defaultDisplay
        d.getSize(size)
        return size
    }
}