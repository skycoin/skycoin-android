package com.skycoin.wallet.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = DatabaseHelper.class.getName();

    public static final int DB_VERSION = 1;

    public static final String DATABASE_NAME = "skycoin_tx.db";
    public static final String TABLE_NAME_TRANSACTIONS = "transactions";

    public static final String COL_TXID = "TXID";
    public static final String COL_NOTE = "NOTE";
    public static final String COL_TIME = "TIME";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME_TRANSACTIONS +
                " (ID INTEGER PRIMARY KEY AUTOINCREMENT,"+
                COL_TXID +" TEXT,"+
                COL_NOTE +" TEXT,"+
                COL_TIME +" INTEGER)");
        db.execSQL("CREATE INDEX idx_txid ON "+TABLE_NAME_TRANSACTIONS+" ("+COL_TXID+")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    //    db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME_TRANSACTIONS);
    //    onCreate(db);
    }

    public boolean insertTx(String txid, String note, long time) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_TXID, txid);
        contentValues.put(COL_NOTE, note);
        contentValues.put(COL_TIME, time);
        long result = db.insert(TABLE_NAME_TRANSACTIONS,null ,contentValues);
        if(result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public String getNoteForTx(String txid) {
        SQLiteDatabase db = getReadableDatabase();
         Cursor res = db.query(TABLE_NAME_TRANSACTIONS,new String[] {COL_NOTE}, COL_TXID + " = ?",
                new String[] {txid}, null,null, null);

        String note = null;
        if(res.moveToNext()) {
            note = res.getString(res.getColumnIndex(COL_NOTE));
        }
        res.close();
        return note;
    }


}