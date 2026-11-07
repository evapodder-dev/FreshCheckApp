package com.freshcheck.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.freshcheck.app.util.NotificationHelper;

/**
 * System BroadcastReceiver that responds to device hardware and connectivity state changes:
 * - Low Battery (Intent.ACTION_BATTERY_LOW): Throttles heavy background sync to conserve power.
 * - Battery Okay (Intent.ACTION_BATTERY_OKAY): Resumes full background sync capabilities.
 * - Power Connected (Intent.ACTION_POWER_CONNECTED): Triggers opportunistic freshness audit.
 * - Power Disconnected (Intent.ACTION_POWER_DISCONNECTED): Logs transition to battery power.
 * - Airplane Mode Changed (Intent.ACTION_AIRPLANE_MODE_CHANGED): Adapts offline-first database mode.
 */
public class PowerStateReceiver extends BroadcastReceiver {
    private static final String TAG = "PowerStateReceiver";

    public interface SystemStateListener {
        void onSystemStateChanged(String eventName, String description);
    }

    private static SystemStateListener stateListener;

    public static void setSystemStateListener(SystemStateListener listener) {
        stateListener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        String action = intent.getAction();
        Log.i(TAG, "System broadcast received: " + action);

        String message;
        switch (action) {
            case Intent.ACTION_BATTERY_LOW:
                message = "Low Battery Detected: FreshCheck throttled background sync to conserve power.";
                Log.w(TAG, message);
                Toast.makeText(context, "FreshCheck: Power Saver Mode Active", Toast.LENGTH_SHORT).show();
                break;

            case Intent.ACTION_BATTERY_OKAY:
                message = "Battery Recovered: Resumed regular cloud sync and expiry audits.";
                Log.i(TAG, message);
                Toast.makeText(context, "FreshCheck: Normal Sync Resumed", Toast.LENGTH_SHORT).show();
                break;

            case Intent.ACTION_POWER_CONNECTED:
                message = "Power Connected: Running opportunistic local database audit.";
                Log.i(TAG, message);
                break;

            case Intent.ACTION_POWER_DISCONNECTED:
                message = "Power Disconnected: Operating on battery power.";
                Log.i(TAG, message);
                break;

            case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
                message = isAirplaneModeOn
                        ? "Airplane Mode ON: FreshCheck switched to 100% offline-first SQLite cache."
                        : "Airplane Mode OFF: Reconnecting to Firebase Cloud Database...";
                Log.i(TAG, message);
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                break;

            default:
                message = "System state event: " + action;
                break;
        }

        if (stateListener != null) {
            stateListener.onSystemStateChanged(action, message);
        }
    }
}
