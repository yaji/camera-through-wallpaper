package com.yaji.viewfinder;

import java.io.File;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.ExifInterface;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.yaji.viewfinder.CameraHideMethods.CameraInfo;

public class CameraUtil {
    private static final String LOG_TAG = "yaji";
    private Context mContext;
    private SharedPreferences mPref;
    private int mCameraId;

    // Date format to be used for JPG file name.
    public static final String DATE_FORMAT = "yyyy-MM-dd_kk.mm.ss";

    // Directory in which pictures are saved.
    public static final String DIR_SAVE_PICS = Environment.getExternalStorageDirectory().toString() + "/CTW";

    /*
     * Constructor
     */
    public CameraUtil(Context context, int cameraId) {
        mContext = context;
        mPref = WalkAroundSettings.getPref(context);
        mCameraId = cameraId;
    }

    /*
     * Save picture size setting into the preference, to be used from WalkAroundSettings.
     */
    public void savePictureSizeSetting(Camera camera) {
        if (camera != null) {
            // Check if this setting is already saved. if so, then do nothing.
            // Otherwise, we start saving them into the preference.
            String sampleKey = WalkAroundSettings.createKey(mCameraId, WalkAroundSettings.KEY_PICTURE_SUPPORTED_SIZE, 0);
            String sample = mPref.getString(sampleKey, null);
            if (sample == null) {
                try {
                    Editor edit = mPref.edit();
                    // Save supported picture size.
                    Camera.Parameters params = camera.getParameters();
                    List<Size> supportedPictureSizes = params.getSupportedPictureSizes();

                    if (supportedPictureSizes != null) {
                        int numOfSizes = supportedPictureSizes.size();
                        for (int i = 0; i < numOfSizes; i++) {
                            Size size = supportedPictureSizes.get(i);
                            String key = WalkAroundSettings.createKey(mCameraId, WalkAroundSettings.KEY_PICTURE_SUPPORTED_SIZE, i);
                            String value = size.width + "x" + size.height;
                            edit.putString(key, value);
                        }

                        // Save picture size currently set in the camera as
                        // well.
                        String currentKey = WalkAroundSettings.createKey(mCameraId, mContext.getString(R.string.key_picture_size));
                        Size currentSize = params.getPictureSize();
                        String currentValue = currentSize.width + "x" + currentSize.height;
                        edit.putString(currentKey, currentValue);
                    }
                    // Commit
                    edit.commit();
                } catch (Exception e) {
                    Log.w(LOG_TAG, "savePictureSizeSetting() failed", e);
                }
            }
        }
    }

    /*
     * Save camera option setting into the preference, to be used from WalkAroundSettings.
     */
    public void saveCameraOptionSetting(Camera camera, String key) {
        if (camera != null) {
            // Check if picture size setting is already saved. if so, then do
            // nothing. Otherwise, we start saving them into the preference.
            String sampleKey = WalkAroundSettings.createKey(mCameraId, key, 0);
            String sample = mPref.getString(sampleKey, null);
            if (sample == null) {
                try {
                    Editor edit = mPref.edit();
                    // Save supported picture size.
                    Camera.Parameters params = camera.getParameters();
                    List<String> supportedValues = null;
                    int currentKeyId = 0;
                    String currentValue = null;
                    if (key.equals(WalkAroundSettings.KEY_SUPPORTED_WHITE_BALANCE)) {
                        supportedValues = params.getSupportedWhiteBalance();
                        currentKeyId = R.string.key_white_balance;
                        currentValue = params.getWhiteBalance();
                    } else if (key.equals(WalkAroundSettings.KEY_SUPPORTED_COLOR_EFFECT)) {
                        supportedValues = params.getSupportedColorEffects();
                        currentKeyId = R.string.key_color_effect;
                        currentValue = params.getColorEffect();
                    } else if (key.equals(WalkAroundSettings.KEY_SUPPORTED_FLASH_MODE)) {
                        supportedValues = params.getSupportedFlashModes();
                        currentKeyId = R.string.key_flash_mode;
                        currentValue = params.getFlashMode();
                    } else if (key.equals(WalkAroundSettings.KEY_SUPPORTED_FOCUS_MODE)) {
                        supportedValues = params.getSupportedFocusModes();
                        currentKeyId = R.string.key_focus_mode;
                        currentValue = params.getFocusMode();
                    }

                    if (supportedValues != null) {
                        int numOfSizes = supportedValues.size();
                        for (int i = 0; i < numOfSizes; i++) {
                            String value = supportedValues.get(i);
                            String generatedKey = WalkAroundSettings.createKey(mCameraId, key, i);
                            edit.putString(generatedKey, value);
                        }

                        // Save white balance currently set in the camera as
                        // well.
                        String currentGeneratedKey = WalkAroundSettings.createKey(mCameraId, mContext.getString(currentKeyId));
                        edit.putString(currentGeneratedKey, currentValue);
                    }
                    // Commit
                    edit.commit();

                } catch (Exception e) {
                    Log.w(LOG_TAG, "saveCameraOptionSetting() failed", e);
                }
            }
        }
    }

