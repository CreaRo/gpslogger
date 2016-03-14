package com.mendhak.gpslogger.ui;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.SimpleMainActivity;
import com.mendhak.gpslogger.common.PreferenceHelper;

public class InitActivity extends AppCompatActivity {

    public final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init);

        Log.d("INITACT", "The phone number is " + PreferenceHelper.getInstance().getUserPhoneNumber());

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermissionPhoneNumber();
        } else {
            startActivity(new Intent(InitActivity.this, SimpleMainActivity.class));
        }
    }

    private void checkPermissionPhoneNumber() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.GET_ACCOUNTS}, MY_PERMISSIONS_REQUEST_READ_CONTACTS);
        } else {
            startActivity(new Intent(InitActivity.this, SimpleMainActivity.class));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!

                    PreferenceHelper.getInstance().setUserPhoneNumber(getEmail(getApplicationContext()));
                    Log.d("INITACT", "The phone number is " + PreferenceHelper.getInstance().getUserPhoneNumber());
                    startActivity(new Intent(InitActivity.this, SimpleMainActivity.class));
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(getApplicationContext(), "We Need This Permission to identify you! Please give! Pretty please!", Toast.LENGTH_LONG).show();
                    checkPermissionPhoneNumber();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    static String getEmail(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = getAccount(accountManager);

        if (account == null) {
            return null;
        } else {
            return account.name;
        }
    }

    private static Account getAccount(AccountManager accountManager) {
        Account[] accounts = accountManager.getAccountsByType("com.google");
        Account account;
        if (accounts.length > 0) {
            account = accounts[0];
        } else {
            account = null;
        }
        return account;
    }
}
