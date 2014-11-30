package com.yaji.viewfinder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "yaji";
    private static boolean mReceived = false;

    @Override
    public void onReceive(Context context, Intent i) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())) {
            mReceived = true;
            long bootTime = SystemClock.elapsedRealtime();
            Log.d(LOG_TAG, "onReceive(), bootTime:" + bootTime);
        }
    }

    /*
     * Returns true if we have already received BOOT_COMPLETED intent.
     */
    public static boolean isReceived() {
        return mReceived;
    }
}
