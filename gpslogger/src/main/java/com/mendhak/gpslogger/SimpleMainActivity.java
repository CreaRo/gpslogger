package com.mendhak.gpslogger;

import android.app.AlarmManager;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mendhak.gpslogger.common.EventBusHook;
import com.mendhak.gpslogger.common.PreferenceHelper;
import com.mendhak.gpslogger.common.Session;
import com.mendhak.gpslogger.common.Systems;
import com.mendhak.gpslogger.common.events.ServiceEvents;
import com.mendhak.gpslogger.common.events.UploadEvents;
import com.mendhak.gpslogger.common.slf4j.Logs;
import com.mendhak.gpslogger.loggers.Files;
import com.mendhak.gpslogger.senders.FileSender;
import com.mendhak.gpslogger.senders.FileSenderFactory;
import com.mendhak.gpslogger.senders.email.AutoEmailManager;
import com.mendhak.gpslogger.senders.email.LastTimeSentChecker;
import com.mendhak.gpslogger.ui.Dialogs;
import com.mendhak.gpslogger.ui.fragments.display.GpsBigViewFragment;
import com.mendhak.gpslogger.ui.fragments.display.GpsDetailedViewFragment;
import com.mendhak.gpslogger.ui.fragments.display.GpsLogViewFragment;
import com.mendhak.gpslogger.ui.fragments.display.GpsSimpleViewFragment;

import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import de.greenrobot.event.EventBus;

public class SimpleMainActivity extends AppCompatActivity {

    private PreferenceHelper preferenceHelper;
    private AutoEmailManager aem;
    private static Intent serviceIntent;
    private static final Logger LOG = Logs.of(SimpleMainActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_main);

        preferenceHelper = PreferenceHelper.getInstance();
        this.aem = new AutoEmailManager(preferenceHelper);

        setCustomPreferences();

