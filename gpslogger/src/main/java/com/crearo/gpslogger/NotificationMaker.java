package com.crearo.gpslogger;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import com.crearo.gpslogger.ui.Dialogs;

/**
 * Created by rish on 25/3/16.
 */
public class NotificationMaker {
    public static void createGPSUnavailableNotification(Context context) {

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);

        mBuilder.setContentTitle("Enable Your GPS");
        mBuilder.setContentText("We really need you to turn on your GPS, and restart logging.");
        mBuilder.setTicker("Enable Your GPS");
        mBuilder.setSmallIcon(R.drawable.notification);
        mBuilder.setOngoing(true);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, SimpleMainActivity.class);
        resultIntent.putExtra("notificationId", 10);

        //This ensures that navigating backward from the Activity leads out of the app to Home page
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Adds the back stack for the Intent
        stackBuilder.addParentStack(SimpleMainActivity.class);

        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_ONE_SHOT //can only be used once
                );
        // start the activity when the user clicks the notification text
        mBuilder.setContentIntent(resultPendingIntent);

        NotificationManager myNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // pass the Notification object to the system
        myNotificationManager.notify(10, mBuilder.build());

        try {
            Dialogs.alert("Turn On GPS!", "Please enable GPS for our research!", context, new Dialogs.MessageBoxCallback() {
                @Override
                public void messageBoxResult(int which) {
                    
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}