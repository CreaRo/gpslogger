package com.crearo.gpslogger.senders.dropbox;




import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.crearo.gpslogger.common.PreferenceHelper;
import com.crearo.gpslogger.common.events.UploadEvents;
import com.crearo.gpslogger.common.slf4j.Logs;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;
import de.greenrobot.event.EventBus;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;


public class DropboxJob extends Job {


    private static final Logger LOG = Logs.of(DropboxJob.class);
    private static PreferenceHelper preferenceHelper = PreferenceHelper.getInstance();
    String fileName;
    DropboxAPI<AndroidAuthSession> dropboxApi;



    protected DropboxJob(String fileName) {
        super(new Params(1).requireNetwork().persist().addTags(getJobTag(fileName)));

        this.fileName = fileName;
    }

    @Override
    public void onAdded() {
    }

    @Override
    public void onRun() throws Throwable {
        File gpsDir = new File(preferenceHelper.getGpsLoggerFolder());
        File gpxFile = new File(gpsDir, fileName);

        FileInputStream fis = new FileInputStream(gpxFile);

        DropBoxManager manager = new DropBoxManager(PreferenceHelper.getInstance());
        AndroidAuthSession session = manager.getSession();
        dropboxApi = new DropboxAPI<>(session);
        DropboxAPI.Entry upEntry = dropboxApi.putFileOverwrite(gpxFile.getName(), fis, gpxFile.length(), null);
        LOG.info("DropBox upload complete. Rev: " + upEntry.rev);
        fis.close();
        EventBus.getDefault().post(new UploadEvents.Dropbox().succeeded());

    }


    @Override
    protected void onCancel() {
    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        EventBus.getDefault().post(new UploadEvents.Dropbox().failed("Could not upload to Dropbox", throwable));
        LOG.error("Could not upload to Dropbox", throwable);
        return false;
    }

    public static String getJobTag(String fileName) {
        return "DROPBOX" + fileName;
    }
}
