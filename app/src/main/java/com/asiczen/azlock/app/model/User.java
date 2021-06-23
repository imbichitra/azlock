package com.asiczen.azlock.app.model;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.asiczen.azlock.MainActivity;
import com.asiczen.azlock.R;


/*
 * Created by user on 8/13/2015.
 */
public class User{

    private String id;
    private String name;
    private String phone;
    private String email;
    private static String accessMode;
    private Bitmap image;

    public User()
    {
        id = name = phone = email = accessMode = null;
        image = BitmapFactory.decodeResource(MainActivity.mContext.getResources(), R.mipmap.ic_user);
    }

    User(String id, String name, Bitmap image)
    {
        this.id = id;
        this.name = name;
        this.image = image;
        accessMode = null;
    }

    User(String id, String name, String phone, String email, Bitmap image)
    {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.image = image;
        accessMode = null;
    }

    public void setAccessMode(String accessMode) {
        User.accessMode = accessMode;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }



    public void setImage(Bitmap image) {
        this.image = image;
    }

    static String getAccessMode() {
        return accessMode;
    }

    public String getEmail() {
        return email;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getId() {
        return id.toUpperCase();
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return "User ID:"+getId()+"\nName:"+getName()+"\nPhone:"+getPhone()
                +"\nEmail:"+getEmail()+"\nMode:"+getAccessMode()+"\n";
    }
}
