package co.wangun.pnmfr;

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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.luxand.FSDK;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import co.wangun.pnmfr.adapter.ImagePreviewAdapter;
import co.wangun.pnmfr.api.ApiClient;
import co.wangun.pnmfr.api.BaseApiService;
import co.wangun.pnmfr.model.FaceResult;
import co.wangun.pnmfr.utils.BmpConverter;
import co.wangun.pnmfr.utils.DisplayUtils;
import co.wangun.pnmfr.utils.FaceOverlayView;
import co.wangun.pnmfr.utils.ImageUtils;
import co.wangun.pnmfr.utils.SessionManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * FACE DETECT EVERY FRAME WIL CONVERT TO GRAY BITMAP SO THIS HAS HIGHER PERFORMANCE THAN RGB BITMAP
 * COMPARE FPS (DETECT FRAME PER SECOND) OF 2 METHODs FOR MORE DETAIL
 */

public final class FaceDetectActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.ErrorCallback {

    //FaceDetectActivity
    public static final String TAG = FaceDetectActivity.class.getSimpleName();
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
    private RecyclerView recyclerView;

    private ImagePreviewAdapter imagePreviewAdapter;
    private ArrayList<Bitmap> facesBitmap;

    // yas
    private BaseApiService mApiService;
    private SessionManager sessionManager;

    // onCreate
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //yas
        sessionManager = new SessionManager(this);

        setContentView(R.layout.activity_camera_viewer);
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

        recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        handler = new Handler();
        faces = new FaceResult[MAX_FACE];
        faces_previous = new FaceResult[MAX_FACE];
        for (int i = 0; i < MAX_FACE; i++) {
            faces[i] = new FaceResult();
            faces_previous[i] = new FaceResult();
        }

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Scan");

        if (icicle != null)
            cameraId = icicle.getInt(BUNDLE_CAMERA_ID, 0);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Check for the camera permission before accessing the camera
        SurfaceHolder holder = mView.getHolder();
        holder.addCallback(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_camera, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            case R.id.switchCam:
                if (numberOfCameras == 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Switch Camera").setMessage("Your device have one camera").setNeutralButton("Close", null);
                    AlertDialog alert = builder.create();
                    alert.show();
                    return true;
                }
                cameraId = (cameraId + 1) % numberOfCameras;
                recreate();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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
        resetData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(BUNDLE_CAMERA_ID, cameraId);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        //Find the total number of cameras available
        resetData();

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
        mDisplayRotation = DisplayUtils.getDisplayRotation(FaceDetectActivity.this);
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

    /**
     * Release Memory
     */
    private void resetData() {
        if (imagePreviewAdapter == null) {
            facesBitmap = new ArrayList<>();
            imagePreviewAdapter = new ImagePreviewAdapter(
                    FaceDetectActivity.this,
                    facesBitmap,
                    new ImagePreviewAdapter.ViewHolder.OnItemClickListener() {
                        @Override
                        public void onClick(View v, int position) {
                            imagePreviewAdapter.setCheck(position);
                            //DialogForm(position);
                        }
                    }
            );
            recyclerView.setAdapter(imagePreviewAdapter);
        } else {
            imagePreviewAdapter.clearAll();
        }
    }

    @Override
    public void onError(int error, Camera camera) {
        Log.e(TAG, "Encountered an unexpected camera error: " + error);
    }

    /**
     * Create Register Dialog
     */
    private void DialogForm(final int i) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(FaceDetectActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_image_preview, null);
        final EditText nameEdit = dialogView.findViewById(R.id.name_edit);

        dialog.setView(dialogView);
        dialog.setCancelable(true);
        dialog.setTitle("Register");

        dialog.setPositiveButton("SUBMIT", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                String name = nameEdit.getText().toString();
                imagePreviewAdapter.setName(name);

                sessionManager.putName(name);
                sessionManager.putFace(imagePreviewAdapter.setBmp(i));

                Log.d("AAA Name", sessionManager.getName());
                Log.d("AAA Face", sessionManager.getFace());

                imagePreviewAdapter.notifyDataSetChanged();
                dialog.dismiss();
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void matchingFaces() {

        // init fsdk
        FSDK.ActivateLibrary(getString(R.string.license));
        FSDK.Initialize();

        // get face 1
        FSDK.HImage face1 = new FSDK.HImage();
        FSDK.LoadImageFromFile(face1, "/storage/emulated/0/" + getString(R.string.app_name) + "/register.jpg");
        FSDK.FSDK_FaceTemplate faceTemp1 = new FSDK.FSDK_FaceTemplate();
        FSDK.TFacePosition facePosi1 = new FSDK.TFacePosition();
        facePosi1.xc = 256;
        facePosi1.yc = 256;
        facePosi1.w = 512;
        FSDK.GetFaceTemplateInRegion(face1, facePosi1, faceTemp1);

        // get face 2
        FSDK.HImage face2 = new FSDK.HImage();
        FSDK.LoadImageFromFile(face2, "/storage/emulated/0/" + getString(R.string.app_name) + "/recognize.jpg");
        FSDK.FSDK_FaceTemplate faceTemp2 = new FSDK.FSDK_FaceTemplate();
        FSDK.TFacePosition facePosi2 = new FSDK.TFacePosition();
        facePosi2.xc = 256;
        facePosi2.yc = 256;
        facePosi2.w = 512;
        FSDK.GetFaceTemplateInRegion(face2, facePosi2, faceTemp2);

        // matching
        float[] similarity = new float[1];
        float[] threshold = new float[1];

        FSDK.GetMatchingThresholdAtFAR(0.1f, threshold);
        FSDK.MatchFaces(faceTemp1, faceTemp2, similarity);

        if (similarity[0] > threshold[0]) {
            Toast.makeText(getApplicationContext(),
                    "SAME PERSON with confidence: " + similarity[0] + "\n\n" + sessionManager.getLoc("full"),
                    Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(),
                    "DIFFERENT PERSON with confidence: " + similarity[0] + "\n\n" + sessionManager.getLoc("full"),
                    Toast.LENGTH_LONG).show();
        }

        // debug
        Log.d("FDA", "threshold: " + threshold[0] + " " + "similarity: " + similarity[0]);
    }

    /**
     * Request match faces
     */
    private void RecogFaces() {
        mApiService = ApiClient.getClient().create(BaseApiService.class);

        File file1 = new File("/storage/emulated/0/savedImages/face1.jpg");
        RequestBody requestFile1 = RequestBody.create(MediaType.parse("multipart/form-data"), file1);
        MultipartBody.Part body1 = MultipartBody.Part.createFormData("img_1", file1.getName(), requestFile1);

        File file2 = new File("/storage/emulated/0/savedImages/face2.jpg");
        RequestBody requestFile2 = RequestBody.create(MediaType.parse("multipart/form-data"), file2);
        MultipartBody.Part body2 = MultipartBody.Part.createFormData("img_2", file2.getName(), requestFile2);

        mApiService.matchFaces("WangunBearerToken 1772170802", body1, body2)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Log.d("RRR onResponse", "onResponse");
                        if (response.isSuccessful()) {
                            Log.d("RRR Response", "Response successful");
                            try {
                                JSONObject jsonRESULTS = new JSONObject(response.body().string());
                                Log.d("RRR Code", jsonRESULTS.getString("status"));
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("RRR Exception", e.toString());
                            }
                        } else {
                            Log.e("RRR Rns", "Response not successful");
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Log.e("RRR onFailure", t.toString());
                    }
                });
    }

