package com.mendhak.gpslogger;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.shortcuts.ShortcutStart;
import com.mendhak.gpslogger.shortcuts.ShortcutStop;

/**
 * Created by rish on 24/3/16.
 */
public class GpsLocationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {

            LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean gps_enabled = false;
            boolean network_enabled = false;

            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();

                Intent intent1 = new Intent(context, ShortcutStart.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
            } catch (Exception ex) {
            }

            try {
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();

                Intent intent1 = new Intent(context, ShortcutStart.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);
            } catch (Exception ex) {
            }

            if (!gps_enabled && !network_enabled) {
                Log.wtf("GLR", "Removing all notifs");
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancelAll();
                Log.wtf("GLR", "Removed all notifs");
                Toast.makeText(context, "Please Enable Your GPS", Toast.LENGTH_LONG).show();

                Session.setGpsEnabled(false);
                Session.setStarted(false);
                Intent intent1 = new Intent(context, ShortcutStop.class);
                intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent1);

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.wtf("GLR", "Creating one notif");
                        NotificationMaker.createGPSUnavailableNotification(context);
                        Log.wtf("GLR", "Created one notif");
                    }
                }, 2000);
            }
        }
    }
}