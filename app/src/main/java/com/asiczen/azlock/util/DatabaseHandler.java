package com.asiczen.azlock.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.asiczen.azlock.app.model.AccessLog;
import com.asiczen.azlock.app.model.Door;
import com.asiczen.azlock.app.model.Guest;
import com.asiczen.azlock.app.model.User;
import com.asiczen.azlock.app.model.WifiNetwork;
import com.asiczen.azlock.content.AppContext;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/*
 * Created by user on 8/13/2015.
 */
public final class DatabaseHandler extends SQLiteOpenHelper implements DatabaseUtility {

    private final Context mContext;
    private final AppContext appContext;
    private final String TAG = DatabaseHandler.class.getSimpleName();
    public DatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, UTIL_DATABASE_VERSION);
        this.mContext = context;
        appContext=AppContext.getContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        /*Log.d("DatabaseHandler", "Dropping Tables");
        db.execSQL("DROP TABLE IF EXISTS " + UserTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RegisteredDoorTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DoorTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + LogTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + WifiConfigTable.TABLE_NAME);*/

        //Log.d("DatabaseHandler", "Creating Tables");
        String CREATE_USER_TABLE = "CREATE TABLE "+ UserTable.TABLE_NAME+"("+ UserTable.ID+" TEXT PRIMARY KEY, "+ UserTable.NAME+" TEXT, "
                + UserTable.PHONE+" TEXT, "+ UserTable.EMAIL+" TEXT, "+ UserTable.IMAGE+" BLOB, "+ UserTable.PIN+" TEXT, "
                + UserTable.ACCESS_MODE+" TEXT, "+ UserTable.ACCESS_TYPE+" TEXT, "+ UserTable.START_ACCESS_DATETIME+" TEXT, "
                + UserTable.END_ACCESS_DATETIME+" TEXT)";
        String CREATE_REGISTERED_DOOR_TABLE = "CREATE TABLE "+ RegisteredDoorTable.TABLE_NAME+"("+ RegisteredDoorTable.ID+" INTEGER PRIMARY KEY, "
                + RegisteredDoorTable.USER_ID+" TEXT, "+ RegisteredDoorTable.DOOR_ID+" TEXT, "+RegisteredDoorTable.KEY_STATUS+" INTEGER)";
        String CREATE_DOOR_TABLE = "CREATE TABLE "+ DoorTable.TABLE_NAME+"("+ DoorTable.ID+" TEXT PRIMARY KEY, "
                + DoorTable.NAME+" TEXT, "+DoorTable.ROUTER_ADDRESS +" TEXT, "+DoorTable.ROUTER_PORT+" INTEGER, "
                +DoorTable.DOOR_IP +" TEXT, "+DoorTable.SUBNET_MASK +" TEXT, "+DoorTable.DEFAULT_GATEWAY +" TEXT)";
        String CREATE_LOG_TABLE = "CREATE TABLE "+ LogTable.TABLE_NAME+"("+ LogTable.ID+" INTEGER PRIMARY KEY, "
                + LogTable.ACCESS_DATE_TIME+" DATETIME, "+ LogTable.ACCESS_STATUS+" TEXT, "+ LogTable.USER_ID
                +" TEXT, "+ LogTable.DOOR_ID+" TEXT, "+LogTable.ACCESS_FAILURE_REASON+" TEXT)";
        String CREATE_WIFI_NETWORK_TABLE = "CREATE TABLE "+ WifiConfigTable.TABLE_NAME+"("+ WifiConfigTable.ID+" INTEGER PRIMARY KEY, "
                + WifiConfigTable.SSID +" TEXT, "+ WifiConfigTable.SECURITY +" TEXT, "+ WifiConfigTable.PASSWORD +" TEXT, "
                + WifiConfigTable.AUTO_CONNECT+" INTEGER)";
        db.execSQL(CREATE_USER_TABLE);
        db.execSQL(CREATE_REGISTERED_DOOR_TABLE);
        db.execSQL(CREATE_DOOR_TABLE);
        db.execSQL(CREATE_LOG_TABLE);
        db.execSQL(CREATE_WIFI_NETWORK_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

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

        // Create tables again
        onCreate(db);
    }

    /**
     * Insert new user (owner or guest) using {Link User} object.
     * #user object must contain valid userId which can be set using
     * {Link User.setId} method.
     *
     * param user
     * return {Link true} if user is successfully registered
     */
    /*public boolean insert(User user)
    {
        Log.d(TAG, "DB/Inserting User:" + user);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues userValues = new ContentValues();
        long rowUser;
        boolean isExist = false;
        Owner owner = Owner.getInstance(user);
        Guest guest = Guest.getInstance(user);
        if(owner != null) {
            userValues.put(UserTable.ID, owner.getId());
            userValues.put(UserTable.NAME, owner.getName());
            userValues.put(UserTable.PHONE, owner.getPhone());
            userValues.put(UserTable.EMAIL, owner.getEmail());
            userValues.put(UserTable.IMAGE, ImageUtility.getBytes(owner.getImage()));
            //userValues.put(UserTable.PIN, owner.getPin());
            userValues.put(UserTable.ACCESS_MODE, "owner");

            if(isExist(owner.getId()))
            {
                isExist = true;
            }
        }
        else if(guest != null)
        {
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

            if(isExist(guest.getId()))
            {
                isExist = true;
            }
        }

        if(!isExist) {
            // Inserting Row
            rowUser = db.insert(UserTable.TABLE_NAME, null, userValues);
            Log.d(TAG, "Insert Guest:" + rowUser);
            return (rowUser != -1);
        }
        else {
            return true;
        }
    }*/

    /**
     * Register connected door to current user.
     * #user object must contain valid userId which can be set using
     * {\Link User.setId} method. Also #registerDoor must contain
     * valid doorId which can be set by {Link Door.setId}.
     *
     * param user Connected user
     * param registeredDoor Connected door
     * return true is successfully registered
     */
    /*public boolean registerDoor(User user, Door registeredDoor)
    {
        Log.d(TAG, "DB/Registering User to Door:" + user.getId()+"->"+registeredDoor.getId());
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues resiteredDoorValues = new ContentValues();
        long rowRegisteredDoor;
        if(!this.isRegistered(user, registeredDoor)) {
            resiteredDoorValues.put(RegisteredDoorTable.USER_ID, user.getId());
            resiteredDoorValues.put(RegisteredDoorTable.DOOR_ID, registeredDoor.getId());
            resiteredDoorValues.put(RegisteredDoorTable.KEY_STATUS, registeredDoor.status);
            rowRegisteredDoor = db.insert(RegisteredDoorTable.TABLE_NAME, null, resiteredDoorValues);
        } else {
            return true;
        }
        Log.d(TAG, "Insert Door:" +rowRegisteredDoor);
        return (rowRegisteredDoor != -1);
    }*/

    /**
     * Insert door details.
     * doorId should not be null.
     *
     * param door
     * return
     */
    /*public boolean insert(Door door)
    {
        Log.d(TAG, "DB/Inserting door:" + door);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(DoorTable.ID, door.getId());
        values.put(DoorTable.NAME, door.getName());

        if(!isExist(door)) {
            // Inserting Door details
            return (db.insert(DoorTable.TABLE_NAME, null, values) != -1);
        }
        else
        {
            // Updating Door details
            return update(door);
        }
    }*/

    // check if this door is already exist
    private boolean isExist(Door door)
    {
        boolean isExists = false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "
                + DoorTable.TABLE_NAME + " WHERE " + DoorTable.ID + " = '" + door.getId() + "'", null);
        if(cursor != null)
        {
            cursor.moveToFirst();
            isExists = cursor.getCount() > 0;
            cursor.close();
        }
        //db.close();
        return isExists;
    }

    // insert guest log
    /*public boolean insert(AccessLog log)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        long row = -1;
        //Log.d("DatabaseHandler", "Inserting Log/Failure Reason:"+log.getFailureReason());
        values.put(LogTable.ACCESS_DATE_TIME, log.getAccessDateTime());
        values.put(LogTable.ACCESS_STATUS, log.getAccessStatus());
        values.put(LogTable.ACCESS_FAILURE_REASON, log.getFailureReason());
        values.put(LogTable.USER_ID, log.getUser().getId());
        values.put(LogTable.DOOR_ID, log.getDoor().getId());

        if(!isExist(log)) {
            // Inserting Row
            row = db.insert(LogTable.TABLE_NAME, null, values);
        }
        else Log.d("DatabaseHandler", "Inserting Log/Record already exists.");
         // Closing database connection
        return row != -1;
    }*/

    /*public Cursor get(String query) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery(query, null);
    }*/

    // get the details of an user by userId
    /*public User getUser(String userId)
    {
        User user = null;
        Owner owner;
        Guest guest;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " WHERE "
                + UserTable.ID + " = '" + userId + "'", null);
        if(cursor != null)
        {
            cursor.moveToFirst();
            if(cursor.getCount() == 1)
            {
                String userType = cursor.getString(6);
                //Log.d("DatabaseHandler", "getUser/userType" + userType);
                if(userType.equalsIgnoreCase("owner"))
                {
                    owner = new Owner();
                    owner.setId(cursor.getString(0));
                    owner.setName(cursor.getString(1));
                    owner.setPhone(cursor.getString(2));
                    owner.setEmail(cursor.getString(3));
                    owner.setImage(ImageUtility.getImage(cursor.getBlob(4)));
                    owner.setPin(cursor.getString(5));
                    owner.setAccessMode(userType);
                    user = owner;
                }
                else if(userType.equalsIgnoreCase("guest"))
                {
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
    }*/

    // get the details of door using doorId
    /*public Door getDoor(String doorId)
    {
        Door door = new Door();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+ DoorTable.TABLE_NAME+" WHERE "+ DoorTable.ID +" = '"+doorId+"'", null);
        if(cursor != null)
        {
            cursor.moveToFirst();
            if(cursor.getCount() == 1)
            {
                door.setId(cursor.getString(0));
                door.setName(cursor.getString(1));
            } else {
                return null;
            }
            cursor.close();
        }
        db.close();
        return door;
    }*/

    // For currently connected door, fetch all logs in descending order of datetime.
    /*public ArrayList<GuestLogActivity.GuestLog> getGuestLog(String doorID)
    {
        ArrayList<GuestLogActivity.GuestLog> guestLogs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues values = new ContentValues();
        //Log.d("DatabaseHandler", "Loading Log");

        Cursor cursor1 = db.rawQuery("SELECT " + LogTable.USER_ID + " FROM " + LogTable.TABLE_NAME
                + " GROUP BY " + LogTable.USER_ID, null);

        int guestNo = 0;
        if(cursor1 != null)
        {
            cursor1.moveToFirst();
            if(cursor1.getCount()>0) {
                String userId = cursor1.getString(0);
                //Log.d("DatabaseHandler", "UserId:"+userId+" > "+isExist(userId));
                if(!isExist(userId))
                {
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
        }
        Cursor cursor = db.rawQuery("SELECT " + LogTable.ID + ", " + UserTable.NAME + ", " + UserTable.IMAGE + ", "
                + LogTable.ACCESS_DATE_TIME + ", " + LogTable.ACCESS_STATUS + ", " + UserTable.ID + ", "
                + LogTable.ACCESS_FAILURE_REASON + " FROM " + UserTable.TABLE_NAME + ", " + LogTable.TABLE_NAME
                + " WHERE " + LogTable.DOOR_ID + " = '" + doorID + "' AND " + LogTable.TABLE_NAME + "."
                + LogTable.USER_ID + " = " + UserTable.TABLE_NAME + "." + UserTable.ID + " ORDER BY DATETIME("
                + LogTable.ACCESS_DATE_TIME + ") DESC", null);

        if (cursor != null) {
            cursor.moveToFirst();
            do {
                //Log.d("DatabaseHandler", "getGuestLog/Logs:" + cursor.getCount());
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
    }*/

    /*public ArrayList<AccessLog> getLog(String guestID, String doorID)
    {
        ArrayList<AccessLog> guestLogs = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + LogTable.ACCESS_DATE_TIME + ", " + LogTable.ACCESS_STATUS + ", " + LogTable.ACCESS_FAILURE_REASON + " FROM "
                + LogTable.TABLE_NAME + " WHERE " + LogTable.USER_ID + " = '" + guestID + "' AND " + LogTable.DOOR_ID + " = '" + doorID + "'", null);
        if(cursor != null)
        {
            cursor.moveToFirst();
            do{
                if(cursor.getCount()>0) {
                    guestLogs.add(new AccessLog(guestID, cursor.getString(0), cursor.getString(1), cursor.getString(2), doorID));
                }
            }while(cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return guestLogs;
    }*/

    /*public void delete(GuestLogActivity.GuestLog guestLog)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ LogTable.TABLE_NAME+" where "+ LogTable.ID+" = '"+guestLog.getLogId()+"'");
        int rowAffected;
        if(getKeyStatus(guestLog.getGuestId(), appContext.getDoor().getId()) != Door.KEY_SHARED) {
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
    }*/

   /* public boolean delete(Guest guest, Door door)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        //db.execSQL("delete from " + RegisteredDoorTable.TABLE_NAME + " where " + RegisteredDoorTable.USER_ID + " = '" + guest.getId()
                //+ "' AND " + RegisteredDoorTable.DOOR_ID + " = '" + door.getId() + "'");
        int rows = db.delete(RegisteredDoorTable.TABLE_NAME, RegisteredDoorTable.USER_ID + " = ? AND "
                + RegisteredDoorTable.DOOR_ID + " = ?", new String[]{guest.getId(), door.getId()});

        Cursor cursor = this.getReadableDatabase().rawQuery("SELECT * FROM " + LogTable.TABLE_NAME + " WHERE "
                + LogTable.USER_ID + " = '" + guest.getId() + "' AND " + LogTable.DOOR_ID + " = '" + door.getId()
                + "'", null);
        if(cursor != null) {
            cursor.moveToFirst();
            if(cursor.getCount() == 0) {
                //Log.d("DatabaseHandler", "No log found. Deleting the guest");
                db.delete(UserTable.TABLE_NAME, UserTable.ID + " = ?", new String[]{guest.getId()});
            }
            cursor.close();
        }

        return rows > 0;
    }*/

    private boolean delete(String userID, String doorID)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(RegisteredDoorTable.TABLE_NAME, RegisteredDoorTable.USER_ID + " = ? AND "
                + RegisteredDoorTable.DOOR_ID + " = ?", new String[]{userID, doorID});

        return rows > 0;
    }

    private boolean isRegistered(User user, Door door)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT * FROM "+ RegisteredDoorTable.TABLE_NAME+" WHERE "+ RegisteredDoorTable.USER_ID +" = '"
                +user.getId()+"' AND "+ RegisteredDoorTable.DOOR_ID+" = '"+door.getId()+"'", null);
        if(cursor != null)
        {
            count = cursor.getCount();
            cursor.close();
        }
        //db.close();
        return (count > 0);
    }
    private boolean isRegistered(String userID, String doorID)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = db.rawQuery("SELECT * FROM "+ RegisteredDoorTable.TABLE_NAME+" WHERE "+ RegisteredDoorTable.USER_ID +" = '"
                +userID+"' AND "+ RegisteredDoorTable.DOOR_ID+" = '"+doorID+"'", null);
        if(cursor != null)
        {
            count = cursor.getCount();
            cursor.close();
        }
        //db.close();
        return (count > 0);
    }

    /*public ArrayList<Guest> getGuests(String doorId, int viewCode)
    {
        ArrayList<Guest> guests = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Guest guest;
        Cursor cursor = null;
        switch (viewCode)
        {
            case Utils.SHOW_ALL_GUESTS:
                cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                        + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND u."
                        + UserTable.ID + " = " + "r." + RegisteredDoorTable.USER_ID + " ORDER BY " + UserTable.NAME, null);
                break;
            case Utils.SHOW_ALL_GUESTS_EXCEPT_KEY_DELETED: *//* Get Active and Expired guests *//*
                cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                        + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND r."
                        +RegisteredDoorTable.KEY_STATUS+" <> '"+Door.KEY_DELETED+"' AND u."
                        + UserTable.ID + " = " + "r." + RegisteredDoorTable.USER_ID + " ORDER BY " + UserTable.NAME, null);
                break;
            case Utils.SHOW_ACTIVE_GUESTS_ONLY: *//* Get Active guests only *//*
                cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                        + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND r."
                        +RegisteredDoorTable.KEY_STATUS+" <> '"+Door.KEY_DELETED+"' AND r."
                        +RegisteredDoorTable.KEY_STATUS+" <> '"+Door.KEY_EXPIRED+"' AND u."
                        + UserTable.ID + " = " + "r." + RegisteredDoorTable.USER_ID + " ORDER BY " + UserTable.NAME, null);
                break;
            case Utils.SHOW_EXPIRED_GUESTS_ONLY:
                cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE "
                        + "u." + UserTable.ACCESS_MODE + " = 'guest' AND r." + RegisteredDoorTable.DOOR_ID + " = '" + doorId + "' AND r."
                        +RegisteredDoorTable.KEY_STATUS+" = '"+Door.KEY_EXPIRED+"' AND u."
                        + UserTable.ID + " = " + "r." + RegisteredDoorTable.USER_ID + " ORDER BY " + UserTable.NAME, null);
                break;
        }

        if(cursor != null)
        {
            cursor.moveToFirst();
            do{
                if(cursor.getCount()>0) {
                    guest = new Guest(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getString(3),
                            cursor.getString(7), ImageUtility.getImage(cursor.getBlob(4)));
                    guest.setAccessStartDateTime(cursor.getString(8));
                    guest.setAccessEndDateTime(cursor.getString(9));
                    guests.add(guest);
                }
            }while(cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return guests;
    }*/


    /*public boolean setImage(User user)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(UserTable.IMAGE, ImageUtility.getBytes(user.getImage()));
        int rowsAffected = db.update(UserTable.TABLE_NAME, cv, UserTable.ID + " = ?", new String[]{user.getId()});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }*/

    /*public Bitmap getImage(User user)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Bitmap image = null;
        Cursor cursor = db.rawQuery("SELECT " + UserTable.IMAGE + " FROM " + UserTable.TABLE_NAME + " WHERE "
                + UserTable.ID + " = '" + user.getId() + "'", null);
        if(cursor != null)
        {
            cursor.moveToFirst();
            if(cursor.getCount() == 1) {
                image = ImageUtility.getImage(cursor.getBlob(0));
            }
            cursor.close();
        }
        db.close();
        return image;
    }*/

    private boolean isExist(AccessLog accessLog)
    {
        boolean isExists = false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + LogTable.ACCESS_DATE_TIME + ", " + LogTable.ACCESS_STATUS + " FROM "
                + LogTable.TABLE_NAME + " WHERE " + LogTable.USER_ID + " = '" + accessLog.getUser().getId() + "' AND "
                + LogTable.DOOR_ID + " = '" + accessLog.getDoor().getId() + "' AND " + LogTable.ACCESS_DATE_TIME + " = '"
                + accessLog.getAccessDateTime() + "' AND " + LogTable.ACCESS_STATUS + " = '" + accessLog.getAccessStatus() + "'", null);
        if(cursor != null)
        {
            cursor.moveToFirst();
            isExists = cursor.getCount() > 0;
            cursor.close();
        }
        //db.close();
        return isExists;
    }

    private boolean isExist(String userID)
    {
        boolean isExists = false;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + UserTable.TABLE_NAME + " WHERE " + UserTable.ID + " = '" + userID + "'", null);
        if(cursor!=null)
        {
            cursor.moveToFirst();
            isExists = cursor.getCount()>0;
            cursor.close();
        }
        //db.close();
        return isExists;
    }
    /*public boolean update(Guest guest)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues userValues = new ContentValues();
        userValues.put(UserTable.NAME, guest.getName());
        userValues.put(UserTable.PHONE, guest.getPhone());
        userValues.put(UserTable.EMAIL, guest.getEmail());
        userValues.put(UserTable.IMAGE, ImageUtility.getBytes(guest.getImage()));
        userValues.put(UserTable.ACCESS_MODE, "guest");
        userValues.put(UserTable.ACCESS_TYPE, guest.getAccessType());
        userValues.put(UserTable.START_ACCESS_DATETIME, guest.getAccessStartDateTime());
        userValues.put(UserTable.END_ACCESS_DATETIME, guest.getAccessEndDateTime());
        int rowsAffected = db.update(UserTable.TABLE_NAME, userValues, UserTable.ID + " = ?", new String[]{guest.getId()});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }*/

    /*public boolean update(Owner owner)
    {
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

    private boolean update(Door door)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DoorTable.NAME, door.getName());
        int rowsAffected = db.update(DoorTable.TABLE_NAME, values, DoorTable.ID + " = ?", new String[]{door.getId()});

        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }

    /*public boolean insertRouterInfo(String doorId, String routerAddress, int routerPort){
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

    /*public RouterInfo getRouterInfo(String doorId){
        RouterInfo routerInfo = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DoorTable.TABLE_NAME + " WHERE " + DoorTable.ID + " = '" + doorId + "'", null);
        if(cursor != null)
        {
            cursor.moveToFirst();
            if(cursor.getCount() == 1)
            {
                routerInfo = new RouterInfo(cursor.getString(2), cursor.getInt(3));
            } else {
                return null;
            }
            cursor.close();
        }
        db.close();
        return routerInfo;
    }*/

    /*
    * updateKeyStatus will calculate current time with access end time and update
    * key status according to it. If parameters are null then the method will
    * update key status for all guests.
    */
    public void updateKeyStatus(String guestId, String doorId){
        Guest guest;
        Door door;

        if(guestId == null && doorId == null) {
            //Log.d("DatabaseHandler", "updateKeyStatus/Updating all guests [guestId=" + guestId + ", doorId=" + doorId + "]");
            boolean flag = false;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + UserTable.ID + ", " + UserTable.NAME + ", " + UserTable.ACCESS_TYPE + ", " + UserTable.END_ACCESS_DATETIME + ", "
                    + RegisteredDoorTable.DOOR_ID + ", " + RegisteredDoorTable.KEY_STATUS + " FROM " + UserTable.TABLE_NAME + " u, "
                    + RegisteredDoorTable.TABLE_NAME + " r WHERE u." + UserTable.ID + " = r." + RegisteredDoorTable.USER_ID + " AND "
                    + UserTable.ACCESS_MODE + " = 'guest' AND r."+ RegisteredDoorTable.KEY_STATUS + " <> " + Door.KEY_DELETED, null);
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
                        }

                    }
                } while (cursor.moveToNext());
                cursor.close();

            }
        } else if(guestId != null && doorId != null){
            Log.d("DatabaseHandler", "updateKeyStatus/Updating selected guest [guestId="+guestId+", doorId="+doorId+"]");
            boolean flag = false;
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT " + UserTable.ID + ", " + UserTable.NAME + ", " + UserTable.ACCESS_TYPE + ", "
                    + UserTable.END_ACCESS_DATETIME + ", " + RegisteredDoorTable.DOOR_ID + ", " + RegisteredDoorTable.KEY_STATUS
                    + " FROM " + UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME + " r WHERE u." + UserTable.ID
                    + " = r." + RegisteredDoorTable.USER_ID + " AND " + UserTable.ACCESS_MODE + " = 'guest' AND r."
                    + RegisteredDoorTable.USER_ID+" = '"+guestId+"' AND r." +RegisteredDoorTable.DOOR_ID+" = '"+doorId
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

    /*public void deleteKeys(ArrayList<Guest> guests){
        SQLiteDatabase db = this.getWritableDatabase();
        boolean isLogExist = false;
        boolean isRegisteredForMultipleDoors = false;
        Log.w("DatabaseHandler", "Deleting Keys:" + guests + " [Door="+appContext.getDoor().getId()+"]");
        for(Guest guest : guests) {
            Cursor cursor1 = this.getReadableDatabase().rawQuery("SELECT * FROM " + LogTable.TABLE_NAME + " WHERE "
                    + LogTable.USER_ID+ " = '"+guest.getId()+"'", null);
            if(cursor1 != null) {
                cursor1.moveToFirst();
                if(cursor1.getCount()>0){
                    isLogExist = true;
                }
                cursor1.close();
            }
            Cursor cursor2 = this.getReadableDatabase().rawQuery("SELECT "+UserTable.ID+", "+RegisteredDoorTable.KEY_STATUS+", "
                    +RegisteredDoorTable.DOOR_ID+" FROM "+UserTable.TABLE_NAME + " u, " + RegisteredDoorTable.TABLE_NAME
                    + " r WHERE "+ RegisteredDoorTable.USER_ID+ " = '"+guest.getId()+"' AND u."+UserTable.ID+" = r."
                    + RegisteredDoorTable.USER_ID, null);
            if(cursor2 != null) {
                cursor2.moveToFirst();
                if(cursor2.getCount()>1){
                    isRegisteredForMultipleDoors = true;
                }
                cursor2.close();
            }

            if(isLogExist || isRegisteredForMultipleDoors) {
                ContentValues values = new ContentValues();
                values.put(RegisteredDoorTable.KEY_STATUS, Door.KEY_DELETED);
                int rows = db.update(RegisteredDoorTable.TABLE_NAME, values, RegisteredDoorTable.USER_ID
                        + " = ? AND "+RegisteredDoorTable.DOOR_ID+" = ?",new String[]{guest.getId(),
                        appContext.getDoor().getId()});
                Log.w("DatabaseHandler", "deleteKeys/updated:" + (rows > 0));
            } else {
                if(new DatabaseHandler(mContext).isRegistered(guest.getId(), appContext.getDoor().getId())) {
                    if (new DatabaseHandler(mContext).delete(guest.getId(), appContext.getDoor().getId())) {
                        Log.w("CustomAdapter", "deleteKeys/Deleting from RegisteredDoorTable/" + guest.getName());
                    }
                }
                int rows = db.delete(UserTable.TABLE_NAME, UserTable.ID + " = ? ", new String[]{guest.getId()});
                Log.w("DatabaseHandler", "deleteKeys/deleted:" + (rows > 0));
            }
        }

    }*/

    private int getKeyStatus(String userId, String doorId){
        SQLiteDatabase db = this.getReadableDatabase();
        int status = Door.KEY_UNKNOWN;
        Cursor cursor = db.rawQuery("SELECT "+RegisteredDoorTable.KEY_STATUS+" FROM "+RegisteredDoorTable.TABLE_NAME
                +" WHERE "+RegisteredDoorTable.USER_ID+" = ? AND "+RegisteredDoorTable.DOOR_ID+" = ?", new String[]{userId, doorId});
        if(cursor != null){
            if(cursor.getCount()>0) {
                cursor.moveToFirst();
                status = cursor.getInt(0);
            }
            cursor.close();
        }
        //Log.d("DatabaseHandler", "getKeyStatus/keyStatus:" + status);
        db.close();
        return status;
    }

    /*public boolean addNetwork(WifiNetwork wifiNetwork)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(WifiConfigTable.SSID, wifiNetwork.getSSID());
        values.put(WifiConfigTable.SECURITY, wifiNetwork.getSecurity());
        values.put(WifiConfigTable.PASSWORD, wifiNetwork.getPassword());
        values.put(WifiConfigTable.AUTO_CONNECT, wifiNetwork.getAutoConnect());

        long row = db.insert(WifiConfigTable.TABLE_NAME, null, values);

        return (row != -1);
    }*/

    public ArrayList<WifiNetwork> getNetworks()
    {
        ArrayList<WifiNetwork> networks = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        WifiNetwork wifiNetwork;
        Cursor cursor = db.rawQuery("SELECT * FROM " + WifiConfigTable.TABLE_NAME, null);
        if(cursor != null)
        {
            cursor.moveToFirst();
            do{
                if(cursor.getCount()>0) {
                    wifiNetwork = new WifiNetwork(cursor.getString(1), cursor.getInt(2));
                    wifiNetwork.setPassword(cursor.getString(3));
                    wifiNetwork.setAutoConnect(cursor.getInt(4));
                    networks.add(wifiNetwork);
                }
            }while(cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return networks;
    }

    /*public boolean update(WifiNetwork wifiNetwork, String oldSSID, int updateFlag){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        switch (updateFlag){
            case WifiNetwork.UPDATE_SSID:
                Log.d(TAG, "UPDATE_SSID [old = "+ oldSSID+", new = "+wifiNetwork.getSSID()+"]");
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
        Log.d(TAG, "rowsAffected:"+rowsAffected);
        //db.execSQL("UPDATE myTable SET Column1 = someValue WHERE columnId = "+ someValue);
        return rowsAffected > 0;
    }*/

    /*public void deleteNetwork(WifiNetwork network){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(WifiConfigTable.TABLE_NAME, WifiConfigTable.SSID + " = ?", new String[]{network.getSSID()});
    }*/
}
