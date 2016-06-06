/*
*    This file is part of GPSLogger for Android.
*
*    GPSLogger for Android is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 2 of the License, or
*    (at your option) any later version.
*
*    GPSLogger for Android is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.crearo.gpslogger.senders.dropbox;


import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.dropbox.client2.session.Session;
import com.dropbox.client2.session.TokenPair;
import com.crearo.gpslogger.BuildConfig;
import com.crearo.gpslogger.common.AppSettings;
import com.crearo.gpslogger.common.PreferenceHelper;
import com.crearo.gpslogger.common.Strings;
import com.crearo.gpslogger.common.slf4j.Logs;
import com.crearo.gpslogger.senders.FileSender;
import com.crearo.gpslogger.ui.fragments.settings.DropboxAuthorizationFragment;
import com.path.android.jobqueue.CancelResult;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.TagConstraint;

import org.slf4j.Logger;

import java.io.File;
import java.util.List;


public class DropBoxManager extends FileSender {

    private static final Logger LOG = Logs.of(DropBoxManager.class);
    private static final Session.AccessType ACCESS_TYPE = Session.AccessType.APP_FOLDER;

    private final DropboxAPI<AndroidAuthSession> dropboxApi;
    private final PreferenceHelper preferenceHelper;

    public DropBoxManager(PreferenceHelper preferenceHelper) {
        this.preferenceHelper = preferenceHelper;
        AndroidAuthSession session = getSession();
        dropboxApi = new DropboxAPI<>(session);
    }

    /**
     * Whether the user has authorized GPSLogger with DropBox
     *
     * @return True/False
     */
    public boolean isLinked() {
        return dropboxApi.getSession().isLinked();
    }

    public boolean finishAuthorization() {
        AndroidAuthSession session = dropboxApi.getSession();
        if (!session.isLinked() && session.authenticationSuccessful()) {
            // Mandatory call to complete the auth
            session.finishAuthentication();

            // Store it locally in our app for later use
            TokenPair tokens = session.getAccessTokenPair();
            storeKeys(tokens.key, tokens.secret);
            return true;
        }

        return false;
    }


    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     *
     * @param key    The Access Key
     * @param secret The Access Secret
     */
    private void storeKeys(String key, String secret) {
        preferenceHelper.setDropBoxAccessKeyName(key);
        preferenceHelper.setDropBoxAccessSecret(secret);
    }

    private void clearKeys() {
        preferenceHelper.setDropBoxAccessKeyName(null);
        preferenceHelper.setDropBoxAccessSecret(null);
    }

    /**
     * Returns a Dropbox API session, which holds various auth and operational methods on it
     *
     * @return
     */
    protected AndroidAuthSession getSession() {
        AppKeyPair appKeyPair = new AppKeyPair(BuildConfig.DROPBOX_APP_KEY, BuildConfig.DROPBOX_APP_SECRET);
        AndroidAuthSession session;

        AccessTokenPair storedKeys = getKeys();
        if (storedKeys != null) {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE, storedKeys);
        } else {
            session = new AndroidAuthSession(appKeyPair, ACCESS_TYPE);
        }

        return session;
    }

    /**
     * Shows keeping the access keys returned from Trusted Authenticator in a local
     * store, rather than storing user name & password, and re-authenticating each
     * time (which is not to be done, ever).
     *
     * @return Array of [access_key, access_secret], or null if none stored
     */
    public AccessTokenPair getKeys() {

        AccessTokenPair pair = null;
        String key = preferenceHelper.getDropBoxAccessKeyName();
        String secret = preferenceHelper.getDropBoxAccessSecretName();
        if (!Strings.isNullOrEmpty(key) && !Strings.isNullOrEmpty(secret)) {
            pair = new AccessTokenPair(key, secret);
        }
        return pair;
    }

    public void startAuthentication(DropboxAuthorizationFragment dropboxAuthorizationFragment) {
        // Start the remote authentication
        dropboxApi.getSession().startAuthentication(dropboxAuthorizationFragment.getActivity());
    }

    public void unLink() {
        // Remove credentials from the session
        dropboxApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
    }

    @Override
    public void uploadFile(List<File> files) {
        for (File f : files) {
            LOG.debug(f.getName());
            uploadFile(f.getName());
        }
    }

    @Override
    public boolean isAvailable() {
        return
                isLinked()
                        && preferenceHelper.getDropBoxAccessKeyName() != null
                        && preferenceHelper.getDropBoxAccessSecretName() != null;
    }


    @Override
    public boolean hasUserAllowedAutoSending() {
        return preferenceHelper.isDropboxAutoSendEnabled();
    }

    public void uploadFile(final String fileName) {
        final JobManager jobManager = AppSettings.getJobManager();
        jobManager.cancelJobsInBackground(new CancelResult.AsyncCancelCallback() {
            @Override
            public void onCancelled(CancelResult cancelResult) {
                jobManager.addJobInBackground(new DropboxJob(fileName));
            }
        }, TagConstraint.ANY, DropboxJob.getJobTag(fileName));

    }

    @Override
    public boolean accept(File dir, String name) {
        return true;
    }

}




