package com.freshcheck.app.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.freshcheck.app.service.InventoryAuditService;

public class InventoryAuditReceiver extends BroadcastReceiver {
    private static final String TAG = "InventoryAuditReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (InventoryAuditService.ACTION_AUDIT_COMPLETE.equals(intent.getAction())) {
            int expiring = intent.getIntExtra(InventoryAuditService.EXTRA_EXPIRING_COUNT, 0);
            int expired = intent.getIntExtra(InventoryAuditService.EXTRA_EXPIRED_COUNT, 0);
            int total = intent.getIntExtra(InventoryAuditService.EXTRA_TOTAL_COUNT, 0);

            Log.d(TAG, "InventoryAuditReceiver received audit complete broadcast: Total="
                    + total + ", Expiring=" + expiring + ", Expired=" + expired);

            String msg = "Audit Finished: " + expiring + " item(s) expiring soon, " + expired + " expired.";
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        }
    }
}
