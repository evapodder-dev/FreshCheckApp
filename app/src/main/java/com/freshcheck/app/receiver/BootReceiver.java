package com.freshcheck.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.freshcheck.app.util.WorkManagerScheduler;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device booted. Rescheduling WorkManager expiry checks...");
            WorkManagerScheduler.schedulePeriodicExpiryCheck(context);
        }
    }
}
