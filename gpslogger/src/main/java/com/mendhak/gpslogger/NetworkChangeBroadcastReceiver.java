package com.mendhak.gpslogger;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.shortcuts.ShortcutStart;

/**
 * Created by rish on 1/4/16.
 */
public class NetworkChangeBroadcastReceiver extends BroadcastReceiver {

    private static final String DEBUG_TAG = "NetworkChangeBroadcast";

    @Override
    public void onReceive(final Context context, final Intent intent) {

        if (Session.isStarted()) {
            // Do nothing
            Log.d(DEBUG_TAG, "Session is already running");
        } else {
            // Start the fucking session.
            Log.d(DEBUG_TAG, "Session isn't on. Will start.");
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();

            Intent intent1 = new Intent(context, ShortcutStart.class);
            intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent1);
        }

    }
}