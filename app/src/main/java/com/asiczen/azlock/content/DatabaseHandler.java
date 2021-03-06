package com.asiczen.azlock.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.asiczen.azlock.GuestLogActivity;
import com.asiczen.azlock.R;
import com.asiczen.azlock.app.model.AccessLog;
import com.asiczen.azlock.app.model.BridgeDetail;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.app.model.Owner;
import com.asiczen.azlock.app.model.User;
import com.asiczen.azlock.app.model.WifiNetwork;
import com.asiczen.azlock.util.DatabaseUtility;
import com.asiczen.azlock.util.DateTimeFormat;
import com.asiczen.azlock.util.ImageUtility;
import com.asiczen.azlock.util.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/*
 * Created by user on 8/13/2015.
 */
public final class DatabaseHandler extends SQLiteOpenHelper implements DatabaseUtility {

    private final Context mContext;
    private final AppContext appContext;
    //private SQLiteDatabase writableDatabase;
    private final String TAG = DatabaseHandler.class.getSimpleName();
    private static final String TABLE_NAME = "logindata";
    private static final String C0L_1 = "EMAIL";

    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = context;
        appContext = AppContext.getContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /*Log.d("DatabaseHandler", "Dropping Tables");
        db.execSQL("DROP TABLE IF EXISTS " + UserTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RegisteredDoorTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DoorTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LogTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WifiConfigTable.TABLE_NAME);*/

