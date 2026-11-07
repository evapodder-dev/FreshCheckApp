package com.freshcheck.app.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.util.NotificationHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Started Service demonstration that runs an asynchronous audit
 * across local inventory records, flags expired items, and broadcasts
 * results to receivers and active activities.
 */
public class InventoryAuditService extends Service {
    private static final String TAG = "InventoryAuditService";
    public static final String ACTION_AUDIT_COMPLETE = "com.freshcheck.app.ACTION_INVENTORY_AUDIT_COMPLETE";
    public static final String ACTION_START_FOREGROUND = "com.freshcheck.app.ACTION_START_FOREGROUND";
    public static final String EXTRA_EXPIRING_COUNT = "expiring_count";
    public static final String EXTRA_EXPIRED_COUNT = "expired_count";
    public static final String EXTRA_TOTAL_COUNT = "total_count";

    private ExecutorService executorService;
    private FoodRepository foodRepository;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "InventoryAuditService onCreate() invoked");
        executorService = Executors.newSingleThreadExecutor();
        foodRepository = new FoodRepository(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "InventoryAuditService onStartCommand() started with startId: " + startId);

        if (intent != null && ACTION_START_FOREGROUND.equals(intent.getAction())) {
            startForeground(NotificationHelper.NOTIFICATION_ID_AUDIT,
                    NotificationHelper.buildForegroundServiceNotification(this, "Active pantry audit running in foreground..."));
        }

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Simulate processing and performing database audit
                    List<FoodItem> activeItems = foodRepository.getAllActive();
                    int expiringCount = 0;
                    int expiredCount = 0;
                    StringBuilder sampleItems = new StringBuilder();

                    for (FoodItem item : activeItems) {
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

                    Log.d(TAG, "Audit completed: Total=" + activeItems.size()
                            + ", Expiring=" + expiringCount + ", Expired=" + expiredCount);

                    // Show notification if urgent items found
                    if (expiringCount > 0 || expiredCount > 0) {
                        NotificationHelper.showExpiryAlert(
                                InventoryAuditService.this,
                                expiringCount,
                                expiredCount,
                                sampleItems.toString()
                        );
                    }

                    // Broadcast audit results to application receivers
                    Intent broadcastIntent = new Intent(ACTION_AUDIT_COMPLETE);
                    broadcastIntent.setPackage(getPackageName());
                    broadcastIntent.putExtra(EXTRA_EXPIRING_COUNT, expiringCount);
                    broadcastIntent.putExtra(EXTRA_EXPIRED_COUNT, expiredCount);
                    broadcastIntent.putExtra(EXTRA_TOTAL_COUNT, activeItems.size());
                    sendBroadcast(broadcastIntent);

                } catch (Exception e) {
                    Log.e(TAG, "Error during inventory audit", e);
                } finally {
                    try {
                        stopForeground(true);
                    } catch (Exception ignored) {}
                    stopSelf(startId);
                }
            }
        });

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // Started Service; binding not required
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "InventoryAuditService onDestroy() invoked");
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