    /*
     * Save the number of cameras into the preference, to be used from WalkAroundSettings.
     */
    public void saveNumberOfCameraSetting(int numOfCameras) {
        // Check if this setting is already saved. if so, then do nothing.
        // Otherwise, we start saving them into the preference.
        String key = WalkAroundSettings.KEY_SUPPORTED_NUM_OF_CAMERAS;
        int sample = mPref.getInt(key, -1);
        if (sample == -1) {
            try {
                // Save the number of cameras.
                Editor edit = mPref.edit();
                edit.putInt(key, numOfCameras);

                // Save camera id currently set in the camera as well.
                final int defaultCameraId = CameraHideMethods.CameraInfo.CAMERA_FACING_BACK;
                edit.putString(mContext.getString(R.string.key_cameraid), defaultCameraId + "");

                // Commit
                edit.commit();

            } catch (Exception e) {
                Log.w(LOG_TAG, "saveNumberOfCameraSetting() failed", e);
            }
        }
    }

    /*
     * Save whether camera supports zoom, into the preference, to be used from WalkAroundSettings.
     */
    public void saveZoomSupported(Camera camera) {
        if (camera != null) {
            // Check if this setting is already saved. if so, then do nothing.
            // Otherwise, we start saving them into the preference.
            String key = WalkAroundSettings.KEY_SUPPORTED_ZOOM;
            String sample = mPref.getString(key, null);
            if (sample == null) {
                try {
                    // Save the number of cameras.
                    Editor edit = mPref.edit();

                    // Save whether this device supports zoom or not.
                    boolean isSupported = camera.getParameters().isZoomSupported();
                    if (isSupported) {
                        edit.putString(key, WalkAroundSettings.ZOOM_SUPPORTED);
                    } else {
                        edit.putString(key, WalkAroundSettings.ZOOM_NOT_SUPPORTED);
                    }

                    // Commit
                    edit.commit();

                } catch (Exception e) {
                    Log.w(LOG_TAG, "saveZoomSupported() failed", e);
                }
            }
        }
    }

    /*
     * Set preview size.
     */
    public void setPreviewSizeToCamParam(Camera.Parameters params) {
        String key = WalkAroundSettings.createKey(mCameraId, WalkAroundSettings.KEY_PREVIEW_SIZE);
        String savedPreviewSize = mPref.getString(key, null);

        if (savedPreviewSize != null) {
            int index = savedPreviewSize.indexOf('x');
            int width = Integer.parseInt(savedPreviewSize.substring(0, index));
            int height = Integer.parseInt(savedPreviewSize.substring(index + 1));

            params.setPreviewSize(width, height);
        } else {
            final Resources resources = mContext.getResources();
            final boolean portrait = resources.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
            final DisplayMetrics metrics = resources.getDisplayMetrics();
            final List<Camera.Size> sizes = params.getSupportedPreviewSizes();

            if (sizes != null) { // Some devices return null for sizes?
                                 // According to crash report on Android market.
                // Try to find a preview size that matches the screen first
                boolean found = false;
                int displayWidth = portrait ? metrics.heightPixels : metrics.widthPixels;
                int displayHeight = portrait ? metrics.widthPixels : metrics.heightPixels;
                for (Camera.Size size : sizes) {
                    if (size.width == displayWidth && size.height == displayHeight) {
                        params.setPreviewSize(size.width, size.height);
                        Log.d(LOG_TAG, "CameraUtil.setPreviewSizeToCamParam(), 1, size:" + size.width + "x" + size.height);
                        found = true;
                    }
                }

                // If no suitable preview size was found, try to find size from
                // near aspect ration point of view.
                if (!found) {
                    float ratio = (float) displayWidth / (float) displayHeight;
                    float diff = Float.MAX_VALUE;
                    int width = 0;
                    int height = 0;

                    // Find width and height from aspect ratio.
                    for (Camera.Size size : sizes) {
                        float r = (float) size.width / (float) size.height;
                        if (ratio - r < diff) {
                            diff = ratio - r;
                            width = size.width;
                            height = size.height;
                        }
                    }

                    params.setPreviewSize(width, height);
                    Log.d(LOG_TAG, "CameraUtil.setPreviewSizeToCamParam(), 2, size:" + width + "x" + height);
                }
            }

            // Save preview size into the preference.
            Editor edit = mPref.edit();
            Size size = params.getPreviewSize();
            String value = size.width + "x" + size.height;
            edit.putString(key, value);
            edit.commit();
        }
    }

