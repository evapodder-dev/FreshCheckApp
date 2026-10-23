package com.freshcheck.app.util;

import android.content.Context;
import android.util.Log;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.freshcheck.app.worker.ExpiryCheckWorker;

import java.util.concurrent.TimeUnit;

public final class WorkManagerScheduler {
    private static final String TAG = "WorkManagerScheduler";
    public static final String WORK_NAME_PERIODIC = "freshcheck_periodic_expiry_check";

    private WorkManagerScheduler() {}

    public static void schedulePeriodicExpiryCheck(Context context) {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                ExpiryCheckWorker.class,
                24, TimeUnit.HOURS,
                6, TimeUnit.HOURS
        ).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
        );
        Log.d(TAG, "Scheduled periodic 24-hour expiry check with WorkManager");
    }

    public static void runImmediateCheck(Context context) {
        OneTimeWorkRequest oneTimeWork = new OneTimeWorkRequest.Builder(ExpiryCheckWorker.class).build();
        WorkManager.getInstance(context).enqueue(oneTimeWork);
        Log.d(TAG, "Enqueued immediate one-time WorkManager expiry check");
    }
}