    /**
     * Do face detect in thread
     */
    private class FaceDetectThread extends Thread {
        private Handler handler;
        private byte[] data = null;
        private Context ctx;
        private Bitmap faceCroped;

        public FaceDetectThread(Handler handler, Context ctx) {
            this.ctx = ctx;
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

                    /**
                     * Only detect face size > 20x20
                     */
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

                            if (rectCheck.contains(mid.x, mid.y) && (System.currentTimeMillis() - faces_previous[j].getTime()) < 1000) {
                                idFace = faces_previous[j].getId();
                                break;
                            }
                        }

                        if (idFace == Id) Id++;

                        faces[i].setFace(idFace, mid, eyesDis, confidence, pose, System.currentTimeMillis());

                        faces_previous[i].set(faces[i].getId(), faces[i].getMidEye(), faces[i].eyesDistance(), faces[i].getConfidence(), faces[i].getPose(), faces[i].getTime());

                        //
                        // if focus in a face 5 frame -> take picture face display in RecyclerView
                        // because of some first frame have low quality
                        //
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
                                            if (imagePreviewAdapter.getItemCount() == 0) {
                                                Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                                                        faceCroped, 512, 512, false);
                                                String from = getIntent().getStringExtra("from");
                                                BmpConverter.SaveImage(resizedBitmap, from);
                                                imagePreviewAdapter.add(resizedBitmap);

                                                if (from.equals("register")) {
                                                    Toast.makeText(getApplicationContext(), "Muka berhasil didaftarkan", Toast.LENGTH_LONG).show();
                                                    Intent myIntent = new Intent(FaceDetectActivity.this, MainActivity.class);
                                                    startActivity(myIntent);
                                                    finish();
                                                } else {
                                                    matchingFaces();
                                                }
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
