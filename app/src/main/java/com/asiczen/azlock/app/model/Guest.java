package com.asiczen.azlock.app.model;

import android.graphics.Bitmap;

import java.util.ArrayList;

/*
 * Created by user on 8/26/2015.
 */
public class Guest extends User {

    private String accessStartDateTime, accessEndDateTime, accessType;

    private ArrayList<AccessLog> log;
    private final ArrayList<Door> doors;


    public Guest()
    {
        super();
        accessType = null;
        accessStartDateTime = accessEndDateTime = null;
        log = new ArrayList<>();
        doors = new ArrayList<>();
    }

    public Guest(String id, String name, String phone, String email, String accessType, Bitmap image)
    {
        super(id, name, phone, email, image);
        this.accessType = accessType;
        accessStartDateTime = accessEndDateTime = null;
        log = new ArrayList<>();
        doors = new ArrayList<>();
    }

    public Guest(String id, String name, Bitmap image)
    {
        super(id, name, image);
        accessType = null;
        accessStartDateTime = accessEndDateTime = null;
        log = new ArrayList<>();
        doors = new ArrayList<>();
    }

    public static Guest getInstance(User user)
    {
        Guest guest = null;
        if(user instanceof Guest)
            guest = (Guest) user;
        return guest;
    }

    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }

    private ArrayList<AccessLog> getLog() {
        return log;
    }

    /*public void setLog(ArrayList<AccessLog> log) {
        this.log = log;
    }*/

    private ArrayList<Door> getDoors() {
        return doors;
    }

    public void setAccessStartDateTime(String accessStartDateTime) {
        this.accessStartDateTime = accessStartDateTime;
    }

    public void setAccessEndDateTime(String accessEndDateTime) {
        this.accessEndDateTime = accessEndDateTime;
    }

    public String getAccessEndDateTime() {
        return accessEndDateTime;
    }

    public String getAccessStartDateTime() {
        return accessStartDateTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Guest)) {
            return false;
        }

        Guest guest = (Guest) obj;
        return this.getId().equalsIgnoreCase(guest.getId());
    }

    @Override
    public String toString() {
        return "User ID:"+getId()+"\nName:"+getName()+"\nPhone:"+getPhone()+"\nAccess:"+getAccessType()
                +"\nEmail:"+getEmail()+"\nMode:"+getAccessMode()+"\nStart Time:"+getAccessStartDateTime()
                +"\nEnd Time:" +getAccessEndDateTime()
                +"\nRegistered Doors:"+getDoors()+"\nLogs:"+getLog();
    }
}
