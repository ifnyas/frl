package co.wangun.pnmfr.utils

import android.content.Context
import android.graphics.*
import android.util.TypedValue
import android.view.View
import co.wangun.pnmfr.model.FaceResult

//This class is a simple View to display the faces.
class FaceOverlayView(context: Context?) : View(context) {

    private var mPaint: Paint? = null
    private var mTextPaint: Paint? = null
    private var mDisplayOrientation = 0
    private var mOrientation = 0
    private var previewWidth = 0
    private var previewHeight = 0
    private var mFaces: Array<FaceResult>? = null
    private var fps = 0.0
    private var isFront = false

    private fun initialize() {
        // We want a white box around the face:
        val metrics = resources.displayMetrics
        val stroke = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2f, metrics).toInt()
        mPaint = Paint()
        mPaint!!.isAntiAlias = true
        mPaint!!.isDither = true
        mPaint!!.color = Color.WHITE
        mPaint!!.strokeWidth = stroke.toFloat()
        mPaint!!.style = Paint.Style.STROKE
        mTextPaint = Paint()
        mTextPaint!!.isAntiAlias = true
        mTextPaint!!.isDither = true
        val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 15f, metrics).toInt()
        mTextPaint!!.textSize = size.toFloat()
        mTextPaint!!.color = Color.WHITE
        mTextPaint!!.style = Paint.Style.FILL
    }

    fun setFPS(fps: Double) {
        this.fps = fps
    }

    fun setFaces(faces: Array<FaceResult>?) {
        mFaces = faces
        invalidate()
    }

    fun setDisplayOrientation(displayOrientation: Int) {
        mDisplayOrientation = displayOrientation
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mFaces != null && mFaces!!.size > 0) {
            var scaleX = width.toFloat() / previewWidth.toFloat()
            var scaleY = height.toFloat() / previewHeight.toFloat()
            when (mDisplayOrientation) {
                90, 270 -> {
                    scaleX = width.toFloat() / previewHeight.toFloat()
                    scaleY = height.toFloat() / previewWidth.toFloat()
                }
            }
            canvas.save()
            canvas.rotate(-mOrientation.toFloat())
            val rectF = RectF()
            for (face in mFaces!!) {
                val mid = PointF()
                face.getMidPoint(mid)
                if (mid.x != 0.0f && mid.y != 0.0f) {
                    val eyesDis = face.eyesDistance()
                    rectF.set(
                        RectF(
                            (mid.x - eyesDis * 1.2f) * scaleX,
                            (mid.y - eyesDis * 0.65f) * scaleY,
                            (mid.x + eyesDis * 1.2f) * scaleX,
                            (mid.y + eyesDis * 1.75f) * scaleY
                        )
                    )
                    if (isFront) {
                        val left = rectF.left
                        val right = rectF.right
                        rectF.left = width - right
                        rectF.right = width - left
                    }
                    canvas.drawRect(rectF, mPaint!!)
                }
            }
            canvas.restore()
        }
    }

    fun setPreviewWidth(previewWidth: Int) {
        this.previewWidth = previewWidth
    }

    fun setPreviewHeight(previewHeight: Int) {
        this.previewHeight = previewHeight
    }

    fun setFront(front: Boolean) {
        isFront = front
    }

    init {
        initialize()
    }
}