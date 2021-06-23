package com.asiczen.azlock.app.model;

import java.util.ArrayList;

/*
 * Created by user on 8/26/2015.
 */
public class Owner extends User {
    private String pin;
    private final ArrayList<Door> doors;

    public Owner()
    {
        super();
        pin = null;
        doors = new ArrayList<>();
    }

    public static Owner getInstance(User user)
    {
        Owner owner = null;
        if(user instanceof Owner)
            owner = (Owner) user;
        return owner;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    private String getPin() {
        return pin;
    }

    private ArrayList<Door> getDoors() {
        return doors;
    }

    @Override
    public String toString() {
        return "User ID:"+getId()+"\nName:"+getName()+"\nPhone:"+getPhone()
                +"\nEmail:"+getEmail()+"\nMode:"+getAccessMode()+"\nPin:"+getPin()
                +"\nRegistered Doors:"+getDoors()+"\n";
    }
}
