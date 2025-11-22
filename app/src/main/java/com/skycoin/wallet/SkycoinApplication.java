package com.skycoin.wallet;

import android.app.Application;
import android.util.Log;

import com.skycoin.wallet.db.DatabaseHelper;
import com.skycoin.wallet.encryption.EncryptionManager;

public class SkycoinApplication extends Application {

    private static final String TAG = SkycoinApplication.class.getName();

    private DatabaseHelper mDb;

    public void onCreate() {
        super.onCreate();
        mDb = new DatabaseHelper(this);
        Log.d(TAG, "Starting Application");
        try {
            EncryptionManager.generateKey(); // try this on startup. every time after the first will just check and NO-OP
        } catch (Exception ex) {
            Log.e(TAG,"could not generate encryption key",ex);
            // TODO: show popup and die on "OK"
            throw new RuntimeException("Could not generate encryption key. not safe to continue",ex);
        }
    }

    public DatabaseHelper getDb() {
        return mDb;
    }

}
