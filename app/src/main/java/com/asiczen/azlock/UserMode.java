package com.asiczen.azlock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
//import static com.google.android.gms.internal.zzs.TAG;

/**
 * Created by somnath on 26-10-2017.
 */

class UserMode extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "usermode.db";
    private static final String TABLE_NAME = "usrmod_table";
    private static final String col_1 = "Door";
    private static final String col_2 = "User";
    private final String TAG = UserMode.class.getSimpleName();
    public UserMode(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("create table " + TABLE_NAME +"("+col_1+" TEXT , "+col_2+ " TEXT ) ");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public void insertData(String Doorid, String mode) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(col_1,Doorid);
        contentValues.put(col_2, mode);
        sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
    }

    public Cursor getData(String Doorid)
    {
        Log.d(TAG,"doorid = " + Doorid);
        SQLiteDatabase db=this.getReadableDatabase();
        return db.rawQuery("select * from "+TABLE_NAME+" where "+ col_1+" = '" + Doorid+ "'",null);
    }
    public void updateData(String Id,String mode) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(col_1, Id);
        contentValues.put(col_2, mode);
        Log.d(TAG, Id);
        sqLiteDatabase.update(TABLE_NAME, contentValues, "Door= ?", new String[]{Id});
    }
    public void deleteData(String id){
        SQLiteDatabase sqLiteDatabase=this.getWritableDatabase();
        sqLiteDatabase.delete(TABLE_NAME,col_1+"=?",new String[]{id});
    }
}