        Log.d("DatabaseHandler", "Creating Tables");
        String CREATE_USER_TABLE = "CREATE TABLE " + UserTable.TABLE_NAME + "(" + UserTable.ID + " TEXT PRIMARY KEY, " + UserTable.NAME + " TEXT, "
                + UserTable.PHONE + " TEXT, " + UserTable.EMAIL + " TEXT, " + UserTable.IMAGE + " BLOB, " + UserTable.PIN + " TEXT, "
                + UserTable.ACCESS_MODE + " TEXT, " + UserTable.ACCESS_TYPE + " TEXT, " + UserTable.START_ACCESS_DATETIME + " TEXT, "
                + UserTable.END_ACCESS_DATETIME + " TEXT)";
        String CREATE_REGISTERED_DOOR_TABLE = "CREATE TABLE " + RegisteredDoorTable.TABLE_NAME + "(" + RegisteredDoorTable.ID + " INTEGER PRIMARY KEY, "
                + RegisteredDoorTable.USER_ID + " TEXT, " + RegisteredDoorTable.DOOR_ID + " TEXT, " + RegisteredDoorTable.KEY_STATUS + " INTEGER)";
        String CREATE_DOOR_TABLE = "CREATE TABLE " + DoorTable.TABLE_NAME + "(" + DoorTable.ID + " TEXT PRIMARY KEY, "
                + DoorTable.NAME + " TEXT, " + DoorTable.ROUTER_ADDRESS + " TEXT, " + DoorTable.ROUTER_PORT + " INTEGER, "
                + DoorTable.DOOR_IP + " TEXT, " + DoorTable.SUBNET_MASK + " TEXT, " + DoorTable.DEFAULT_GATEWAY + " TEXT)";
        String CREATE_LOG_TABLE = "CREATE TABLE " + LogTable.TABLE_NAME + "(" + LogTable.ID + " INTEGER PRIMARY KEY, "
                + LogTable.ACCESS_DATE_TIME + " DATETIME, " + LogTable.ACCESS_STATUS + " TEXT, " + LogTable.USER_ID
                + " TEXT, " + LogTable.DOOR_ID + " TEXT, " + LogTable.ACCESS_FAILURE_REASON + " TEXT)";
        String CREATE_WIFI_NETWORK_TABLE = "CREATE TABLE " + WifiConfigTable.TABLE_NAME + "(" + WifiConfigTable.ID + " INTEGER PRIMARY KEY, "
                + WifiConfigTable.SSID + " TEXT, " + WifiConfigTable.SECURITY + " TEXT, " + WifiConfigTable.PASSWORD + " TEXT, "
                + WifiConfigTable.AUTO_CONNECT + " INTEGER)";
        String CREATE_DISPLAY_TABLE = "CREATE TABLE " + DisplayTable.TABLE_NAME + "(" + DisplayTable.MAC_ID + " TEXT PRIMARY KEY, " + DisplayTable.DOOR_NAME + " TEXT, " + DisplayTable.DATE_TIME + " DATETIME DEFAULT CURRENT_TIMESTAMP)";
        String CREATE_BRIDGE_TABLE = "CREATE TABLE " + BridgeTable.TABLE_NAME + "(" + BridgeTable.BRIDGE_ID + " TEXT PRIMARY KEY, " + BridgeTable.PASSWORD + " TEXT)";

        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_REGISTERED_DOOR_TABLE);
        db.execSQL(CREATE_DOOR_TABLE);
        db.execSQL(CREATE_LOG_TABLE);
        db.execSQL(CREATE_WIFI_NETWORK_TABLE);
        db.execSQL(CREATE_DISPLAY_TABLE);
        String create_login_table = " CREATE TABLE " + TABLE_NAME + "(" + C0L_1 + " TEXT PRIMARY KEY)";
        db.execSQL(create_login_table);
        db.execSQL(CREATE_BRIDGE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        Log.d(TAG, "onUpgrade: ");
        /*if (newVersion > oldVersion) {
            db.execSQL("ALTER TABLE "+UserTable.TABLE_NAME+" ADD COLUMN "+UserTable.GROUP+" TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE "+UserTable.TABLE_NAME+" ADD COLUMN "+UserTable.START_ACCESS_DATETIME+" TEXT DEFAULT NULL");
            db.execSQL("ALTER TABLE "+UserTable.TABLE_NAME+" ADD COLUMN "+UserTable.ACCESS_DURATION+" TEXT DEFAULT NULL");
        }*/
        // Drop older table if existed
        //Log.d("DatabaseHandler", "Dropping Tables");
        db.execSQL("DROP TABLE IF EXISTS " + UserTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RegisteredDoorTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DoorTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LogTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WifiConfigTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DisplayTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + BridgeTable.TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    public void insertDisplayTableInfo(String doorName, String macId) {
        SQLiteDatabase write = this.getWritableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        try {
            ContentValues userValues = new ContentValues();
            userValues.put(DisplayTable.DOOR_NAME, doorName);
            userValues.put(DisplayTable.MAC_ID, macId);
            userValues.put(DisplayTable.DATE_TIME, dateFormat.format(date));
            long ii = write.insertOrThrow(DisplayTable.TABLE_NAME, null, userValues);
            Log.d(TAG, "write data=" + ii);
        } catch (Exception e) {
            ContentValues userValues = new ContentValues();
            userValues.put(DisplayTable.DOOR_NAME, doorName);
            userValues.put(DisplayTable.DATE_TIME, dateFormat.format(date));
            Log.d(TAG, "update2");
            long ii = write.update(DisplayTable.TABLE_NAME, userValues, DisplayTable.MAC_ID + "=?", new String[]{macId});
            Log.d(TAG, "write data1=" + ii);
            //e.printStackTrace();
            Log.d(TAG, "exception found");
        }
    }

    public ArrayList<AppContext.DisplayTableContent> getDataFromDisplayTable() {
        Log.d(TAG, "getDataFromDisplayTable is called");
        ArrayList<AppContext.DisplayTableContent> list = new ArrayList<>();
        SQLiteDatabase read = this.getReadableDatabase();
        // Cursor  cursor = read.rawQuery("SELECT * FROM "+DisplayTable.TABLE_NAME,null);
        //Cursor  cursor = read.rawQuery(DisplayTable.TABLE_NAME,new String[]{"SELECT * FROM "+DisplayTable.TABLE_NAME},null);
        Cursor cursor = read.query(DisplayTable.TABLE_NAME, null, null, null,
                null, null, DisplayTable.DATE_TIME + " DESC LIMIT 3");
        try {
            while (cursor.moveToNext()) {
                String door_name = cursor.getString(cursor.getColumnIndex(DisplayTable.DOOR_NAME));
                String mac_id = cursor.getString(cursor.getColumnIndex(DisplayTable.MAC_ID));
                String DATE = cursor.getString(cursor.getColumnIndex(DisplayTable.DATE_TIME));
                Log.d(TAG, "door_name=" + door_name);
                Log.d(TAG, "mac_id=" + mac_id);
                Log.d(TAG, "DATE=" + DATE);
                AppContext.DisplayTableContent content = new AppContext.DisplayTableContent(1, door_name, mac_id);
                list.add(content);

            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public void insertData(String email) {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(C0L_1, email);
            db.insert(TABLE_NAME, null, contentValues);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public ArrayList<String> getData() {
        ArrayList<String> list = new ArrayList<>();
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor res = db.rawQuery("select * from " + TABLE_NAME, null);
        if (res.getCount() != 0) {
            while (res.moveToNext()) {
                list.add(res.getString(0));
            }
            res.close();
        }
        return list;
    }

    /**
     * Insert new user (owner or guest) using {Link User} object.
     * #user object must contain valid userId which can be set using
     * {Link User.setId} method.
     * <p>
     * param user
     * return {Link true} if user is successfully registered
     */
    public boolean insert(User user) {
        Log.d(TAG, "DB/Inserting User:" + user);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues userValues = new ContentValues();
        long rowUser;
        boolean isExist = false;
        Owner owner = Owner.getInstance(user);
        Guest guest = Guest.getInstance(user);
        if (owner != null) {
            userValues.put(UserTable.ID, owner.getId());
            userValues.put(UserTable.NAME, owner.getName());
            userValues.put(UserTable.PHONE, owner.getPhone());
            userValues.put(UserTable.EMAIL, owner.getEmail());
            userValues.put(UserTable.IMAGE, ImageUtility.getBytes(owner.getImage()));
            //userValues.put(UserTable.PIN, owner.getPin());
            userValues.put(UserTable.ACCESS_MODE, "owner");

            if (isExist(owner.getId())) {
                isExist = true;
            }
        } else if (guest != null) {
            //Log.d("DBHandler", "Access End:"+guest.getAccessEndDateTime());
            userValues.put(UserTable.ID, guest.getId());
            userValues.put(UserTable.NAME, guest.getName());
            userValues.put(UserTable.PHONE, guest.getPhone());
            userValues.put(UserTable.EMAIL, guest.getEmail());
            userValues.put(UserTable.IMAGE, ImageUtility.getBytes(guest.getImage()));
            userValues.put(UserTable.ACCESS_MODE, "guest");
            userValues.put(UserTable.ACCESS_TYPE, guest.getAccessType());
            userValues.put(UserTable.START_ACCESS_DATETIME, guest.getAccessStartDateTime());
            userValues.put(UserTable.END_ACCESS_DATETIME, guest.getAccessEndDateTime());

            if (isExist(guest.getId())) {
                isExist = true;
            }
        }

        if (!isExist) {
            // Inserting Row
            rowUser = db.insert(UserTable.TABLE_NAME, null, userValues);
            Log.d(TAG, "Insert Guest:" + rowUser);
            return (rowUser != -1);
        } else {
            return true;
        }
    }

    /**
     * Register connected door to current user.
     * #user object must contain valid userId which can be set using
     * {Link User.setId} method. Also #registerDoor must contain
     * valid doorId which can be set by {Link Door.setId}.
     *
     * @param user           Connected user
     * @param registeredDoor Connected door
     * @return true is successfully registered
     */
    public boolean registerDoor(User user, Door registeredDoor) {
        Log.d(TAG, "DB/Registering User to Door:" + user.getId() + "->" + registeredDoor.getId());
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues resiteredDoorValues = new ContentValues();
        long rowRegisteredDoor;
        if (!this.isRegistered(user, registeredDoor)) {
            resiteredDoorValues.put(RegisteredDoorTable.USER_ID, user.getId());
            resiteredDoorValues.put(RegisteredDoorTable.DOOR_ID, registeredDoor.getId());
            resiteredDoorValues.put(RegisteredDoorTable.KEY_STATUS, registeredDoor.status);
            rowRegisteredDoor = db.insert(RegisteredDoorTable.TABLE_NAME, null, resiteredDoorValues);
        } else {
            return true;
        }
        Log.d(TAG, "Insert Door:" + rowRegisteredDoor);
        return (rowRegisteredDoor != -1);
    }

    /**
     * Insert door details.
     * doorId should not be null.
     * <p>
     * param door
     * return
     */
    public boolean insert(Door door) {
        Log.d(TAG, "DB/Inserting door:" + door);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DoorTable.ID, door.getId());
        values.put(DoorTable.NAME, door.getName());

        //long row = -1;
        if (!isExist(door)) {
            // Inserting Door details
            return (db.insert(DoorTable.TABLE_NAME, null, values) != -1);
        } else {
            // Updating Door details
            return update(door);
        }
    }

    // check if this door is already exist
    private boolean isExist(Door door) {
        boolean isExists = false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "
                + DoorTable.TABLE_NAME + " WHERE " + DoorTable.ID + " = '" + door.getId() + "'", null);
        if (cursor != null) {
            cursor.moveToFirst();
            isExists = cursor.getCount() > 0;
            cursor.close();
        }
        //db.close();
        return isExists;
    }

    // insert guest log
    public boolean insert(AccessLog log) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        long row =-1;
        Log.d("DatabaseHandler", "Inserting Log/Failure Reason:"+log.getFailureReason());
        values.put(LogTable.ACCESS_DATE_TIME, log.getAccessDateTime());
        values.put(LogTable.ACCESS_STATUS, log.getAccessStatus());
        values.put(LogTable.ACCESS_FAILURE_REASON, log.getFailureReason());
        values.put(LogTable.USER_ID, log.getUser().getId());
        values.put(LogTable.DOOR_ID, log.getDoor().getId());

        if (!isExist(log)) {
            // Inserting Row
            row = db.insert(LogTable.TABLE_NAME, null, values);
        Log.d(TAG, "insert:data "+row);
        } else Log.d("DatabaseHandler", "Inserting Log/Record already exists.");
        // Closing database connection
        db.close();
        return row != -1;
    }

   /* public Cursor get(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(query, null);
    }*/

    // get the details of an user by userId
    public User getUser(String userId) {
        User user = null;
        Owner owner;
        Guest guest;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " WHERE "
                + UserTable.ID + " = '" + userId + "'", null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() == 1) {
                String userType = cursor.getString(6);
                //Log.d("DatabaseHandler", "getUser/userType" + userType);
                if (userType.equalsIgnoreCase("owner")) {
                    owner = new Owner();
                    owner.setId(cursor.getString(0));
                    owner.setName(cursor.getString(1));
                    owner.setPhone(cursor.getString(2));
                    owner.setEmail(cursor.getString(3));
                    owner.setImage(ImageUtility.getImage(cursor.getBlob(4)));
                    owner.setPin(cursor.getString(5));
                    owner.setAccessMode(userType);
                    user = owner;
                } else if (userType.equalsIgnoreCase("guest")) {
                    guest = new Guest();
                    guest.setId(cursor.getString(0));
                    guest.setName(cursor.getString(1));
                    guest.setPhone(cursor.getString(2));
                    guest.setEmail(cursor.getString(3));
                    guest.setImage(ImageUtility.getImage(cursor.getBlob(4)));
                    guest.setAccessMode(userType);
                    guest.setAccessType(cursor.getString(7));
                    guest.setAccessStartDateTime(cursor.getString(8));
                    guest.setAccessEndDateTime(cursor.getString(9));
                    user = guest;
                }
            } else {
                return null;
            }
            cursor.close();
        }
        db.close();
        return user;
    }

    // get the details of door using doorId
    public Door getDoor(String doorId) {
        Door door = new Door();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DoorTable.TABLE_NAME + " WHERE " + DoorTable.ID + " = '" + doorId + "'", null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() == 1) {
                door.setId(cursor.getString(0));
                door.setName(cursor.getString(1));
            } else {
                return null;
            }
            cursor.close();
        }
        db.close();
        return door;
    }

    // For currently connected door, fetch all logs in descending order of datetime.
    public ArrayList<GuestLogActivity.GuestLog> getGuestLog(String doorID) {
        ArrayList<GuestLogActivity.GuestLog> guestLogs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        //Log.d("DatabaseHandler", "Loading Log");

        /*Cursor cursor1 = db.rawQuery("SELECT " + LogTable.USER_ID + " FROM " + LogTable.TABLE_NAME
                + " GROUP BY " + LogTable.USER_ID, null);

        int guestNo = 0;
        if (cursor1 != null) {
            cursor1.moveToFirst();
            if (cursor1.getCount() > 0) {
                String userId = cursor1.getString(0);
                Log.d("DatabaseHandler", "UserId: occ"+userId);
                if (!isExist(userId)) {
                    guestNo++;
                    values.put(UserTable.ID, userId);
                    values.put(UserTable.NAME, "Guest " + guestNo);
                    values.put(UserTable.IMAGE, ImageUtility.getBytes(
                            BitmapFactory.decodeResource(mContext.getResources(), R.mipmap.ic_user)));
                    values.put(UserTable.ACCESS_MODE, "guest");
                    db.insert(UserTable.TABLE_NAME, null, values);
                }
            }
            cursor1.close();
        }*/
        //Cursor cursor11 = db.rawQuery("SELECT " + LogTable.ACCESS_DATE_TIME + ", " + LogTable.ACCESS_STATUS + " FROM "+ LogTable.TABLE_NAME +" WHERE " + LogTable.USER_ID + " = '4048513C3636'",null);

        /*if(cursor11 != null){
            cursor11.moveToFirst();
            do {
                if (cursor11.getCount() > 0){
                    Log.d(TAG, "getGuestLog: DATA FOUND "+cursor11.getString(0)+ " "+cursor11.getString(1));
                }
            }while (cursor11.moveToNext());
        }else {
            Log.d(TAG, "getGuestLog: NO DATA FOUND 1");
        }*/
        Cursor cursor = db.rawQuery("SELECT " + LogTable.ID + ", " + UserTable.NAME + ", " + UserTable.IMAGE + ", "
                + LogTable.ACCESS_DATE_TIME + ", " + LogTable.ACCESS_STATUS + ", " + UserTable.ID + ", "
                + LogTable.ACCESS_FAILURE_REASON + " FROM " + UserTable.TABLE_NAME + ", " + LogTable.TABLE_NAME
                + " WHERE " + LogTable.DOOR_ID + " = '" + doorID + "' AND " + LogTable.TABLE_NAME + "."
                + LogTable.USER_ID + " = " + UserTable.TABLE_NAME + "." + UserTable.ID + " ORDER BY DATETIME("
                + LogTable.ACCESS_DATE_TIME + ") DESC", null);

        if (cursor != null) {
            cursor.moveToFirst();
            do {
                Log.d("DatabaseHandler", "getGuestLog/Logs:" + cursor.getCount());
                if (cursor.getCount() > 0) {
                    GuestLogActivity.GuestLog guestLog = new GuestLogActivity.GuestLog(cursor.getInt(0), cursor.getString(1),
                            ImageUtility.getImage(cursor.getBlob(2)), cursor.getString(3), cursor.getString(4), cursor.getString(6));
                    guestLog.setGuestId(cursor.getString(5));
                    guestLogs.add(guestLog);
                    //Log.d("DatabaseHandler", "getGuestLog/Access Details:" + guestLog);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return guestLogs;
    }

    // get logs in between specific dates.
    public ArrayList<GuestLogActivity.GuestLog> getLogByDates(String doorID, String fromDate, String toDate) {
        ArrayList<GuestLogActivity.GuestLog> guestLogs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        //Log.d("DatabaseHandler", "Loading Log");
        Cursor cursor = db.rawQuery("SELECT " + LogTable.ID + ", " + UserTable.NAME + ", " + UserTable.IMAGE + ", "
                + LogTable.ACCESS_DATE_TIME + ", " + LogTable.ACCESS_STATUS + ", " + LogTable.USER_ID + ", "
                + LogTable.ACCESS_FAILURE_REASON + " FROM " + UserTable.TABLE_NAME + ", " + LogTable.TABLE_NAME
                + " WHERE " + LogTable.DOOR_ID + " = '" + doorID + "' AND " + LogTable.TABLE_NAME + "." + LogTable.USER_ID
                + " = " + UserTable.TABLE_NAME + "." + UserTable.ID + " AND " + LogTable.ACCESS_DATE_TIME + " BETWEEN '"
                + fromDate + " 00:00' AND '" + toDate + " 23:59' " + " ORDER BY DATETIME(" + LogTable.ACCESS_DATE_TIME
                + ") DESC", null);

        if (cursor != null) {
            cursor.moveToFirst();
            do {
                //Log.d("DatabaseHandler", "No. of Logs:" + cursor.getCount());
                if (cursor.getCount() > 0) {
                    GuestLogActivity.GuestLog guestLog = new GuestLogActivity.GuestLog(cursor.getInt(0), cursor.getString(1),
                            ImageUtility.getImage(cursor.getBlob(2)), cursor.getString(3), cursor.getString(4), cursor.getString(6));
                    guestLog.setGuestId(cursor.getString(5));
                    guestLogs.add(guestLog);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return guestLogs;
    }

    // fetch log for selected guests
    public ArrayList<GuestLogActivity.GuestLog> getLogByGuests(ArrayList<Guest> guests, String doorID) {
        ArrayList<GuestLogActivity.GuestLog> guestLogs = new ArrayList<>();

        StringBuilder str = new StringBuilder();
        for (int k = 0; k < guests.size(); k++) {
            if (k != guests.size() - 1)
                str.append(guests.get(k).getId()).append("' OR ").append(LogTable.USER_ID).append(" = '");
            else
                str.append(guests.get(k).getId());
        }

        SQLiteDatabase db = this.getReadableDatabase();
        //Log.d("DatabaseHandler", "Loading Log");
        Cursor cursor = db.rawQuery("SELECT " + LogTable.ID + ", " + UserTable.NAME + ", " + UserTable.IMAGE + ", " + LogTable.ACCESS_DATE_TIME
                + ", " + LogTable.ACCESS_STATUS + ", " + LogTable.USER_ID + ", " + LogTable.ACCESS_FAILURE_REASON + " FROM " + UserTable.TABLE_NAME + ", " + LogTable.TABLE_NAME
                + " WHERE " + LogTable.DOOR_ID + " = '" + doorID + "' AND " + LogTable.TABLE_NAME + "." + LogTable.USER_ID + " = " + UserTable.TABLE_NAME + "."
                + UserTable.ID + " AND (" + LogTable.USER_ID + " = '" + str + "') ORDER BY DATETIME(" + LogTable.ACCESS_DATE_TIME + ") DESC", null);

        if (cursor != null) {
            cursor.moveToFirst();
            do {
                //Log.d("DatabaseHandler", "No. of Logs:" + cursor.getCount());
                if (cursor.getCount() > 0) {
                    GuestLogActivity.GuestLog guestLog = new GuestLogActivity.GuestLog(cursor.getInt(0), cursor.getString(1),
                            ImageUtility.getImage(cursor.getBlob(2)), cursor.getString(3), cursor.getString(4), cursor.getString(6));
                    guestLog.setGuestId(cursor.getString(5));
                    guestLogs.add(guestLog);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return guestLogs;
    }

    public void delete(GuestLogActivity.GuestLog guestLog) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from " + LogTable.TABLE_NAME + " where " + LogTable.ID + " = '" + guestLog.getLogId() + "'");
        int rowAffected;
        if (getKeyStatus(guestLog.getGuestId(), appContext.getDoor().getId()) != Door.KEY_SHARED) {
            SQLiteDatabase readableDb = getReadableDatabase();
            Cursor cursor = readableDb.rawQuery("SELECT * FROM " + LogTable.TABLE_NAME + " WHERE "
                    + LogTable.USER_ID + " = '" + guestLog.getGuestId() + "' AND " + LogTable.DOOR_ID + " = '"
                    + appContext.getDoor().getId() + "'", null);
            if (cursor != null) {
                cursor.moveToFirst();
                if (cursor.getCount() == 0) {
                    //Log.d("DatabaseHandler", "No log found. Deleting the guest");
                    rowAffected = db.delete(UserTable.TABLE_NAME, UserTable.ID + " = ?", new String[]{guestLog.getGuestId()});
                    Log.d(TAG, "Log Deleted <" + (rowAffected > 0) + ">");
                }
                cursor.close();
            }
            readableDb.close();
        }
    }

    public boolean delete(Guest guest, Door door) {
        Log.d(TAG, "AZLOCK: delete "+guest.getName());
        SQLiteDatabase db = this.getWritableDatabase();
        //db.execSQL("delete from " + RegisteredDoorTable.TABLE_NAME + " where " + RegisteredDoorTable.USER_ID + " = '" + guest.getId()
        //+ "' AND " + RegisteredDoorTable.DOOR_ID + " = '" + door.getId() + "'");
        int rows = db.delete(RegisteredDoorTable.TABLE_NAME, RegisteredDoorTable.USER_ID + " = ? AND "
                + RegisteredDoorTable.DOOR_ID + " = ?", new String[]{guest.getId(), door.getId()});

        /*Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + LogTable.TABLE_NAME + " WHERE "
                + LogTable.USER_ID + " = '" + guest.getId() + "' AND " + LogTable.DOOR_ID + " = '" + door.getId()
                + "'", null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() == 0) {
                Log.d("DatabaseHandler", "AZLOCK No log found. Deleting the guest");
                db.delete(UserTable.TABLE_NAME, UserTable.ID + " = ?", new String[]{guest.getId()});
            }
            Log.d(TAG, "AZLOCK GUEST NOT DELETE: ");
            cursor.close();
        }*/
        Log.d(TAG, "AZLOCK delete gust called: ");
        int deletedGuestKey = db.delete(UserTable.TABLE_NAME, UserTable.ID + " = ?", new String[]{guest.getId()});
        Log.d(TAG, "AZLOCK deletedGuestKey: "+deletedGuestKey);
        return rows > 0;
    }

    private boolean delete(String userID, String doorID) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(RegisteredDoorTable.TABLE_NAME, RegisteredDoorTable.USER_ID + " = ? AND "
                + RegisteredDoorTable.DOOR_ID + " = ?", new String[]{userID, doorID});

        return rows > 0;
    }

    public boolean isRegistered(User user, Door door) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT * FROM " + RegisteredDoorTable.TABLE_NAME + " WHERE " + RegisteredDoorTable.USER_ID + " = '"
                + user.getId() + "' AND " + RegisteredDoorTable.DOOR_ID + " = '" + door.getId() + "'", null);
        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }
        //db.close();
        return (count > 0);
    }

    private boolean isRegistered(String userID, String doorID) {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT * FROM " + RegisteredDoorTable.TABLE_NAME + " WHERE " + RegisteredDoorTable.USER_ID + " = '"
                + userID + "' AND " + RegisteredDoorTable.DOOR_ID + " = '" + doorID + "'", null);
        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }
        //db.close();
        return (count > 0);
    }

    public ArrayList<Guest> getGuests(String doorId, int viewCode) {
        ArrayList<Guest> guests = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Guest guest;
        Cursor cursor = null;
        switch (viewCode) {
            case Utils.SHOW_ALL_GUESTS:
                cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                        + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND u."
                        + UserTable.ID + " = " + "r." + RegisteredDoorTable.USER_ID + " ORDER BY " + UserTable.NAME, null);
                break;
            case Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED: /* Get Active and Expired guests */
                cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                        + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND r."
                        + RegisteredDoorTable.KEY_STATUS + " <> '" + Door.KEY_DELETED + "' AND u."
                        + UserTable.ID + " = " + "r." + RegisteredDoorTable.USER_ID + " ORDER BY " + UserTable.NAME, null);
                break;
            case Utils.SHOW_ACTIVE_GUESTS_ONLY: /* Get Active guests only */
                cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                        + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND r."
                        + RegisteredDoorTable.KEY_STATUS + " <> '" + Door.KEY_DELETED + "' AND r."
                        + RegisteredDoorTable.KEY_STATUS + " <> '" + Door.KEY_EXPIRED + "' AND u."
                        + UserTable.ID + " = " + "r." + RegisteredDoorTable.USER_ID + " ORDER BY " + UserTable.NAME, null);
                break;
            case Utils.SHOW_EXPIRED_GUESTS_ONLY:
                cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                        + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND r."
                        + RegisteredDoorTable.KEY_STATUS + " = '" + Door.KEY_EXPIRED + "' AND u."
                        + UserTable.ID + " = " + "r." + RegisteredDoorTable.USER_ID + " ORDER BY " + UserTable.NAME, null);
                break;
        }

        if (cursor != null) {
            cursor.moveToFirst();
            do {
                if (cursor.getCount() > 0) {
                    guest = new Guest(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                            cursor.getString(7), ImageUtility.getImage(cursor.getBlob(4)));
                    guest.setAccessStartDateTime(cursor.getString(8));
                    guest.setAccessEndDateTime(cursor.getString(9));
                    guests.add(guest);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return guests;
    }


    /*public ArrayList<Guest> getActiveGuestsByGroup(ArrayList<String> groups, String doorId) {
        ArrayList<Guest> guests = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        StringBuilder str = new StringBuilder();
        for (int k = 0; k < groups.size(); k++) {
            if (k != guests.size() - 1)
                str.append(groups.get(k)).append("' OR ").append(UserTable.ACCESS_TYPE).append(" = '");
            else
                str.append(groups.get(k));
        }

        Cursor cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." +
                RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND u." + UserTable.ID + " = " +
                "r." + RegisteredDoorTable.USER_ID + " AND r." + RegisteredDoorTable.KEY_STATUS + " <> '" + Door.KEY_EXPIRED
                + "' AND (" + UserTable.ACCESS_TYPE + " = '" + str + "')", null);
        if (cursor != null) {
            cursor.moveToFirst();
            do {
                if (cursor.getCount() > 0) {
                    Guest guest = new Guest(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                            cursor.getString(7), ImageUtility.getImage(cursor.getBlob(4)));
                    guest.setAccessStartDateTime(cursor.getString(8));
                    guest.setAccessEndDateTime(cursor.getString(9));
                    guests.add(guest);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        return guests;
    }*/

    public boolean setImage(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(UserTable.IMAGE, ImageUtility.getBytes(user.getImage()));
        int rowsAffected = db.update(UserTable.TABLE_NAME, cv, UserTable.ID + " = ?", new String[]{user.getId()});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }

    public Bitmap getImage(User user) {
        SQLiteDatabase db = this.getReadableDatabase();
        Bitmap image = null;
        Cursor cursor = db.rawQuery("SELECT " + UserTable.IMAGE + " FROM " + UserTable.TABLE_NAME + " WHERE "
                + UserTable.ID + " = '" + user.getId() + "'", null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() == 1) {
                image = ImageUtility.getImage(cursor.getBlob(0));
            }
            cursor.close();
        }
        db.close();
        return image;
    }

    private boolean isExist(AccessLog accessLog) {
        boolean isExists = false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + LogTable.ACCESS_DATE_TIME + ", " + LogTable.ACCESS_STATUS + " FROM "
                + LogTable.TABLE_NAME + " WHERE " + LogTable.USER_ID + " = '" + accessLog.getUser().getId() + "' AND "
                + LogTable.DOOR_ID + " = '" + accessLog.getDoor().getId() + "' AND " + LogTable.ACCESS_DATE_TIME + " = '"
                + accessLog.getAccessDateTime() + "' AND " + LogTable.ACCESS_STATUS + " = '" + accessLog.getAccessStatus() + "'", null);
        if (cursor != null) {
            cursor.moveToFirst();
            isExists = cursor.getCount() > 0;
            cursor.close();
        }
        //db.close();
        return isExists;
    }

    public boolean isLogExist(String doorId) {
        boolean isExists = false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + LogTable.TABLE_NAME + " WHERE " + LogTable.DOOR_ID + " = ?",
                new String[]{doorId});
        if (cursor != null) {
            cursor.moveToFirst();
            isExists = cursor.getCount() > 0;
            cursor.close();
        }
        //db.close();
        return isExists;
    }

    public void deleteAllLogs() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(LogTable.TABLE_NAME, "1", null);
    }

    public void deleteAllGuests() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(UserTable.TABLE_NAME, "1", null);
    }

    public boolean isExist(String userID) {
        boolean isExists = false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " WHERE " + UserTable.ID + " = '" + userID + "'", null);
        if (cursor != null) {
            cursor.moveToFirst();
            isExists = cursor.getCount() > 0;
            cursor.close();
        }
        //db.close();
        return isExists;
    }

    public boolean update(Guest guest) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues userValues = new ContentValues();
        userValues.put(UserTable.NAME, guest.getName());
        userValues.put(UserTable.PHONE, guest.getPhone());
        userValues.put(UserTable.EMAIL, guest.getEmail());
        //userValues.put(UserTable.IMAGE, ImageUtility.getBytes(guest.getImage())); //stoped bichi due to impage quality loss
        userValues.put(UserTable.ACCESS_MODE, "guest");
        userValues.put(UserTable.ACCESS_TYPE, guest.getAccessType());
        userValues.put(UserTable.START_ACCESS_DATETIME, guest.getAccessStartDateTime());
        userValues.put(UserTable.END_ACCESS_DATETIME, guest.getAccessEndDateTime());
        int rowsAffected = db.update(UserTable.TABLE_NAME, userValues, UserTable.ID + " = ?", new String[]{guest.getId()});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }

    /*public boolean update(Owner owner) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues userValues = new ContentValues();
        userValues.put(UserTable.NAME, owner.getName());
        userValues.put(UserTable.PHONE, owner.getPhone());
        userValues.put(UserTable.EMAIL, owner.getEmail());
        userValues.put(UserTable.IMAGE, ImageUtility.getBytes(owner.getImage()));
        userValues.put(UserTable.ACCESS_MODE, "owner");
        int rowsAffected = db.update(UserTable.TABLE_NAME, userValues, UserTable.ID + " = ?", new String[]{owner.getId()});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }*/

    public boolean update(Door door) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DoorTable.NAME, door.getName());
        int rowsAffected = db.update(DoorTable.TABLE_NAME, values, DoorTable.ID + " = ?", new String[]{door.getId()});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }

    /*public boolean insertRouterInfo(String doorId, String routerAddress, int routerPort) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DoorTable.ROUTER_ADDRESS, routerAddress);
        values.put(DoorTable.ROUTER_PORT, routerPort);
        int rowsAffected = db.update(DoorTable.TABLE_NAME, values, DoorTable.ID + " = ?", new String[]{doorId});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }*/

    /*public boolean setRouterConfiguration(String doorId, String routerAddress, int routerPort, String deviceIp, String subnet, String gateway){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DoorTable.ROUTER_ADDRESS, routerAddress);
        values.put(DoorTable.ROUTER_PORT, routerPort);
        values.put(DoorTable.DOOR_IP, deviceIp);
        values.put(DoorTable.SUBNET_MASK, subnet);
        values.put(DoorTable.DEFAULT_GATEWAY, gateway);
        int rowsAffected = db.update(DoorTable.TABLE_NAME, values, DoorTable.ID + " = ?", new String[]{doorId});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }*/

    /*public RouterInfo getRouterInfo(String doorId) {
        RouterInfo routerInfo = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DoorTable.TABLE_NAME + " WHERE " + DoorTable.ID + " = '" + doorId + "'", null);
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() == 1) {
                routerInfo = new RouterInfo(cursor.getString(2), cursor.getInt(3));
            } else {
                return null;
            }
            cursor.close();
        }
        db.close();
        return routerInfo;
    }*/

    /*public ArrayList<Door> getRouterConfiguredDoors() {
        ArrayList<Door> doors = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DoorTable.TABLE_NAME + " WHERE " + DoorTable.ROUTER_ADDRESS + " IS NOT NULL", null);
        if (cursor != null) {
            cursor.moveToFirst();
            do {
                if (cursor.getCount() > 0) {
                    Door door = new Door(cursor.getString(0), cursor.getString(1));
                    door.setRouterInfo(new RouterInfo(cursor.getString(2), cursor.getInt(3)));
                    doors.add(door);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return doors;
    }*/

    /*
     * updateKeyStatus will calculate current time with access end time and update
     * key status according to it. If parameters are null then the method will
     * update key status for all guests.
     */
    public void updateKeyStatus(String guestId, String doorId) {
        Guest guest;
        Door door;

        if (guestId == null && doorId == null) {
            //Log.d("DatabaseHandler", "updateKeyStatus/Updating all guests [guestId=" + guestId + ", doorId=" + doorId + "]");
            boolean flag = false;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + UserTable.ID + ", " + UserTable.NAME + ", " + UserTable.ACCESS_TYPE + ", " + UserTable.END_ACCESS_DATETIME + ", "
                    + RegisteredDoorTable.DOOR_ID + ", " + RegisteredDoorTable.KEY_STATUS + " FROM " + UserTable.TABLE_NAME + " u, "
                    + RegisteredDoorTable.TABLE_NAME + " r WHERE u." + UserTable.ID + " = r." + RegisteredDoorTable.USER_ID + " AND "
                    + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.KEY_STATUS + " <> " + Door.KEY_DELETED, null);
            if (cursor != null) {
                cursor.moveToFirst();
                do {
                    if (cursor.getCount() > 0) {
                        guest = new Guest();
                        door = new Door();
                        guest.setId(cursor.getString(0));
                        guest.setName(cursor.getString(1));
                        guest.setAccessType(cursor.getString(2));
                        guest.setAccessEndDateTime(cursor.getString(3));
                        door.setId(cursor.getString(4));
                        door.status = cursor.getInt(5);
                        //Log.d("DatabaseHandler", "updateKeyStatus/status:" + door.status);
                        try {
                            //Log.d("DatabaseHandler", "updateKeyStatus/Updating Key Status:" + guest);
                            if (guest.getAccessType().equalsIgnoreCase("Limited Time")) {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                                Date accessEndDateTime = dateFormat.parse(guest.getAccessEndDateTime());
                                Date currentDateTime = dateFormat.parse(DateTimeFormat.getDateTime(2));

                                if (accessEndDateTime != null) {
                                    if (accessEndDateTime.compareTo(currentDateTime) < 0) {
                                        door.status = Door.KEY_EXPIRED;
                                        flag = true;
                                        //Log.d("DatabaseHandler", "updateKeyStatus/updating status to KEY_EXPIRED");
                                    } else {
                                        door.status = Door.KEY_SHARED;
                                        flag = true;
                                        //Log.d("DatabaseHandler", "updateKeyStatus/updating status to KEY_SHARED");
                                    }
                                }
                            } else if (guest.getAccessType().equalsIgnoreCase("Full Time")) {
                                door.status = Door.KEY_SHARED;
                                flag = true;
                                //Log.d("DatabaseHandler", "updateKeyStatus/updating status to KEY_SHARED");
                            }
                            if (flag) {
                                ContentValues values = new ContentValues();
                                values.put(RegisteredDoorTable.KEY_STATUS, door.status);
                                this.getWritableDatabase().update(RegisteredDoorTable.TABLE_NAME, values,
                                        RegisteredDoorTable.USER_ID + " = ? AND " + RegisteredDoorTable.DOOR_ID + " = ?",
                                        new String[]{guest.getId(), door.getId()});

                            }

                        } catch (ParseException e) {
                            Log.d("DatabaseHandler", "updateKeyStatus/" + e.getClass() + " Message:"
                                    + e.getMessage() + " Cause:" + e.getCause());
                        }catch (Exception e){
                            e.printStackTrace();
                        }

                    }
                } while (cursor.moveToNext());
                cursor.close();

            }
        } else if (guestId != null && doorId != null) {
            Log.d("DatabaseHandler", "updateKeyStatus/Updating selected guest [guestId=" + guestId + ", doorId=" + doorId + "]");
            boolean flag = false;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + UserTable.ID + ", " + UserTable.NAME + ", " + UserTable.ACCESS_TYPE + ", "
                    + UserTable.END_ACCESS_DATETIME + ", " + RegisteredDoorTable.DOOR_ID + ", " + RegisteredDoorTable.KEY_STATUS
                    + " FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE u." + UserTable.ID
                    + " = r." + RegisteredDoorTable.USER_ID + " AND " + UserTable.ACCESS_MODE + " = 'guest' AND r."
                    + RegisteredDoorTable.USER_ID + " = '" + guestId + "' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId
                    + "'", null);
            if (cursor != null) {
                cursor.moveToFirst();
                if (cursor.getCount() > 0) {
                    guest = new Guest();
                    door = new Door();
                    guest.setId(cursor.getString(0));
                    guest.setName(cursor.getString(1));
                    guest.setAccessType(cursor.getString(2));
                    guest.setAccessEndDateTime(cursor.getString(3));
                    door.setId(cursor.getString(4));
                    door.status = cursor.getInt(5);
                    Log.d("DatabaseHandler", "updateKeyStatus/status:" + door.status);
                    try {
                        Log.d("DatabaseHandler", "updateKeyStatus/Updating Key Status:" + guest);
                        if (guest.getAccessType().equalsIgnoreCase("Limited Time")) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
                            Date accessEndDateTime = dateFormat.parse(guest.getAccessEndDateTime());
                            Date currentDateTime = dateFormat.parse(DateTimeFormat.getDateTime(2));

                            if (accessEndDateTime != null) {
                                if (accessEndDateTime.compareTo(currentDateTime) < 0) {
                                    door.status = Door.KEY_EXPIRED;
                                    flag = true;
                                    Log.d("DatabaseHandler", "updateKeyStatus/updating status to KEY_EXPIRED");
                                } else {
                                    door.status = Door.KEY_SHARED;
                                    flag = true;
                                    Log.d("DatabaseHandler", "updateKeyStatus/updating status to KEY_SHARED");
                                }
                            }
                        } else if (guest.getAccessType().equalsIgnoreCase("Full Time")) {
                            door.status = Door.KEY_SHARED;
                            flag = true;
                            Log.d("DatabaseHandler", "updateKeyStatus/updating status to KEY_SHARED");
                        }
                        if (flag) {
                            ContentValues values = new ContentValues();
                            values.put(RegisteredDoorTable.KEY_STATUS, door.status);
                            this.getWritableDatabase().update(RegisteredDoorTable.TABLE_NAME, values,
                                    RegisteredDoorTable.USER_ID + " = ? AND " + RegisteredDoorTable.DOOR_ID + " =?",
                                    new String[]{guest.getId(), door.getId()});

                        }
                        cursor.close();
                    } catch (ParseException e) {
                        Log.d("DatabaseHandler", "updateKeyStatus/" + e.getClass() + " Message:"
                                + e.getMessage() + " Cause:" + e.getCause());
                    }

                }
            }

        }
    }

    public void setKeyShared(ArrayList<Guest> guests) {
        ContentValues values = new ContentValues();
        values.put(RegisteredDoorTable.KEY_STATUS, Door.KEY_SHARED);
        for (Guest g : guests) {
            this.getWritableDatabase().update(RegisteredDoorTable.TABLE_NAME, values,
                    RegisteredDoorTable.USER_ID + " = ? AND " + RegisteredDoorTable.DOOR_ID + " = ?",
                    new String[]{g.getId(), appContext.getDoor().getId()});
        }
    }

    public void deleteKeys(ArrayList<Guest> guests) {
        SQLiteDatabase db = this.getWritableDatabase();
        boolean isLogExist = false;
        boolean isRegisteredForMultipleDoors = false;
        Log.w("DatabaseHandler", "Deleting Keys:" + guests + " [Door=" + appContext.getDoor().getId() + "]");
        for (Guest guest : guests) {
            Cursor cursor1 = this.getReadableDatabase().rawQuery("SELECT * FROM " + LogTable.TABLE_NAME + " WHERE "
                    + LogTable.USER_ID + " = '" + guest.getId() + "'", null);
            if (cursor1 != null) {
                cursor1.moveToFirst();
                if (cursor1.getCount() > 0) {
                    isLogExist = true;
                }
                cursor1.close();
            }
            Cursor cursor2 = this.getReadableDatabase().rawQuery("SELECT " + UserTable.ID + ", " + RegisteredDoorTable.KEY_STATUS + ", "
                    + RegisteredDoorTable.DOOR_ID + " FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME
                    + " r WHERE " + RegisteredDoorTable.USER_ID + " = '" + guest.getId() + "' AND u." + UserTable.ID + " = r."
                    + RegisteredDoorTable.USER_ID, null);
            if (cursor2 != null) {
                cursor2.moveToFirst();
                if (cursor2.getCount() > 1) {
                    isRegisteredForMultipleDoors = true;
                }
                cursor2.close();
            }

            if (isLogExist || isRegisteredForMultipleDoors) {
                ContentValues values = new ContentValues();
                values.put(RegisteredDoorTable.KEY_STATUS, Door.KEY_DELETED);
                int rows = db.update(RegisteredDoorTable.TABLE_NAME, values, RegisteredDoorTable.USER_ID
                        + " = ? AND " + RegisteredDoorTable.DOOR_ID + " = ?", new String[]{guest.getId(),
                        appContext.getDoor().getId()});
                Log.w("DatabaseHandler", "deleteKeys/updated:" + (rows > 0));
            } else {
                if (new DatabaseHandler(mContext).isRegistered(guest.getId(), appContext.getDoor().getId())) {
                    if (new DatabaseHandler(mContext).delete(guest.getId(), appContext.getDoor().getId())) {
                        Log.w("CustomAdapter", "deleteKeys/Deleting from RegisteredDoorTable/" + guest.getName());
                    }
                }
                int rows = db.delete(UserTable.TABLE_NAME, UserTable.ID + " = ? ", new String[]{guest.getId()});
                Log.w("DatabaseHandler", "deleteKeys/deleted:" + (rows > 0));
            }
        }

    }

    public int getKeyStatus(String userId, String doorId) {
        SQLiteDatabase db = this.getReadableDatabase();
        int status = Door.KEY_UNKNOWN;
        Cursor cursor = db.rawQuery("SELECT " + RegisteredDoorTable.KEY_STATUS + " FROM " + RegisteredDoorTable.TABLE_NAME
                + " WHERE " + RegisteredDoorTable.USER_ID + " = ? AND " + RegisteredDoorTable.DOOR_ID + " = ?", new String[]{userId, doorId});
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                status = cursor.getInt(0);
            }
            cursor.close();
        }
        //Log.d("DatabaseHandler", "getKeyStatus/keyStatus:" + status);
        db.close();
        return status;
    }

    public boolean addNetwork(WifiNetwork wifiNetwork) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(WifiConfigTable.SSID, wifiNetwork.getSSID());
        values.put(WifiConfigTable.SECURITY, wifiNetwork.getSecurity());
        values.put(WifiConfigTable.PASSWORD, wifiNetwork.getPassword());
        values.put(WifiConfigTable.AUTO_CONNECT, wifiNetwork.getAutoConnect());

        long row = db.insert(WifiConfigTable.TABLE_NAME, null, values);

        Log.d(TAG, "inserted ssid to db");

        return (row != -1);
    }

    public ArrayList<WifiNetwork> getNetworks() {
        ArrayList<WifiNetwork> networks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        WifiNetwork wifiNetwork;
        Cursor cursor = db.rawQuery("SELECT * FROM " + WifiConfigTable.TABLE_NAME, null);
        if (cursor != null) {
            cursor.moveToFirst();
            do {
                if (cursor.getCount() > 0) {
                    wifiNetwork = new WifiNetwork(cursor.getString(1), cursor.getInt(2));
                    wifiNetwork.setPassword(cursor.getString(3));
                    wifiNetwork.setAutoConnect(cursor.getInt(4));
                    networks.add(wifiNetwork);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return networks;
    }

    /*public WifiNetwork getNetwork(String SSID) {
        SQLiteDatabase db = this.getReadableDatabase();
        WifiNetwork wifiNetwork = null;
        Cursor cursor = db.rawQuery("SELECT * FROM " + WifiConfigTable.TABLE_NAME + " WHERE "
                + WifiConfigTable.SSID + " = ?", new String[]{SSID});
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.getCount() > 0) {
                wifiNetwork = new WifiNetwork(cursor.getString(1), cursor.getInt(2));
                wifiNetwork.setPassword(cursor.getString(3));
                wifiNetwork.setAutoConnect(cursor.getInt(4));
            }
            cursor.close();
        }
        db.close();
        return wifiNetwork;
    }*/

    public boolean update(WifiNetwork wifiNetwork, String oldSSID, int updateFlag) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        switch (updateFlag) {
            case WifiNetwork.UPDATE_SSID:
                Log.d(TAG, "UPDATE_SSID [old = " + oldSSID + ", new = " + wifiNetwork.getSSID() + "]");
                values.put(WifiConfigTable.SSID, wifiNetwork.getSSID());
                break;
            case WifiNetwork.UPDATE_SECURITY:
                values.put(WifiConfigTable.SECURITY, wifiNetwork.getSecurity());
                break;
            case WifiNetwork.UPDATE_PASSWORD:
                values.put(WifiConfigTable.PASSWORD, wifiNetwork.getPassword());
                break;
            case WifiNetwork.UPDATE_AUTO_CONNECT:
                values.put(WifiConfigTable.AUTO_CONNECT, wifiNetwork.getAutoConnect());
                break;
            case WifiNetwork.UPDATE_ALL:
                values.put(WifiConfigTable.AUTO_CONNECT, wifiNetwork.getAutoConnect());
                values.put(WifiConfigTable.PASSWORD, wifiNetwork.getPassword());
                values.put(WifiConfigTable.SECURITY, wifiNetwork.getSecurity());
                values.put(WifiConfigTable.SSID, wifiNetwork.getSSID());
                break;
        }
        int rowsAffected = db.update(WifiConfigTable.TABLE_NAME, values, WifiConfigTable.SSID + " = ?", new String[]{oldSSID});
        Log.d(TAG, "rowsAffected:" + rowsAffected);
        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }

    public void deleteNetworks(ArrayList<WifiNetwork> wifiNetworks) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (WifiNetwork network : wifiNetworks) {
            db.delete(WifiConfigTable.TABLE_NAME, WifiConfigTable.SSID + " = ?", new String[]{network.getSSID()});
        }
    }

    /*public void deleteNetwork(WifiNetwork network) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(WifiConfigTable.TABLE_NAME, WifiConfigTable.SSID + " = ?", new String[]{network.getSSID()});
    }*/

    public long insertBridgeData(BridgeDetail data) {
        SQLiteDatabase write = this.getWritableDatabase();
        ContentValues userValues = new ContentValues();
        userValues.put(BridgeTable.BRIDGE_ID, data.getBridgeId());
        userValues.put(BridgeTable.PASSWORD, data.getPassword());
        try {
            long ii = write.insertOrThrow(BridgeTable.TABLE_NAME, null, userValues);
            Log.d(TAG, "write data=" + ii);
            return ii;
        } catch (SQLiteConstraintException e) {
            long maxRowEffect = write.update(BridgeTable.TABLE_NAME, userValues, BridgeTable.BRIDGE_ID + " = ?", new String[]{data.getBridgeId()});
            Log.d(TAG, "insertBridgeData: "+maxRowEffect);
            long var = Integer.MAX_VALUE -maxRowEffect;
            Log.d(TAG, "insertBridgeData:var "+var);
            return var+maxRowEffect;
        }
    }

    public List<BridgeDetail> getBridgeData(int whichCall) {
        List<BridgeDetail> bridgeDetailList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        BridgeDetail bridgeDetail;
        Cursor cursor = db.rawQuery("SELECT * FROM " + BridgeTable.TABLE_NAME, null);
        if (cursor != null) {
            cursor.moveToFirst();
            /*
             * this whichCall is only for check weather it contain one bridge id or more.
             * because if user have only one bridge id then direct goto RemoteConnectActivity by passing
             * bridge id and password , if he has more then one bride id then show all bridge id
             * allow user to choose which bridge id he/she want to connect with mqtt broker.
             * */
            Log.d(TAG, "getBridgeData: " + cursor.getCount());
            if (Utils.BRIDGE_CHECK == whichCall) {
                if (cursor.getCount() == 1) {
                    bridgeDetail = new BridgeDetail(cursor.getString(0), cursor.getString(1));
                    bridgeDetailList.add(bridgeDetail);
                }
            } else if (cursor.getCount() > 0) {
                do {
                    bridgeDetail = new BridgeDetail(cursor.getString(0), cursor.getString(1));
                    bridgeDetailList.add(bridgeDetail);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        db.close();
        return bridgeDetailList;
    }

    public void deleteBrideData(BridgeDetail data) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(BridgeTable.TABLE_NAME, BridgeTable.BRIDGE_ID + "=?", new String[]{data.getBridgeId()});
    }

    /*public boolean isBrdigeAContain(String bridgeId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT (*) FROM " + BridgeTable.TABLE_NAME + " WHERE " + BridgeTable.BRIDGE_ID + "=?", new String[]{String.valueOf(bridgeId)});
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        Log.d(TAG, "isBrdigeAContain: " + count);
        return count > 0;
    }*/

    public void updatePassword(String id, String password) {
        Log.d(TAG, "updatePassword: " + id + "  " + password);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(BridgeTable.PASSWORD, password);
        long d = db.update(BridgeTable.TABLE_NAME, values, BridgeTable.BRIDGE_ID + " = ?", new String[]{id});
        Log.d(TAG, "updatePassword: " + d);
    }
}
