package co.wangun.pnmfr.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;

import com.luxand.FSDK;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;

import co.wangun.pnmfr.R;
import co.wangun.pnmfr.model.FaceResult;
import co.wangun.pnmfr.utils.BmpConverter;
import co.wangun.pnmfr.utils.DisplayUtils;
import co.wangun.pnmfr.utils.FaceOverlayView;
import co.wangun.pnmfr.utils.ImageUtils;
import co.wangun.pnmfr.utils.SessionManager;

/**
 * FACE DETECT EVERY FRAME WIL CONVERT TO GRAY BITMAP SO THIS HAS HIGHER PERFORMANCE THAN RGB BITMAP
 * COMPARE FPS (DETECT FRAME PER SECOND) OF 2 METHODs FOR MORE DETAIL
 */

public final class FaceActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.ErrorCallback {

    //FaceActivity
    public static final String TAG = FaceActivity.class.getSimpleName();
    private static final int MAX_FACE = 10;

    // fps detect face (not FPS of camera)
    long start, end;
    int counter = 0;
    double fps;

    // Number of Cameras in device.
    private int numberOfCameras;
    private Camera mCamera;
    private int cameraId = 1;

    // Let's keep track of the display rotation and orientation also:
    private int mDisplayRotation;
    private int mDisplayOrientation;
    private int previewWidth;
    private int previewHeight;

    // The surface view for the camera data
    private SurfaceView mView;

    // Draw rectangles and other fancy stuff:
    private FaceOverlayView mFaceView;
    private boolean isThreadWorking = false;
    private Handler handler;
    private FaceDetectThread detectThread = null;
    private int prevSettingWidth;
    private int prevSettingHeight;
    private android.media.FaceDetector fdet;
    private byte[] grayBuff;
    private int bufflen;
    private int[] rgbs;
    private FaceResult[] faces;
    private FaceResult[] faces_previous;
    private int Id = 0;
    private String BUNDLE_CAMERA_ID = "camera";

    private HashMap<Integer, Integer> facesCount = new HashMap<>();

    private SessionManager sessionManager;

    // onCreate
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //yas
        sessionManager = new SessionManager(this);

        setContentView(R.layout.activity_face);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mView = findViewById(R.id.surfaceview);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Create Face Overlay View:
        mFaceView = new FaceOverlayView(this);
        addContentView(mFaceView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        handler = new Handler();
        faces = new FaceResult[MAX_FACE];
        faces_previous = new FaceResult[MAX_FACE];
        for (int i = 0; i < MAX_FACE; i++) {
            faces[i] = new FaceResult();
            faces_previous[i] = new FaceResult();
        }

        if (icicle != null)
            cameraId = icicle.getInt(BUNDLE_CAMERA_ID, 0);

        // init btn
        initBtn();

        // show dialog if user doesn't have a registered face
        /*
        String from = getIntent().getStringExtra("from");
        if (from.equals("register")) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(FaceActivity.this);
            dialog.setMessage("Wajah kamu belum pernah terdaftar sebelumnya")
                    .setPositiveButton("Daftar", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(FaceActivity.this, FaceActivity.class);
                            intent.putExtra("from", "register");
                            startActivity(intent);
                            finish();
                        }
                    })
                    .setNegativeButton("Lain Kali", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .create()
                    .show();

         */
    }