    /*
     * Camera hardware orientation.
     */
    public int getCameraHWOrientation(int displayOrientation) {
        int degree = 0;

        if (displayOrientation == Surface.ROTATION_0) {
            degree = 0;
        } else if (displayOrientation == Surface.ROTATION_90) {
            degree = 90;
        } else if (displayOrientation == Surface.ROTATION_180) {
            degree = 180;
        } else if (displayOrientation == Surface.ROTATION_270) {
            degree = 270;
        }

        // CameraInfo.
        CameraHideMethods h = new CameraHideMethods();
        CameraInfo info = h.getCameraInfo(mCameraId);

        // Correct camera rotation set manually by end-user.
        int manualOffset = Integer.parseInt(mPref.getString(mContext.getString(R.string.key_correct_rotation), "0"));

        // Calculate orientation.
        int result;
        if (mCameraId == CameraHideMethods.CameraInfo.CAMERA_FACING_FRONT) {
            int cameraHWDefaultOrientation = (info != null) ? info.orientation : CameraInfo.DEFAULT_ORIENTATION_FRONT;
            result = (cameraHWDefaultOrientation + degree + manualOffset) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back_facing
            int cameraHWDefaultOrientation = (info != null) ? info.orientation : CameraInfo.DEFAULT_ORIENTATION_BACK;
            result = (cameraHWDefaultOrientation - degree + manualOffset + 360) % 360;
        }

        return result;
    }

    public int getEXIFRotation(int displayOrientation) {
        int degree = 0;

        if (displayOrientation == Surface.ROTATION_0) {
            degree = 0;
        } else if (displayOrientation == Surface.ROTATION_90) {
            degree = 90;
        } else if (displayOrientation == Surface.ROTATION_180) {
            degree = 180;
        } else if (displayOrientation == Surface.ROTATION_270) {
            degree = 270;
        }

        // CameraInfo.
        CameraHideMethods h = new CameraHideMethods();
        CameraInfo info = h.getCameraInfo(mCameraId);

        // Correct camera rotation set manually by end-user.
        int manualOffset = Integer.parseInt(mPref.getString(mContext.getString(R.string.key_correct_rotation), "0"));

        int result;
        if (mCameraId == CameraHideMethods.CameraInfo.CAMERA_FACING_FRONT) {
            int defaultOrientation = (info != null) ? info.orientation : CameraInfo.DEFAULT_ORIENTATION_FRONT;
            result = (defaultOrientation + degree + manualOffset + 360) % 360;
        } else { // back_facing
            int defaultOrientation = (info != null) ? info.orientation : CameraInfo.DEFAULT_ORIENTATION_BACK;
            result = (defaultOrientation - degree + manualOffset + 360) % 360;
        }

        return result;
    }

    /*
     * Orientation
     */
    public int displayRotationToExifOrientation(int displayRotation) {
        int degree = getEXIFRotation(displayRotation);
        int exifOrientation = 0;

        if (degree == 0) {
            exifOrientation = ExifInterface.ORIENTATION_NORMAL;
        } else if (degree == 90) {
            exifOrientation = ExifInterface.ORIENTATION_ROTATE_90;
        } else if (degree == 180) {
            exifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
        } else if (degree == 270) {
            exifOrientation = ExifInterface.ORIENTATION_ROTATE_270;
        }

        return exifOrientation;
    }

    /*
     * Get the current camera id.
     */
    public static int getCurrentCameraId(Context context) {
        int camera = CameraHideMethods.CameraInfo.CAMERA_FACING_BACK;

        SharedPreferences pref = WalkAroundSettings.getPref(context);
        String cameraId = pref.getString(context.getString(R.string.key_cameraid), "");
        if (cameraId.equals(CameraHideMethods.CameraInfo.CAMERA_FACING_FRONT + "")) {
            camera = CameraHideMethods.CameraInfo.CAMERA_FACING_FRONT;
        }

        return camera;
    }

    /*
     * File name to be saved for picture.
     */
    public static String createPictureFilePath() {
        String filename = DateFormat.format(DATE_FORMAT, System.currentTimeMillis()).toString();
        return CameraUtil.DIR_SAVE_PICS + File.separator + filename + ".JPG";
    }

    /*
     * File name to be saved for movie.
     */
    public static String createMovieFilePath() {
        String filename = DateFormat.format(DATE_FORMAT, System.currentTimeMillis()).toString();
        return CameraUtil.DIR_SAVE_PICS + File.separator + filename + ".mp4";
    }
}
