package com.yaji.viewfinder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.util.Log;

/*
 * Utility class that provides the following hide API in android.hardware.Camera through reflection.
 *   1) Camera:addCallbackBuffer()
 *   2) Camera:setPreviewCallbackWithBuffer()
 */
public class CameraHideMethods {
    private static final String LOG_TAG = "yaji";

    private Method mMethodAddCallbackBuffer;
    private Method mMethodSetPreviewCallbackWithBuffer;
    private Method mMethodPreviewEnabled;
    private Method mMethodReconnect;
    private Method mMethodOpenLevel9;
    private Method mMethodGetNumberOfCamerasLevel9;
    private Method mMethodGetCameraInfoLevel9;

    /*
     * Constructor.
     */
    public CameraHideMethods() {
        try {
            Class<?> c = Class.forName("android.hardware.Camera");
            Method[] methods = c.getMethods();

            // Iterate methods to find what we want. There should be better way, using Class:getMethod().
            for (int i = 0; i < methods.length; i++) {
                String methodName = methods[i].getName();

                try {
                    if (methodName.equals("addCallbackBuffer")) { // Found.
                        mMethodAddCallbackBuffer = methods[i];
                    } else if (methodName.equals("setPreviewCallbackWithBuffer")) { // Found.
                        mMethodSetPreviewCallbackWithBuffer = methods[i];
                    } else if (methodName.equals("previewEnabled")) { // Found.
                        mMethodPreviewEnabled = methods[i];
                    } else if (methodName.equals("reconnect")) { // Found.
                        mMethodReconnect = methods[i];
                    } else if (methods[i].toGenericString().equals("public static android.hardware.Camera android.hardware.Camera.open(int)")) {
                        mMethodOpenLevel9 = methods[i];
                    } else if (methodName.equals("getNumberOfCameras")) {
                        mMethodGetNumberOfCamerasLevel9 = methods[i];
                    } else if (methodName.equals("getCameraInfo")) {
                        mMethodGetCameraInfoLevel9 = methods[i];
                    }
                } catch (Exception e) {
                    Log.w(LOG_TAG, "CameraHideMethods(), " + methodName);
                }
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "CameraHideMethods", e);
        }

    }

    /*
     * Reflection method for void addCallbackBuffer(byte[] callbackBuffer) in Camera class.
     */
    public void addCallbackBuffer(Camera camera, byte[] callbackBuffer) {
        try {
            if (mMethodAddCallbackBuffer != null) {
                Object[] argList = new Object[1];
                argList[0] = callbackBuffer;
                mMethodAddCallbackBuffer.invoke(camera, argList);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "CameraHideMethods:addCallbackBuffer()", e);
        }
    }

    /*
     * Reflection method for void setPreviewCallbackWithBuffer(PreviewCallback cb) in Camera class.
     */
    public void setPreviewCallbackWithBuffer(Camera camera, PreviewCallback cb) {
        try {
            if (mMethodSetPreviewCallbackWithBuffer != null) {
                Object[] argList = new Object[1];
                argList[0] = cb;
                mMethodSetPreviewCallbackWithBuffer.invoke(camera, argList);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "CameraHideMethods:mMethodSetPreviewCallbackWithBuffer()", e);
        }
    }

    /*
     * Reflection method for void previewEnabled() in Camera class.
     */
    public boolean previewEnabled(Camera camera) {
        boolean retVal = false;
        try {
            if (mMethodPreviewEnabled != null) {
                Boolean b = (Boolean) mMethodPreviewEnabled.invoke(camera);
                retVal = b.booleanValue();
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "CameraHideMethods:mMethodPreviewEnabled()", e);
        }

        return retVal;
    }

    /*
     * Reflection method for void previewEnabled() in Camera class.
     */
    public void reconnect(Camera camera) throws java.io.IOException {
        try {
            if (mMethodReconnect != null) {
                mMethodReconnect.invoke(camera);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "CameraHideMethods:mMethodReconnect()", e);
        }
    }

    /*
     * Reflection method for public static Camera open(int cameraId) in Camera class.
     */
    public Camera open(int cameraId) {
        Camera camera = null;
        try {
            if (mMethodOpenLevel9 != null) {
                Object[] argList = new Object[1];
                argList[0] = cameraId;
                camera = (Camera) mMethodOpenLevel9.invoke(null, argList);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "CameraHideMethods:mMethodOpenLevel9()", e);
        }

        return camera;
    }

    /*
     * Check if we have Camera::open(int cameraId) method.
     */
    public boolean hasCameraOpenLevel9() {
        return mMethodOpenLevel9 != null;
    }

    /*
     * Reflection method for public static int getNumberOfCameras() in Camera class.
     */
    public int getNumberOfCameras() {
        int num = 1;
        try {
            if (mMethodGetNumberOfCamerasLevel9 != null) {
                Integer integer = (Integer) mMethodGetNumberOfCamerasLevel9.invoke(null);
                num = integer.intValue();
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "CameraHideMethods:mMethodGetNumberOfCamerasLevel9()", e);
        }

        return num;
    }

    /*
     * Reflection method for public static Camera open(int cameraId) in Camera class.
     */
    public CameraInfo getCameraInfo(int cameraId) {
        CameraInfo wrapperCameraInfo = null;

        try {
            if (mMethodGetCameraInfoLevel9 != null) {
                Object[] argList = new Object[2];
                argList[0] = cameraId;
                Class<?> cls = Class.forName("android.hardware.Camera$CameraInfo");
                Constructor<?> con = cls.getConstructor();
                Object cameraInfo = con.newInstance();
                argList[1] = cameraInfo;
                mMethodGetCameraInfoLevel9.invoke(null, argList);

                Field facingField = cls.getDeclaredField("facing");
                Field orientationField = cls.getDeclaredField("orientation");
                int facing = facingField.getInt(cameraInfo);
                int orientation = orientationField.getInt(cameraInfo);
                wrapperCameraInfo = new CameraInfo();
                wrapperCameraInfo.facing = facing;
                wrapperCameraInfo.orientation = orientation;
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "CameraHideMethods:mMethodGetCameraInfoLevel9()", e);
        }

        return wrapperCameraInfo;
    }

    /*
     * Wrapper for android.hardware.Camera.CameraInfo defined in Android 9.
     */
    public static class CameraInfo {
        public static final int CAMERA_FACING_BACK = 0;
        public static final int CAMERA_FACING_FRONT = 1;
        public int facing;
        public int orientation;

        public static final int DEFAULT_ORIENTATION_BACK = 90;
        public static final int DEFAULT_ORIENTATION_FRONT = 270;
    }
}