    private void initBtn() {

        ImageButton backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                finish();
            }
        });

        ImageButton camBtn = findViewById(R.id.camBtn);
        camBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (numberOfCameras == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(FaceActivity.this);
                    builder.setTitle("Pilih Kamera")
                            .setMessage("Device hanya punya satu kamera")
                            .setNeutralButton("OK", null)
                            .create()
                            .show();
                }
                cameraId = (cameraId + 1) % numberOfCameras;
                recreate();
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Check for the camera permission before accessing the camera
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
    }

    /**
     * Restarts the camera.
     */
    @Override
    protected void onResume() {
        super.onResume();

        Log.i(TAG, "onResume");
        startPreview();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detector, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CAMERA_ID, cameraId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                if (cameraId == 0) cameraId = i;
            }
        }

        mCamera = Camera.open(cameraId);

        Camera.getCameraInfo(cameraId, cameraInfo);
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            mFaceView.setFront(true);
        }

        try {
            mCamera.setPreviewDisplay(mView.getHolder());
        } catch (Exception e) {
            Log.e(TAG, "Could not preview the image.", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // We have no surface, return immediately:
        if (surfaceHolder.getSurface() == null) {
            return;
        }
        // Try to stop the current preview:
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // Ignore...
        }

        configureCamera(width, height);
        setDisplayOrientation();
        setErrorCallback();

        // Create media.FaceDetector
        float aspect = (float) previewHeight / (float) previewWidth;
        fdet = new android.media.FaceDetector(prevSettingWidth, (int) (prevSettingWidth * aspect), MAX_FACE);

        bufflen = previewWidth * previewHeight;
        grayBuff = new byte[bufflen];
        rgbs = new int[bufflen];

        // Everything is configured! Finally start the camera preview again:
        startPreview();
    }

    private void setErrorCallback() {
        mCamera.setErrorCallback(this);
    }

    private void setDisplayOrientation() {
        // Now set the display orientation:
        mDisplayRotation = DisplayUtils.getDisplayRotation(FaceActivity.this);
        mDisplayOrientation = DisplayUtils.getDisplayOrientation(mDisplayRotation, cameraId);

        mCamera.setDisplayOrientation(mDisplayOrientation);

        if (mFaceView != null) {
            mFaceView.setDisplayOrientation(mDisplayOrientation);
        }
    }

    private void configureCamera(int width, int height) {
        Camera.Parameters parameters = mCamera.getParameters();
        // Set the PreviewSize and AutoFocus:
        setOptimalPreviewSize(parameters, width, height);
        setAutoFocus(parameters);
        // And set the parameters:
        mCamera.setParameters(parameters);
    }

    private void setOptimalPreviewSize(Camera.Parameters cameraParameters, int width, int height) {
        List<Camera.Size> previewSizes = cameraParameters.getSupportedPreviewSizes();
        float targetRatio = (float) width / height;
        Camera.Size previewSize = DisplayUtils.getOptimalPreviewSize(this, previewSizes, targetRatio);
        previewWidth = previewSize.width;
        previewHeight = previewSize.height;

        Log.e(TAG, "previewWidth" + previewWidth);
        Log.e(TAG, "previewHeight" + previewHeight);

        /**
         * Calculate size to scale full frame bitmap to smaller bitmap
         * Detect face in scaled bitmap have high performance than full bitmap.
         * The smaller image size -> detect faster, but distance to detect face shorter,
         * so calculate the size follow your purpose
         */
        if (previewWidth / 4 > 360) {
            prevSettingWidth = 360;
            prevSettingHeight = 270;
        } else if (previewWidth / 4 > 320) {
            prevSettingWidth = 320;
            prevSettingHeight = 240;
        } else if (previewWidth / 4 > 240) {
            prevSettingWidth = 240;
            prevSettingHeight = 160;
        } else {
            prevSettingWidth = 160;
            prevSettingHeight = 120;
        }

        cameraParameters.setPreviewSize(previewSize.width, previewSize.height);

        mFaceView.setPreviewWidth(previewWidth);
        mFaceView.setPreviewHeight(previewHeight);
    }

    private void setAutoFocus(Camera.Parameters cameraParameters) {
        List<String> focusModes = cameraParameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
            cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    private void startPreview() {
        if (mCamera != null) {
            isThreadWorking = false;
            mCamera.startPreview();
            mCamera.setPreviewCallback(this);
            counter = 0;
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        mCamera.setPreviewCallbackWithBuffer(null);
        mCamera.setErrorCallback(null);
        mCamera.release();
        mCamera = null;
    }

    @Override
    public void onPreviewFrame(byte[] _data, Camera _camera) {
        if (!isThreadWorking) {
            if (counter == 0)
                start = System.currentTimeMillis();

            isThreadWorking = true;
            waitForFdetThreadComplete();
            detectThread = new FaceDetectThread(handler, this);
            detectThread.setData(_data);
            detectThread.start();
        }
    }

    private void waitForFdetThreadComplete() {
        if (detectThread == null) {
            return;
        }

        if (detectThread.isAlive()) {
            try {
                detectThread.join();
                detectThread = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onError(int error, Camera camera) {
        Log.e(TAG, "Encountered an unexpected camera error: " + error);
    }

    /**
     * Matching faces
     */
    private void matchingFaces() {

        // init fsdk
        FSDK.ActivateLibrary(getString(R.string.license));
        FSDK.Initialize();

        // init variables
        String path = sessionManager.getPath();
        int w = getResources().getInteger(R.integer.width);

        // get face 1
        FSDK.HImage face1 = new FSDK.HImage();
        FSDK.LoadImageFromFile(face1, path + "/register.jpg");
        FSDK.FSDK_FaceTemplate faceTemp1 = new FSDK.FSDK_FaceTemplate();
        FSDK.TFacePosition facePosi1 = new FSDK.TFacePosition();
        facePosi1.w = w;
        facePosi1.xc = w / 2;
        facePosi1.yc = w / 2;
        FSDK.GetFaceTemplateInRegion(face1, facePosi1, faceTemp1);

        // get face 2
        FSDK.HImage face2 = new FSDK.HImage();
        FSDK.LoadImageFromFile(face2, path + "/recognize.jpg");
        FSDK.FSDK_FaceTemplate faceTemp2 = new FSDK.FSDK_FaceTemplate();
        FSDK.TFacePosition facePosi2 = new FSDK.TFacePosition();
        facePosi2.w = w;
        facePosi2.xc = w / 2;
        facePosi2.yc = w / 2;
        FSDK.GetFaceTemplateInRegion(face2, facePosi2, faceTemp2);

        // matching
        float[] similarity = new float[1];
        float[] threshold = new float[1];
        float farValue = 0.05f; // False Acceptance Rate 5%; the less the more secure

        FSDK.GetMatchingThresholdAtFAR(farValue, threshold);
        FSDK.MatchFaces(faceTemp1, faceTemp2, similarity);

        if (similarity[0] > threshold[0]) {
            sessionManager.putStatus(1);
            startActivity(new Intent(FaceActivity.this, ResultActivity.class));
        } else {
            sessionManager.putStatus(0);
            startActivity(new Intent(FaceActivity.this, ResultActivity.class));
        }

        sessionManager.putConfidence(Float.toString(similarity[0]));
        finish();

        // debug
        Log.d("FA", "threshold: " + threshold[0] + " " + "similarity: " + similarity[0]);
    }

    /**
     * Do face detect in thread
     */
    private class FaceDetectThread extends Thread {
        private Handler handler;
        private byte[] data = null;
        private Bitmap faceCroped;

        public FaceDetectThread(Handler handler, Context ctx) {
            this.handler = handler;
        }


        public void setData(byte[] data) {
            this.data = data;
        }

        public void run() {
//            Log.i("FaceDetectThread", "running");

            float aspect = (float) previewHeight / (float) previewWidth;
            int w = prevSettingWidth;
            int h = (int) (prevSettingWidth * aspect);

            // start Gray Photos
            //ByteBuffer bbuffer = ByteBuffer.wrap(data);
            //bbuffer.get(grayBuff, 0, bufflen);
            //gray8toRGB32(grayBuff, previewWidth, previewHeight, rgbs);
            // end Gray photos

            Bitmap bitmap = Bitmap.createBitmap(rgbs, previewWidth, previewHeight, Bitmap.Config.RGB_565);

            // start RGB Photos
            YuvImage yuv = new YuvImage(data, ImageFormat.NV21,
                    bitmap.getWidth(), bitmap.getHeight(), null);
            Rect rectImage = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            if (!yuv.compressToJpeg(rectImage, 100, baout)) {
                Log.e("CreateBitmap", "compressToJpeg failed");
            }
            BitmapFactory.Options bfo = new BitmapFactory.Options();
            bfo.inPreferredConfig = Bitmap.Config.RGB_565;
            bitmap = BitmapFactory.decodeStream(
                    new ByteArrayInputStream(baout.toByteArray()), null, bfo);
            // end RGB Photos

            if (w % 2 == 1) {
                w -= 1;
            }
            if (h % 2 == 1) {
                h -= 1;
            }

            Bitmap bmp = Bitmap.createScaledBitmap(bitmap, w, h, false);

            float xScale = (float) previewWidth / (float) prevSettingWidth;
            float yScale = (float) previewHeight / (float) h;

            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int rotate = mDisplayOrientation;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT && mDisplayRotation % 180 == 0) {
                if (rotate + 180 > 360) {
                    rotate = rotate - 180;
                } else
                    rotate = rotate + 180;
            }

            switch (rotate) {
                case 90:
                    bmp = ImageUtils.rotate(bmp, 90);
                    xScale = (float) previewHeight / bmp.getWidth();
                    yScale = (float) previewWidth / bmp.getHeight();
                    break;
                case 180:
                    bmp = ImageUtils.rotate(bmp, 180);
                    break;
                case 270:
                    bmp = ImageUtils.rotate(bmp, 270);
                    xScale = (float) previewHeight / (float) h;
                    yScale = (float) previewWidth / (float) prevSettingWidth;
                    break;
            }

            fdet = new android.media.FaceDetector(bmp.getWidth(), bmp.getHeight(), MAX_FACE);

            android.media.FaceDetector.Face[] fullResults = new android.media.FaceDetector.Face[MAX_FACE];
            fdet.findFaces(bmp, fullResults);

            for (int i = 0; i < MAX_FACE; i++) {
                if (fullResults[i] == null) {
                    faces[i].clear();
                } else {
                    PointF mid = new PointF();
                    fullResults[i].getMidPoint(mid);

                    mid.x *= xScale;
                    mid.y *= yScale;

                    float eyesDis = fullResults[i].eyesDistance() * xScale;
                    float confidence = fullResults[i].confidence();
                    float pose = fullResults[i].pose(android.media.FaceDetector.Face.EULER_Y);
                    int idFace = Id;

                    Rect rect = new Rect(
                            (int) (mid.x - eyesDis * 1.20f),
                            (int) (mid.y - eyesDis * 0.55f),
                            (int) (mid.x + eyesDis * 1.20f),
                            (int) (mid.y + eyesDis * 1.85f));

                    // Only detect face size > 20x20
                    if (rect.height() * rect.width() > 20 * 20) {
                        // Check this face and previous face have same ID?
                        for (int j = 0; j < MAX_FACE; j++) {
                            float eyesDisPre = faces_previous[j].eyesDistance();
                            PointF midPre = new PointF();
                            faces_previous[j].getMidPoint(midPre);

                            RectF rectCheck = new RectF(
                                    (midPre.x - eyesDisPre * 1.5f),
                                    (midPre.y - eyesDisPre * 1.15f),
                                    (midPre.x + eyesDisPre * 1.5f),
                                    (midPre.y + eyesDisPre * 1.85f));

                            if (rectCheck.contains(mid.x, mid.y) && (System.currentTimeMillis() - faces_previous[j].time) < 1000) {
                                idFace = faces_previous[j].id;
                                break;
                            }
                        }

                        if (idFace == Id) Id++;

                        faces[i].setFace(idFace, mid, eyesDis, confidence, pose, System.currentTimeMillis());

                        faces_previous[i].set(faces[i].id, faces[i].midEye, faces[i].eyesDistance(), faces[i].confidence, faces[i].pose, faces[i].time);

                        // if focus in a face 5 frame -> take picture face display
                        // because of some first frame have low quality
                        if (facesCount.get(idFace) == null) {
                            facesCount.put(idFace, 0);
                        } else {
                            int count = facesCount.get(idFace) + 1;
                            if (count <= 5)
                                facesCount.put(idFace, count);

                            // Crop Face to display in RecylerView
                            if (count == 5) {

                                faceCroped = ImageUtils.cropFace(faces[i], bitmap, rotate);

                                if (faceCroped != null) {

                                    handler.post(new Runnable() {

                                        public void run() {

                                            int w = getResources().getInteger(R.integer.width);
                                            Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                                                    faceCroped, w, w, false);
                                            String from = getIntent().getStringExtra("from");
                                            BmpConverter.saveImage(resizedBitmap, from, FaceActivity.this);

                                            if (from.equals("register")) {
                                                AlertDialog.Builder builder = new AlertDialog.Builder(FaceActivity.this);
                                                builder.setMessage("Wajah kamu sudah terdaftar. Sekarang kamu bisa presensi melalui fitur wajah.")
                                                        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                Intent intent = new Intent(FaceActivity.this, FaceActivity.class);
                                                                intent.putExtra("from", "recognize");
                                                                startActivity(intent);
                                                                finish();
                                                            }
                                                        })
                                                        .create()
                                                        .show();
                                                //Toast.makeText(getApplicationContext(), "Muka berhasil didaftarkan", Toast.LENGTH_LONG).show();
                                            } else {
                                                matchingFaces();
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    }
                }
            }

            handler.post(new Runnable() {
                public void run() {
                    //send face to FaceView to draw rect
                    mFaceView.setFaces(faces);

                    //Calculate FPS (Detect Frame per Second)
                    end = System.currentTimeMillis();
                    counter++;
                    double time = (double) (end - start) / 1000;
                    if (time != 0)
                        fps = counter / time;

                    mFaceView.setFPS(fps);

                    if (counter == (Integer.MAX_VALUE - 1000))
                        counter = 0;

                    isThreadWorking = false;
                }
            });
        }

        private void gray8toRGB32(byte[] gray8, int width, int height, int[] rgb_32s) {
            final int endPtr = width * height;
            int ptr = 0;
            while (ptr != endPtr) {
                final int Y = gray8[ptr] & 0xff;
                rgb_32s[ptr] = 0xff000000 + (Y << 16) + (Y << 8) + Y;
                ptr++;
            }
        }
    }
}
