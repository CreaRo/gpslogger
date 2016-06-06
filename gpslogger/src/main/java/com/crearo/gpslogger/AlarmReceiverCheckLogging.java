package com.crearo.gpslogger;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.crearo.gpslogger.common.Session;
import com.crearo.gpslogger.shortcuts.ShortcutStart;

/**
 * Created by rish on 28/3/16.
 */
public class AlarmReceiverCheckLogging extends BroadcastReceiver {
    private static final String DEBUG_TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(DEBUG_TAG, "Recurring alarm; Session Started? " + Session.isStarted());

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