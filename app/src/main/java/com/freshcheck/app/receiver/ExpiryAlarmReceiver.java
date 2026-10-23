package com.freshcheck.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.freshcheck.app.database.FoodRepository;
import com.freshcheck.app.model.FoodItem;
import com.freshcheck.app.util.NotificationHelper;

import java.util.List;

public class ExpiryAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "ExpiryAlarmReceiver";
    public static final String ACTION_CHECK_EXPIRY = "com.freshcheck.app.ACTION_CHECK_EXPIRY";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "ExpiryAlarmReceiver onReceive() triggered with action: " + intent.getAction());

        FoodRepository repository = new FoodRepository(context);
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

        if (expiringCount > 0 || expiredCount > 0) {
            NotificationHelper.showExpiryAlert(
                    context,
                    expiringCount,
                    expiredCount,
                    sampleItems.toString()
            );
        }
    }
}
