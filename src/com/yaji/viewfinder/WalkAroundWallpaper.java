package com.yaji.viewfinder;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.Toast;

public class WalkAroundWallpaper extends WallpaperService {
    private static final String LOG_TAG = "yaji";

    private Camera mCamera;
    private WalkAroundEngine mOwner;
    private CameraHideMethods mCameraHideMethods;
    private SharedPreferences mPref;
    private CameraUtil mCameraUtil;

    public Engine onCreateEngine() {
        Log.d(LOG_TAG, "[in]onCreateEngine()");
        return mOwner = new WalkAroundEngine();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "[in]onDestroy()");
        super.onDestroy();
        stopCamera();
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "[in]onCreate()");
        super.onCreate();
        mCameraHideMethods = new CameraHideMethods();
        mPref = WalkAroundSettings.getPref(getApplicationContext());
        // CameraUtil
        mCameraUtil = new CameraUtil(getApplicationContext(), CameraUtil.getCurrentCameraId(getApplicationContext()));
    }

    /*
     * Start camera.
     */
    private boolean startCamera() {
        boolean retVal = true;
        Log.d(LOG_TAG, "[in]startCamera()");

        try {
            // Create camera object.
            if (mCamera == null) {
                if (mCameraHideMethods.hasCameraOpenLevel9()) {
                    // More than Android9 (GB)
                    int cameraId = CameraUtil.getCurrentCameraId(getApplicationContext());
                    mCamera = mCameraHideMethods.open(cameraId);
                } else {
                    // Less than Android 8 (Froyo)
                    mCamera = Camera.open();
                }
            } else {
                mCamera.reconnect();
            }
        } catch (Exception e) {
            if (mCamera != null) {
                mCamera.release();
            }
            mCamera = null;
            retVal = false;

            Log.e(LOG_TAG, "startCamera(), Error opening the camera", e);
        }

        Log.d(LOG_TAG, "  [out]startCamera(), success:" + retVal);
        return retVal;
    }

    /*
     * Start preview.
     */
    private boolean startPreview() {
        boolean retVal = false;
        Log.d(LOG_TAG, "[in]startPreview()");

        if (mCamera != null) {
            // Start preview.
            try {
                mCamera.startPreview();
                retVal = true;
            } catch (Exception e) {
                Log.e(LOG_TAG, "startPreview() failed.", e);
                // Stop myself.
                stopSelf();
            }
        } else {
            Log.w(LOG_TAG, "WalkAroundEngine::startPreview(), Error: mCamera == null");
            // Stop myself.
            stopSelf();
        }

        Log.d(LOG_TAG, "  [out]startPreview(), success:" + retVal);
        return retVal;
    }

    /*
     * Set camera parameter like orientation.
     */
    private void setCameraParameter() {
        Log.d(LOG_TAG, "[in] setCameraParameter()");
        // Set camera orientation.
        if (!mCameraHideMethods.previewEnabled(mCamera)) {
            final Camera.Parameters params = mCamera.getParameters();

            // Save parameters according to the camera id.
            int cameraId = CameraUtil.getCurrentCameraId(getApplicationContext());
            Log.d(LOG_TAG, "setCameraParameter(), cameraId: " + cameraId);

            // Set preview size.
            mCameraUtil.setPreviewSizeToCamParam(params);

            // Picture size
            String strSize = mPref.getString(WalkAroundSettings.createKey(cameraId, getString(R.string.key_picture_size)), null);
            if (strSize != null) {
                int index = strSize.indexOf('x');
                int width = Integer.parseInt(strSize.substring(0, index));
                int height = Integer.parseInt(strSize.substring(index + 1));
                Size currentSize = params.getPictureSize();
                if (currentSize != null) {
                    if (currentSize.width != width || currentSize.height != height) {
                        params.setPictureSize(width, height);
                        Log.d(LOG_TAG, "picture width:" + params.getPictureSize().width + ", picture height:" + params.getPictureSize().height);
                    }
                }
            }

            // White balance
            String strWB = mPref.getString(WalkAroundSettings.createKey(cameraId, getString(R.string.key_white_balance)), null);
            if (strWB != null) {
                String current = params.getWhiteBalance();
                if (current != null && !current.equals(strWB)) {
                    params.setWhiteBalance(strWB);
                }
            }
            // Flash mode
            String strFlashMode = mPref.getString(WalkAroundSettings.createKey(cameraId, getString(R.string.key_flash_mode)), null);
            if (strFlashMode != null) {
                String current = params.getFlashMode();
                if (current != null && !current.equals(strFlashMode)) {
                    params.setFlashMode(strFlashMode);
                }
            }
            // Focus mode
            String strFocusMode = mPref.getString(WalkAroundSettings.createKey(cameraId, getString(R.string.key_focus_mode)), null);
            if (strFocusMode != null) {
                String current = params.getFocusMode();
                if (current != null && !current.equals(strFocusMode)) {
                    params.setFocusMode(strFocusMode);
                }
            }
            // Color effect
            String strColorEffect = mPref.getString(WalkAroundSettings.createKey(cameraId, getString(R.string.key_color_effect)), null);
            if (strColorEffect != null) {
                String current = params.getColorEffect();
                if (current != null && !current.equals(strColorEffect)) {
                    params.setColorEffect(strColorEffect);
                }
            }

            // Orientation
            int rotation = getDisplayRotation();
            int exifDegree = mCameraUtil.getEXIFRotation(rotation);
            int degree = mCameraUtil.getCameraHWOrientation(rotation);
            params.setRotation(exifDegree);

            // Set params.
            Log.d(LOG_TAG, "mCamera.setParameters(), preview width:" + params.getPreviewSize().width + ", preview height:"
                    + params.getPreviewSize().height);
            Log.d(LOG_TAG, "mCamera.setDisplayOrientation():" + degree);
            mCamera.setParameters(params);
            // setDisplayOrientation is not allowed to be called during preview.
            mCamera.setDisplayOrientation(degree);
        } else {
            Log.d(LOG_TAG, "setCameraParameter(), previewEnabled == true");
        }
        Log.d(LOG_TAG, "  [out] setCameraParameter()");
    }

    /*
     * Get rotation.
     */
    private int getDisplayRotation() {
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        return display.getRotation();
    }

    /*
     * Stop camera.
     */
    private void stopCamera() {
        Log.d(LOG_TAG, "[in]stopCamera()");
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.release();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error in stopCamera()", e);
            }

            mCamera = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (mCamera != null) {
            if (mCameraHideMethods.previewEnabled(mCamera)) {
                mCamera.stopPreview();
            }
            // Set parameter.
            // Orientation
            startCamera();
            setCameraParameter();
            startPreview();
        }
    }

    /*
     * Engine.
     */
    class WalkAroundEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {
        // Gesture detector stuff
        private GestureDetector mGestureDetector;
        private ScaleGestureDetector mScaleGestureDetector;
        private boolean mBootupErrorMode = false;
        private boolean mDoubleTapEnabled = false;
        private boolean mQRRecogEnabled = false;
        private int mDisplayCenterX;
        private int mDisplayCenterY;

        @SuppressWarnings("deprecation")
        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            Log.d(LOG_TAG, "[in]WalkAroundEngine::onCreate(), isPreview():" + isPreview());
            super.onCreate(surfaceHolder);

            // Check if we launched just after the phone bootup.
            mBootupErrorMode = isBootupErrorMode();

            // Surface type set to PUSH_BUFFERS for camera.
            // Some devices in Android 2.3 does not show this wallpaper in livewallpaper preview screen.
            if (!mBootupErrorMode && !isLiveWallpaperPreviewMode()) {
                surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            }

            // Preference callback.
            mPref.registerOnSharedPreferenceChangeListener(this);

            // Load up user's settings
            setTouchEventsEnabled(true); // Always set to true.
            mDoubleTapEnabled = mPref.getBoolean(getString(R.string.key_take_picture), getResources().getBoolean(R.bool.default_camera_take_picture));

            // Display width/height used to judge if we try to start recognition for QR code.
            DisplayMetrics metrics = new DisplayMetrics();
            ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getMetrics(metrics);
            mDisplayCenterX = metrics.widthPixels / 2;
            mDisplayCenterY = metrics.heightPixels / 2;

            // Gesture detector to handle touch events.
            mGestureDetector = new GestureDetector(getApplicationContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    if (mDoubleTapEnabled) {
                        handleDoubleTap();
                    }
                    return true;
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    handleSingleTapConfirmed(e);
                    return true;
                }
            });

            // Gesture detector for multi touch
            mScaleGestureDetector = new ScaleGestureDetector(getApplicationContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    handleMultiTouch(detector);
                    return true;
                }
            });

            Log.d(LOG_TAG, "  [out]WalkAroundEngine::onCreate()");
        }

        @Override
        public void onDestroy() {
            Log.d(LOG_TAG, "[in]WalkAroundEngine::onDestroy()");
            mPref.unregisterOnSharedPreferenceChangeListener(this);
        }

        /*
         * onCommand
         */
        @Override
        public Bundle onCommand(String action, int x, int y, int z, Bundle extras, boolean resultRequested) {
            // Called when application tray of HOME hides our wallpaper.
            return super.onCommand(action, x, y, z, extras, resultRequested);
        }

        /*
         * onVisibilityChanged
         */
        @Override
        public void onVisibilityChanged(boolean visible) {
            Log.d(LOG_TAG, "[in]WalkAroundEngine::onVisibilityChanged(), visible:" + visible);
            if (!visible) {
                Log.d(LOG_TAG, "[in]WalkAroundEngine::onVisibilityChanged() mOwner == this : " + (mOwner == this));
                if (mOwner == this) {
                    stopCamera();
                }
            } else {
                if (mBootupErrorMode) {
                    // for end-user to launch LiveWallpaper picker by double-tap.
                    drawIntroductionScreen(getString(R.string.error_bootup));
                } else if (isLiveWallpaperPreviewMode()) {
                    drawIntroductionScreen(getString(R.string.error_preview_mode));
                } else {
                    // Start camera preview.
                    try {
                        startCamera();
                        if (mCamera != null) {
                            // Set parameter.
                            setCameraParameter();
                            mCamera.setPreviewDisplay(getSurfaceHolder());
                            startPreview();
                        }
                    } catch (IOException e) {
                        mCamera.release();
                        mCamera = null;
                        Log.e(LOG_TAG, "onVisibilityChanged(), Error opening the camera", e);
                    }
                }
            }
            Log.d(LOG_TAG, "  [out]WalkAroundEngine::onVisibilityChanged(), visible:" + visible);
        }

        /*
         * onSurfaceChanged
         */
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(LOG_TAG, "[in]WalkAroundEngine::onSurfaceChanged(), isVisible():" + isVisible());
            super.onSurfaceChanged(holder, format, width, height);

            if (holder.isCreating()) {
                Log.d(LOG_TAG, "WalkAroundEngine::onSurfaceChanged(), holder.isCreating()");
                try {
                    if (mCamera != null) {
                        if (mCameraHideMethods.previewEnabled(mCamera)) {
                            mCamera.stopPreview();
                        }
                        // Set parameter.
                        startCamera();
                        if (mCamera != null) {
                            setCameraParameter();
                            mCamera.setPreviewDisplay(holder);
                        }
                    }
                } catch (IOException e) {
                    mCamera.release();
                    mCamera = null;
                    Log.e(LOG_TAG, "onSurfaceChanged(), Error opening the camera", e);
                }
            }

            if (isVisible()) {
                startPreview();
            }
            Log.d(LOG_TAG, "  [out]WalkAroundEngine::onSurfaceChanged()");
        }

        /*
         * onSurfaceCreated
         */
        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            Log.d(LOG_TAG, "[in]WalkAroundEngine::onSurfaceCreated(), holder:" + holder.toString());
            super.onSurfaceCreated(holder);
        }

        /*
         * Check if we draw camera preview.
         */
        private boolean isLiveWallpaperPreviewMode() {
            boolean retVal = false;
            // Just for gingerbread and further, we will provide special preview mode
            // because it is impossible to draw camera preview when LiveWallpaper preview in more than gingerbread. (not sure why)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                if (isPreview()) {
                    retVal = true;
                }
            }
            return retVal;
        }

        /*
         * Check if we draw camera preview.
         */
        private boolean isBootupErrorMode() {
            boolean retVal = false;

            // Just for gingerbread and further, we will provide special preview mode
            // because it is impossible to draw camera preview when bootup in more than gingerbread. (not sure why)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO) {
                long bootTime = SystemClock.elapsedRealtime();
                final long POSSIBLY_BOOT_COMPLETE_TIME = 3 * 60 * 1000; // 3min
                if (bootTime < POSSIBLY_BOOT_COMPLETE_TIME && !BootCompletedReceiver.isReceived()) {
                    retVal = true;
                }
            }
            return retVal;
        }

        /*
         * Draw introduction screen instead of camera preview.
         */
        private void drawIntroductionScreen(String msg) {
            final SurfaceHolder holder = getSurfaceHolder();
            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    c.drawColor(Color.WHITE);
                    Paint paint = new Paint();
                    paint.setColor(Color.DKGRAY);
                    paint.setTextSize(16);
                    paint.setAntiAlias(true);
                    c.drawText(msg, 40, 280, paint);
                }
            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }
        }

        /*
         * OnTouchEvent.
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            Log.d(LOG_TAG, "onTouchEvent(), " + event.toString());
            if (mScaleGestureDetector != null) {
                mScaleGestureDetector.onTouchEvent(event);
                if (!mScaleGestureDetector.isInProgress()) {
                    // Just pass all touch event to gesture detector for simple implementation.
                    mGestureDetector.onTouchEvent(event);
                }
            }
        }

        /*
         * Handle double tap to take a picture.
         */
        private void handleDoubleTap() {
            if (!isPreview()) {
                if (mBootupErrorMode) {
                    launchLiveWallpaperPickerAndExit();
                } else {
                    // Double tap to take a picture (with/without shutter sound)
                    // Check if SD card is mounted or not.
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        boolean isShutterSoundEnabled = mPref.getBoolean(getString(R.string.key_shutter_sound),
                                getResources().getBoolean(R.bool.default_camera_shutter_sound));

                        if (isShutterSoundEnabled) {
                            // Take a picture.
                            takePicture();
                        } else {
                            // Take a picture from camera preview (low resolution) without shutter sound.
                            takePictureWithoutShutterSound();
                        }
                    }
                }
            }
        }

        /*
         * Take a picture. To be called when double-tapped.
         */
        private void takePicture() {
            if (mCamera != null) {
                try {
                    mCamera.takePicture(null, null, new PictureCallback() {
                        public void onPictureTaken(byte[] rawData, Camera camera) {
                            // Save picture as a JPG file.
                            try {
                                if (rawData != null) {
                                    // Directory.
                                    File dirFile = new File(CameraUtil.DIR_SAVE_PICS);
                                    if (!dirFile.exists()) {
                                        dirFile.mkdirs();
                                    }

                                    // Date to be used for JPG filename.
                                    String filepath = CameraUtil.createPictureFilePath();
                                    FileOutputStream outstream = new FileOutputStream(filepath);
                                    BufferedOutputStream buffer = new BufferedOutputStream(outstream);
                                    buffer.write(rawData);
                                    buffer.close();

                                    // Request MediaScanner to scan the file to be saved right now, and to add it to the media DB.
                                    ImageUtil.scanFile(getApplicationContext(), filepath);

                                    // Preview will be stopped after the image is taken. Therefore restart preview.
                                    startPreview();
                                }
                            } catch (Exception e) {
                                Log.w(LOG_TAG, "onPictureTaken() failed", e);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.w(LOG_TAG, "Camera:takePicture failed", e);
                    stopCamera();
                }
            }
        }

        /*
         * Take a picture. To be called when double-tapped. Without shutter sound. Just capture camera preview.
         */
        private void takePictureWithoutShutterSound() {
            if (mCamera != null) {
                mCamera.setOneShotPreviewCallback(new PreviewCallback() {
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        // Stop preview so that end-user can notice that we are taking a picture.
                        mCamera.stopPreview();

                        // Allocate bitmap data.
                        Size previewSize = mCamera.getParameters().getPreviewSize();
                        final int width = previewSize.width;
                        final int height = previewSize.height;
                        int[] rgb = new int[(width * height)];

                        // Convert from data(YUV420) to rgb(RGB).
                        ImageUtil.decodeYUV420SP(rgb, data, width, height);
                        // Create bitmap to be converted to JPG.
                        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        bmp.setPixels(rgb, 0, width, 0, 0, width, height);

                        // Directory.
                        File dirFile = new File(CameraUtil.DIR_SAVE_PICS);
                        if (!dirFile.exists()) {
                            dirFile.mkdirs();
                        }

                        // Date to be used for JPG filename.
                        String filepath = CameraUtil.createPictureFilePath();
                        ImageUtil.saveBitmapAsFile(filepath, bmp, Bitmap.CompressFormat.JPEG);

                        // Add EXIF data.
                        try {
                            ExifInterface ei = new ExifInterface(filepath);
                            ei.setAttribute(ExifInterface.TAG_ORIENTATION, mCameraUtil.displayRotationToExifOrientation(getDisplayRotation()) + "");
                            ei.saveAttributes();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // Request MediaScanner to scan the file to be saved right now, and to add it to the media DB.
                        ImageUtil.scanFile(getApplicationContext(), filepath);

                        // Inform end-user that picture has been saved.
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_picture_saved), Toast.LENGTH_SHORT).show();

                        // Restart preview.
                        startPreview();
                    }
                });
            } else {
                Log.w(LOG_TAG, "takePictureWithoutShutterSound(), mCamera: null");
            }
        }

        /*
         * Handle single tap to recognize QR code and launch the related application with recognized information.
         */
        private void handleSingleTapConfirmed(MotionEvent e) {
            if (mQRRecogEnabled) {
                if (!isPreview()) {
                    final int T = 100;
                    int x = (int) e.getRawX();
                    int y = (int) e.getRawY();
                    // Do only when tapping the center area of the display.
                    if (mDisplayCenterX < x + T && x < mDisplayCenterX + T && mDisplayCenterY < y + T && y < mDisplayCenterY + T) {
                        recognizeQRCode();
                    }
                }
            }
        }

        /*
         * Start recognition for QR code.
         */
        private void recognizeQRCode() {
            if (mCamera != null) {
                mCamera.setOneShotPreviewCallback(new PreviewCallback() {
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        // mCamera should not be null, but it seems that it is null according to crash report.
                        if (mCamera != null) {
                            Log.d(LOG_TAG, "QR start");
                            // Stop preview so that end-user can notice that we are taking a picture.
                            mCamera.stopPreview();

                            // Allocate bitmap data.
                            Size previewSize = mCamera.getParameters().getPreviewSize();
                            int width = previewSize.width;
                            int height = previewSize.height;
                            int[] rgb = new int[(width * height)];

                            // Convert from data(YUV420) to rgb(RGB).
                            ImageUtil.decodeYUV420SP(rgb, data, width, height);

                            // Create bitmap to be converted to JPG.
                            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                            bmp.setPixels(rgb, 0, width, 0, 0, width, height);

                            // Start recognition.
                            QRRecognizer recog = new QRRecognizer();
                            boolean isFound = recog.recognize(bmp);

                            // Launch app according to the recogniton result.
                            if (isFound) {
                                // Show the recognized information to end-user.
                                String text = recog.getText();
                                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();

                                // Start application.
                                Intent intent = recog.getLaunchIntent();
                                if (intent != null) {
                                    startActivity(intent);
                                }
                            }

                            // Restart preview.
                            startPreview();

                            // For performance measurement.
                            Log.d(LOG_TAG, "QR finished");
                        } else {
                            Log.w(LOG_TAG, "recognizeQRCode(), mCamera: null, (2)");
                        }
                    }
                });
            } else {
                Log.w(LOG_TAG, "recognizeQRCode(), mCamera: null, (1)");
            }
        }

        /*
         * Handle multi touch.
         */
        private void handleMultiTouch(ScaleGestureDetector detector) {
            if (mCamera != null) {
                Camera.Parameters param = mCamera.getParameters();
                if (param != null) {
                    if (param.isZoomSupported()) {
                        int index = param.getZoom();

                        float s = detector.getScaleFactor();
                        if (s > 1.0) {
                            // Zoom in
                            if (index + 1 < param.getMaxZoom()) {
                                param.setZoom(++index);
                                mCamera.setParameters(param);
                            }
                        } else if (s < 1.0) {
                            // Zoom out
                            if (index > 0) {
                                param.setZoom(--index);
                                mCamera.setParameters(param);
                            }
                        }
                    }
                }
            }
        }

        /*
         * Called when shared preference is updated.
         */
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key != null && key.equals(getString(R.string.key_take_picture))) {
                mDoubleTapEnabled = sharedPreferences.getBoolean(getString(R.string.key_take_picture),
                        getResources().getBoolean(R.bool.default_camera_shutter_sound));
                mQRRecogEnabled = sharedPreferences.getBoolean(getString(R.string.key_qr), getResources().getBoolean(R.bool.default_camera_qr));
            }
        }

        /*
         * Launch LiveWallpaer picker.
         */
        private void launchLiveWallpaperPickerAndExit() {
            Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            System.exit(0);
        }
    }
}
