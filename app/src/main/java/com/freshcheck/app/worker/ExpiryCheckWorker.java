package com.freshcheck.app.worker;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.util.NotificationHelper;

import java.util.List;

public class ExpiryCheckWorker extends Worker {
    private static final String TAG = "ExpiryCheckWorker";

    public ExpiryCheckWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "ExpiryCheckWorker running background expiry check...");

        try {
            FoodRepository repository = new FoodRepository(getApplicationContext());
            List<FoodItem> active = repository.getAllActive();

            int expiringCount = 0;
            int expiredCount = 0;
            StringBuilder sampleItems = new StringBuilder();

            for (FoodItem item : active) {
                long days = item.getDaysRemaining();
                if (days < 0) {
                    expiredCount++;
                    if (sampleItems.length() < 50) {
                        if (sampleItems.length() > 0) sampleItems.append(", ");
                        sampleItems.append(item.getName());
                    }
                } else if (days <= 3) {
                    expiringCount++;
                    if (sampleItems.length() < 50) {
                        if (sampleItems.length() > 0) sampleItems.append(", ");
                        sampleItems.append(item.getName());
                    }
                }
            }

            Log.d(TAG, "Worker results: Expiring=" + expiringCount + ", Expired=" + expiredCount);

            if (expiringCount > 0 || expiredCount > 0) {
                NotificationHelper.showExpiryAlert(
                        getApplicationContext(),
                        expiringCount,
                        expiredCount,
                        sampleItems.toString()
                );
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in ExpiryCheckWorker", e);
            return Result.failure();
        }
    }
}
