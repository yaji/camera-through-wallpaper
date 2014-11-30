package com.yaji.viewfinder;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

/*
 * Settings.
 */
public class WalkAroundSettings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String LOG_TAG = "yaji";
    // Preference
    public static final String SHARED_PREFS_NAME = "key_walkaround_settings";

    public static final String KEY_PICTURE_SUPPORTED_SIZE = "picture supported size";
    public static final String KEY_PREVIEW_SIZE = "preview size";
    public static final String KEY_SUPPORTED_WHITE_BALANCE = "supported white balance";
    public static final String KEY_SUPPORTED_FLASH_MODE = "supported flash mode";
    public static final String KEY_SUPPORTED_FOCUS_MODE = "supported focus mode";
    public static final String KEY_SUPPORTED_COLOR_EFFECT = "supported color effect";
    public static final String KEY_SUPPORTED_NUM_OF_CAMERAS = "supported num of camera";
    public static final String KEY_SUPPORTED_ZOOM = "supported zoom";
    public static final String ZOOM_SUPPORTED = "zoom supported yes";
    public static final String ZOOM_NOT_SUPPORTED = "zoom supported no";
    public static final String KEY_BOOT_TIME = "key boot time";

    private int mCameraId = CameraHideMethods.CameraInfo.CAMERA_FACING_BACK;
    private SharedPreferences mPref;

    /*
     * onCreate.
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // SharedPreference setting.
        addPreferencesFromResource(R.xml.walkaround_prefs);

        // Pref
        getPreferenceManager().setSharedPreferencesName(SHARED_PREFS_NAME);
        mPref = getPref(getApplicationContext());

        // Camera settings.
        mCameraId = CameraUtil.getCurrentCameraId(getApplicationContext());
        setCameraParameterOptions(mCameraId);

        // Set setting for camera switching.
        setSwitchCameraSetting();

        // Link to LiveWallpaperPicker.
        PreferenceScreen psSetLiveWallpaper = (PreferenceScreen) findPref(R.string.key_setlivewallpaper);
        psSetLiveWallpaper.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                if (isChangeLiveWallpaperSupported()) {
                    // Launch settings for selecting our livewallpaper directly.
                    launchLiveWallpaperChanger();
                } else {
                    // Inform end-user to select our wallpaper.
                    Toast.makeText(getApplicationContext(), R.string.toast_setlivewallpaper, Toast.LENGTH_LONG).show();

                    // Launch settings menu for selecting LiveWallpaper.
                    launchLiveWallpaperPicker();
                }
                return true;
            }
        });

        // Link to LiveWallpaper picker.
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_MAIN)) {
            // Do nothing.
        } else {
            // If we come from LiveWallpaper preview screen, then we will not
            // show a link to LiveWallpaperPicker.
            PreferenceScreen screen = getPreferenceScreen();
            Preference category = (Preference) findPref(R.string.key_title_help);
            screen.removePreference(category);
        }
    }

    /*
     * onDestroy.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /*
     * onResume
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Set listener for preference change.
        mPref.registerOnSharedPreferenceChangeListener(this);
    }

    /*
     * onPause
     */
    @Override
    protected void onPause() {
        super.onPause();
        // Unregister
        mPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    /*
     * Util method to get SharedPreference.
     */
    public static SharedPreferences getPref(Context context) {
        return context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
    }

    /*
     * Start camera.
     */
    private void saveCameraParameter(int cameraId) {
        Log.d(LOG_TAG, "  [in]saveCameraParameter()");
        boolean retVal = true;
        Camera camera = null;
        CameraHideMethods methods = new CameraHideMethods();

        try {
            // Create camera object.
            if (methods.hasCameraOpenLevel9()) {
                // More than Android9 (GB)
                camera = methods.open(cameraId);
            } else {
                // Less than Android 8 (Froyo)
                camera = Camera.open();
            }
        } catch (Exception e) {
            if (camera != null) {
                camera.release();
            }
            camera = null;
            retVal = false;
            Log.e(LOG_TAG, "saveCameraParameter(), Error opening the camera", e);
        }

        if (camera != null) {
            // CameraUtil
            CameraUtil util = new CameraUtil(getApplicationContext(), CameraUtil.getCurrentCameraId(getApplicationContext()));
            // Save supported picture size in the preference. Performed once for
            // the initial use.
            util.savePictureSizeSetting(camera);
            // Save preview size.
            util.setPreviewSizeToCamParam(camera.getParameters());
            // Save supported white balance list in the preference. Performed
            // once for the initial use.
            util.saveCameraOptionSetting(camera, WalkAroundSettings.KEY_SUPPORTED_WHITE_BALANCE);
            // Save supported color effect list in the preference. Performed
            // once for the initial use.
            util.saveCameraOptionSetting(camera, WalkAroundSettings.KEY_SUPPORTED_COLOR_EFFECT);
            // Save supported flash mode list in the preference. Performed once
            // for the initial use.
            util.saveCameraOptionSetting(camera, WalkAroundSettings.KEY_SUPPORTED_FLASH_MODE);
            // Save supported focus mode list in the preference. Performed once
            // for the initial use.
            util.saveCameraOptionSetting(camera, WalkAroundSettings.KEY_SUPPORTED_FOCUS_MODE);
            // Save the number of cameras in the preference. Performed once for
            // the initial use.
            util.saveNumberOfCameraSetting(methods.getNumberOfCameras());
            // Save whether camera supports zoom.
            util.saveZoomSupported(camera);

            // Finally, we no longer use camera, so release it.
            camera.release();
            camera = null;
        }

        Log.d(LOG_TAG, "  [out]saveCameraParameter(), success:" + retVal);
    }

    /*
     * Camera parameter settings.
     */
    private void setCameraParameterOptions(int cameraId) {
        // Save camera parameter.
        saveCameraParameter(cameraId);

        // Set setting for camera picture size.
        setPictureSizeSetting(cameraId);
        // (1) white balance, (2) flash mode ,(3) focus, (4) color effect.
        setCameraOptionSetting(cameraId, getString(R.string.key_white_balance), KEY_SUPPORTED_WHITE_BALANCE);
        setCameraOptionSetting(cameraId, getString(R.string.key_flash_mode), KEY_SUPPORTED_FLASH_MODE);
        setCameraOptionSetting(cameraId, getString(R.string.key_focus_mode), KEY_SUPPORTED_FOCUS_MODE);
        setCameraOptionSetting(cameraId, getString(R.string.key_color_effect), KEY_SUPPORTED_COLOR_EFFECT);
        // Check if we can change zoom. If not, then set it to invisible.
        handleZoomMenu();
    }

    /*
     * Set setting for camera picture size.
     */
    private void setPictureSizeSetting(int cameraId) {
        String key = getString(R.string.key_picture_size);
        ListPreference lp = (ListPreference) findPref(key);

        boolean isSoundOn = mPref.getBoolean(getString(R.string.key_shutter_sound), getResources().getBoolean(R.bool.default_camera_shutter_sound));

        if (isSoundOn) {
            // Ask for the supported picture size from the preference, and
            // show them to end-user to select.
            List<String> supportedPictureSizeList = new ArrayList<String>();

            // Iterate preference and find all values related to picture size.
            for (int i = 0;; i++) {
                String pictureSizeKey = createKey(cameraId, KEY_PICTURE_SUPPORTED_SIZE, i);
                String value = mPref.getString(pictureSizeKey, null);
                if (value != null) {
                    supportedPictureSizeList.add(value);
                } else {
                    // No more picture size saved. Break.
                    break;
                }
            }

            // Set list items based on the preference.
            int numOfSizes = supportedPictureSizeList.size();
            String[] entries = new String[numOfSizes];
            String[] entryValues = new String[numOfSizes];
            String currentSize = mPref.getString(createKey(cameraId, key), "");
            Editor edit = mPref.edit();
            edit.putString(key, currentSize);
            edit.commit();
            int currentIndex = -1;
            String summary = "";
            for (int i = 0; i < numOfSizes; i++) {
                String size = supportedPictureSizeList.get(i);
                entries[i] = size;
                entryValues[i] = size;
                // Find current setting for the picture size.
                if (size.equals(currentSize)) {
                    currentIndex = i;
                    summary = size;
                }
            }
            lp.setEntries(entries);
            lp.setEntryValues(entryValues);
            if (currentIndex >= 0 && currentIndex < numOfSizes) {
                lp.setValueIndex(currentIndex);
                lp.setSummary(summary);
                lp.setEnabled(true);
            } else {
                lp.setEnabled(false);
                lp.setSummary(getString(R.string.not_supported));
            }
        } else { // isSoundOn == false
            String previewKey = createKey(mCameraId, KEY_PREVIEW_SIZE);
            String previewValue = mPref.getString(previewKey, "");
            lp.setEnabled(false); // Only one picture size, so disable this
                                  // menu.
            lp.setSummary(previewValue);
        }
    }

    /*
     * Set setting for (1) white balance, (2) flash mode (3) focus mode, (4) color effect.
     */
    private void setCameraOptionSetting(int cameraId, String key, String postfix) {
        ListPreference lp = (ListPreference) findPref(key);

        // Ask for the supported items from the preference, and show them to
        // end-user to select.
        List<String> list = new ArrayList<String>();

        // Iterate preference and find all values related to picture size.
        for (int i = 0;; i++) {
            String prefKey = createKey(cameraId, postfix, i);
            String value = mPref.getString(prefKey, null);
            if (value != null) {
                list.add(value);
            } else {
                // No more picture size saved. Break.
                break;
            }
        }

        // Set list items based on the preference.
        int numOfSizes = list.size();
        String[] entries = new String[numOfSizes];
        String[] entryValues = new String[numOfSizes];
        String currentValue = mPref.getString(createKey(cameraId, key), "");
        Editor edit = mPref.edit();
        edit.putString(key, currentValue);
        edit.commit();
        int currentIndex = -1;
        String summary = "";
        for (int i = 0; i < numOfSizes; i++) {
            String value = list.get(i);
            entries[i] = value;
            entryValues[i] = value;
            // Find current setting for the white balance.
            if (value.equals(currentValue)) {
                currentIndex = i;
                summary = value;
            }
        }
        lp.setEntries(entries);
        lp.setEntryValues(entryValues);
        if (currentIndex >= 0 && currentIndex < numOfSizes) {
            lp.setValueIndex(currentIndex);
            lp.setSummary(summary);
            lp.setEnabled(true);
        } else {
            lp.setEnabled(false);
            lp.setSummary(getString(R.string.not_supported));
        }
    }

    /*
     * Set setting for camera switching. Returns new camera id.
     */
    private int setSwitchCameraSetting() {
        String key = getString(R.string.key_cameraid);
        ListPreference lp = (ListPreference) findPref(key);

        // Get the number of camera that already saved in the preference.
        int numOfCameras = mPref.getInt(KEY_SUPPORTED_NUM_OF_CAMERAS, 1);
        int currentCameraId = Integer.parseInt(mPref.getString(key, CameraHideMethods.CameraInfo.CAMERA_FACING_BACK + ""));

        // Set list items based on the preference.
        String[] entries = new String[numOfCameras];
        String[] entryValues = new String[numOfCameras];
        String summary = "";
        for (int i = 0; i < numOfCameras; i++) {
            String value;
            if (i == CameraHideMethods.CameraInfo.CAMERA_FACING_BACK) {
                value = getString(R.string.camera_facing_back);
            } else if (i == CameraHideMethods.CameraInfo.CAMERA_FACING_FRONT) {
                value = getString(R.string.camera_facing_front);
            } else {
                value = "camera" + i;
            }
            entries[i] = value;
            entryValues[i] = i + "";
            // Find current setting.
            if (i == currentCameraId) {
                summary = value;
            }
        }
        lp.setEntries(entries);
        lp.setEntryValues(entryValues);
        lp.setValueIndex(currentCameraId);
        lp.setSummary(summary);

        // Gray out this setting because this phone does not have more than one
        // camera.
        if (numOfCameras <= 1) {
            lp.setEnabled(false);
        }

        return currentCameraId;
    }

    /*
     * Zoom
     */
    private void handleZoomMenu() {
        // If this device does not support zoom, then disable zoom menu with
        // "not supported" message.
        CheckBoxPreference cp = (CheckBoxPreference) findPref(R.string.key_zoom);
        String zoomSupported = mPref.getString(KEY_SUPPORTED_ZOOM, ZOOM_NOT_SUPPORTED);

        if (zoomSupported == ZOOM_NOT_SUPPORTED) {
            cp.setEnabled(false);
            cp.setChecked(false);
            cp.setSummaryOff(R.string.summary_zoom_not_supported);
        }
    }

    /*
     * onSharedPreferenceChanged.
     */
    public void onSharedPreferenceChanged(SharedPreferences pref, String key) {
        // Setting to check if we will take a picture or not.
        if (key.equals(getString(R.string.key_take_picture))) {
            boolean takePicture = pref.getBoolean(getString(R.string.key_take_picture), true);
            if (takePicture) {
                // Inform end-user of where picture will be saved into.
                Toast.makeText(this, getString(R.string.picture_save_to), Toast.LENGTH_LONG).show();
            }
        }
        // Camera picture size, White balance, Flash mode, Focus mode, Color
        // effect.
        else if (key.equals(getString(R.string.key_picture_size)) || key.equals(getString(R.string.key_white_balance))
                || key.equals(getString(R.string.key_flash_mode)) || key.equals(getString(R.string.key_focus_mode))
                || key.equals(getString(R.string.key_color_effect))) {
            Editor edit = pref.edit();
            edit.putString(createKey(mCameraId, key), pref.getString(key, null));
            edit.commit();
            setStringSummary(pref, key);
        } else if (key.equals(getString(R.string.key_cameraid))) {
            setStringSummaryForCameraSwitch(pref);
            mCameraId = Integer.parseInt(pref.getString(key, CameraHideMethods.CameraInfo.CAMERA_FACING_BACK + ""));
            setCameraParameterOptions(mCameraId);
        } else if (key.equals(getString(R.string.key_shutter_sound))) {
            // Disable picture size setting when shutter sound is off.
            setPictureSizeSetting(mCameraId);
        }
    }

    /*
     * Set string summary.
     */
    private void setStringSummary(SharedPreferences pref, String key) {
        String summary = pref.getString(key, "");
        ListPreference lp = (ListPreference) findPref(key);
        lp.setSummary(summary);
    }

    /*
     * Set string summary for camera switch.
     */
    private void setStringSummaryForCameraSwitch(SharedPreferences pref) {
        String key = getString(R.string.key_cameraid);
        String cameraId = pref.getString(key, "");
        String summary = "";
        if (cameraId.equals(CameraHideMethods.CameraInfo.CAMERA_FACING_BACK + "")) {
            summary = getString(R.string.camera_facing_back);
        } else if (cameraId.equals(CameraHideMethods.CameraInfo.CAMERA_FACING_FRONT + "")) {
            summary = getString(R.string.camera_facing_front);
        } else {
            summary = cameraId;
        }
        ListPreference lp = (ListPreference) findPref(key);
        lp.setSummary(summary);
    }

    /*
     * Create key.
     */
    public static String createKey(int cameraId, String postfix) {
        return cameraId + postfix;
    }

    /*
     * Create key.
     */
    public static String createKey(int cameraId, String postfix, int index) {
        return cameraId + postfix + index;
    }

    private final int REQUEST_CODE = 1;

    /*
     * Launch LiveWallpaer picker.
     */
    private void launchLiveWallpaperPicker() {
        Intent intent = new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER);
        startActivityForResult(intent, REQUEST_CODE);
    }

    /*
     * Launch LiveWallpaer changer. Available from Android 4.1 Jelly Beans.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void launchLiveWallpaperChanger() {
        if (isChangeLiveWallpaperSupported()) {
            Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            String pkg = WalkAroundWallpaper.class.getPackage().getName();
            String cls = WalkAroundWallpaper.class.getCanonicalName();
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, new ComponentName(pkg, cls));
            startActivityForResult(intent, REQUEST_CODE);
        }
    }

    /*
     * Return true if end-user can go to setting menu for changing live wallpaer. Enabled since Android 4.1.
     */
    private boolean isChangeLiveWallpaperSupported() {
        final int SDK_JELLYBEANS = 16; // Build.VERSION_CODES.JELLY_BEAN
        return Build.VERSION.SDK_INT >= SDK_JELLYBEANS;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == REQUEST_CODE) {
            finish();
        }
    }

    /*
     * findPreference()
     */
    private Preference findPref(int stringId) {
        return findPref(getString(stringId));
    }

    @SuppressWarnings("deprecation")
    private Preference findPref(String key) {
        return findPreference(key);
    }
}
