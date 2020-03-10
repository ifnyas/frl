package co.wangun.pnmfr.model

import android.graphics.PointF

class FaceResult {
    @kotlin.jvm.JvmField
    var midEye: PointF = PointF(0.0f, 0.0f)
    @kotlin.jvm.JvmField
    var confidence: Float
    @kotlin.jvm.JvmField
    var pose: Float
    @kotlin.jvm.JvmField
    var id = 0
    @kotlin.jvm.JvmField
    var time: Long
    var eyeDist: Float

    fun setFace(
        id: Int,
        midEye: PointF,
        eyeDist: Float,
        confidence: Float,
        pose: Float,
        time: Long
    ) {
        set(id, midEye, eyeDist, confidence, pose, time)
    }

    fun clear() {
        set(
            0, PointF(0.0f, 0.0f), 0.0f, 0.4f, 0.0f,
            System.currentTimeMillis()
        )
    }

    @Synchronized
    operator fun set(
        id: Int,
        midEye: PointF,
        eyeDist: Float,
        confidence: Float,
        pose: Float,
        time: Long
    ) {
        this.id = id
        this.midEye.set(midEye)
        this.eyeDist = eyeDist
        this.confidence = confidence
        this.pose = pose
        this.time = time
    }

    fun eyesDistance(): Float {
        return eyeDist
    }

    fun getMidPoint(pt: PointF) {
        pt.set(midEye)
    }

    init {
        eyeDist = 0.0f
        confidence = 0.4f
        pose = 0.0f
        time = System.currentTimeMillis()
    }
}