        findViewById(R.id.buttosimple_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                selectAndEmailFile();
                LastTimeSentChecker.checkAllFileSentTime(getApplicationContext());
            }
        });

        loadDefaultFragmentView();
        startAndBindService();
        registerEventBus();

        setRecurringAlarmCheckLogging(getApplicationContext());
    }

    private void selectAndEmailFile() {
        showFileListDialog(FileSenderFactory.getEmailSender());
    }

    private void showFileListDialog(final FileSender sender) {

        if (!Systems.isNetworkAvailable(this)) {
            Dialogs.alert(getString(R.string.sorry), getString(R.string.no_network_message), this);
            return;
        }

        final File gpxFolder = new File(preferenceHelper.getGpsLoggerFolder());

        if (gpxFolder.exists() && Files.fromFolder(gpxFolder, sender).length > 0) {
            File[] enumeratedFiles = Files.fromFolder(gpxFolder, sender);

            //Order by last modified
            Arrays.sort(enumeratedFiles, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    if (f1 != null && f2 != null) {
                        return -1 * Long.valueOf(f1.lastModified()).compareTo(f2.lastModified());
                    }
                    return -1;
                }
            });

            List<String> fileList = new ArrayList<>(enumeratedFiles.length);

            for (File f : enumeratedFiles) {
                fileList.add(f.getName());
            }

            final String[] files = fileList.toArray(new String[fileList.size()]);

            new MaterialDialog.Builder(this)
                    .title(R.string.osm_pick_file)
                    .items(files)
                    .positiveText(R.string.ok)
                    .itemsCallbackMultiChoice(null, new MaterialDialog.ListCallbackMultiChoice() {
                        @Override
                        public boolean onSelection(MaterialDialog materialDialog, Integer[] integers, CharSequence[] charSequences) {

                            List<Integer> selectedItems = Arrays.asList(integers);

                            List<File> chosenFiles = new ArrayList<>();

                            for (Object item : selectedItems) {
                                LOG.info("Selected file to upload- " + files[Integer.valueOf(item.toString())]);
                                chosenFiles.add(new File(gpxFolder, files[Integer.valueOf(item.toString())]));
                            }

                            if (chosenFiles.size() > 0) {
                                Dialogs.progress(SimpleMainActivity.this, getString(R.string.please_wait), getString(R.string.please_wait));
                                sender.uploadFile(chosenFiles);
                            }
                            return true;
                        }
                    }).show();

        } else {
            Dialogs.alert(getString(R.string.sorry), getString(R.string.no_files_found), this);
        }
    }

    private void registerEventBus() {
        EventBus.getDefault().register(this);
    }

    private void unregisterEventBus() {
        try {
            EventBus.getDefault().unregister(this);
        } catch (Throwable t) {
            //this may crash if registration did not go through. just be safe
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startAndBindService();
    }

    private final ServiceConnection gpsServiceConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            LOG.debug("Disconnected from GPSLoggingService from MainActivity");
            //loggingService = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            LOG.debug("Connected to GPSLoggingService from MainActivity");
            //loggingService = ((GpsLoggingService.GpsLoggingBinder) service).getService();
        }
    };

    private void startAndBindService() {
        serviceIntent = new Intent(this, GpsLoggingService.class);
        // Start the service in case it isn't already running
        startService(serviceIntent);
        // Now bind to service
        bindService(serviceIntent, gpsServiceConnection, Context.BIND_AUTO_CREATE);
        Session.setBoundToService(true);
    }

    private void loadDefaultFragmentView() {
        int currentSelectedPosition = 0;
        loadFragmentView(currentSelectedPosition);
    }

    private void loadFragmentView(int position) {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();

        switch (position) {
            default:
            case 0:
                transaction.replace(R.id.simple_container, GpsSimpleViewFragment.newInstance());
                break;
            case 1:
                transaction.replace(R.id.simple_container, GpsDetailedViewFragment.newInstance());
                break;
            case 2:
                transaction.replace(R.id.simple_container, GpsBigViewFragment.newInstance());
                break;
            case 3:
                transaction.replace(R.id.simple_container, GpsLogViewFragment.newInstance());
                break;
        }
        transaction.commitAllowingStateLoss();
    }

    private void setCustomPreferences() {
        PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
        preferenceHelper.setSmtpServer("smtp.gmail.com");
        preferenceHelper.setSmtpPort("465");
        preferenceHelper.setSmtpSsl(true);
        preferenceHelper.setSmtpPassword("androidgpslogger");
        preferenceHelper.setSmtpUsername("gpsloggera@gmail.com");
//        preferenceHelper.setSmtpPassword("!ntOthEwilD");
//        preferenceHelper.setSmtpUsername("androrish@gmail.com");
        preferenceHelper.setAutoSendEnabled(true);
        preferenceHelper.setAutoEmailTargets("gpsloggera@gmail.com");
//        preferenceHelper.setAutoEmailTargets("androrish@gmail.com");
        preferenceHelper.setHideNotificationButtons(true);
        preferenceHelper.setSendZipFile(false);
        preferenceHelper.setAutoSendInterval(60);
        preferenceHelper.setEmailAutoSendEnabled(true);
        preferenceHelper.setStartLoggingOnBootup(true);
        preferenceHelper.setStartLoggingOnAppLaunch(true);
        preferenceHelper.setLogToPlainText(true);
        preferenceHelper.setLogToGpx(false);
        preferenceHelper.setPrefixSerialToFileName(true);
    }

    public void onWaitingForLocation(boolean inProgress) {
//        ProgressBar fixBar = (ProgressBar) findViewById(R.id.progressBarGpsFix);
//        fixBar.setVisibility(inProgress ? View.VISIBLE : View.INVISIBLE);
    }

    public void setAnnotationDone() {
        Session.setAnnotationMarked(false);
//        enableDisableMenuItems();
    }

    public void setAnnotationReady() {
        Session.setAnnotationMarked(true);
//        enableDisableMenuItems();
    }

    @EventBusHook
    public void onEventMainThread(UploadEvents.AutoEmail upload) {
        LOG.debug("Auto Email Event completed, success: " + upload.success + upload.message);
        Dialogs.hideProgress();

        if (!upload.success) {
            Log.d("SimpleMainActivity", "Unable To Sent" + upload.emailSubject);
        } else {
            Log.d("SimpleMainActivity", "Sent " + upload.emailSubject + " successfully");
            String fileName = "2016" + upload.emailSubject.split("2016")[1];
            fileName = fileName.replace(".txt", "");
            Log.d("SimpleMainActivity", fileName);

            LastTimeSentChecker.updateFileSentTime(getApplicationContext(), fileName);

        }

        if (!upload.success) {
            LOG.error(getString(R.string.autoemail_title)
                    + "-"
                    + getString(R.string.upload_failure));
//            if (userInvokedUpload) {
//                Dialogs.error(getString(R.string.sorry), getString(R.string.upload_failure), upload.message, upload.throwable, this);
//                userInvokedUpload = false;
//            }
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.WaitingForLocation waitingForLocation) {
        onWaitingForLocation(waitingForLocation.waiting);
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.AnnotationStatus annotationStatus) {
        if (annotationStatus.annotationWritten) {
            setAnnotationDone();
        } else {
            setAnnotationReady();
        }
    }

    @EventBusHook
    public void onEventMainThread(ServiceEvents.LoggingStatus loggingStatus) {
//        enableDisableMenuItems();
    }

    public static void setRecurringAlarmCheckLogging(Context context) {

        Intent downloader = new Intent(context, AlarmReceiverCheckLogging.class);
        PendingIntent recurringDownload = PendingIntent.getBroadcast(context, 0, downloader, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarms = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + 10 * 1000,
                AlarmManager.INTERVAL_HALF_HOUR, recurringDownload); /*AlarmManager.INTERVAL_FIFTEEN_MINUTES*/
    }

//    @EventBusHook
//    public void onEventMainThread(ProfileEvents.CreateNewProfile createProfileEvent) {
//
//        LOG.debug("Creating profile: " + createProfileEvent.newProfileName);
//
//        try {
//            File f = new File(Files.storageFolder(SimpleMainActivity.this), createProfileEvent.newProfileName + ".properties");
//            f.createNewFile();
//
//            populateProfilesList();
//
//        } catch (IOException e) {
//            LOG.error("Could not create properties file for new profile ", e);
//        }
//
//    }
//
//    @EventBusHook
//    public void onEventMainThread(ProfileEvents.DeleteProfile deleteProfileEvent) {
//        LOG.debug("Deleting profile: " + deleteProfileEvent.profileName);
//        File f = new File(Files.storageFolder(SimpleMainActivity.this), deleteProfileEvent.profileName + ".properties");
//        f.delete();
//
//        populateProfilesList();
//    }
}