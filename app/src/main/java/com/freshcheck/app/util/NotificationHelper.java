package com.freshcheck.app.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.freshcheck.app.R;
import com.freshcheck.app.ui.activity.MainActivity;

public final class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    public static final String CHANNEL_ID = "freshcheck_expiry_channel";
    public static final int NOTIFICATION_ID_EXPIRY = 1001;
    public static final int NOTIFICATION_ID_AUDIT = 1002;

    private NotificationHelper() {}

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_desc);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created: " + CHANNEL_ID);
            }
        }
    }

    public static void showExpiryAlert(Context context, int expiringCount, int expiredCount, String sampleItemNames) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Notification permission not granted, skipping alert");
            return;
        }

        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("target_tab", "inventory");

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        String title;
        String message;

        if (expiredCount > 0 && expiringCount > 0) {
            title = "⚠️ Food Expiry Alert (" + (expiringCount + expiredCount) + " items)";
            message = expiredCount + " item(s) expired, " + expiringCount + " expiring soon: " + sampleItemNames;
        } else if (expiredCount > 0) {
            title = "⚠️ Items Expired!";
            message = expiredCount + " food item(s) are past their expiry date: " + sampleItemNames;
        } else if (expiringCount > 0) {
            title = "⏳ Items Expiring Soon!";
            message = expiringCount + " item(s) expiring within 3 days: " + sampleItemNames;
        } else {
            return; // Nothing urgent to notify
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID_EXPIRY, builder.build());
            Log.d(TAG, "Notification posted: " + title);
        }
    }

    public static boolean hasNotificationPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS")
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static android.app.Notification buildForegroundServiceNotification(Context context, String contentText) {
        createNotificationChannel(context);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_database)
                .setContentTitle("FreshCheck Inventory Audit Service")
                .setContentText(contentText != null ? contentText : "Auditing local pantry database & freshness...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
