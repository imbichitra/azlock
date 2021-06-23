package com.asiczen.azlock.util;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * Created by user on 7/30/2015.
 */
public class FileAccess {
    private final String fileName;
    private final Context context;
    public boolean FILE_NOT_FOUND;

    public FileAccess(Context context, String fileName)
    {
        this.context = context;
        this.fileName = fileName;
        FILE_NOT_FOUND = false;
    }

    public String read()
    {
        String s = null;
        byte[] temp;
        File file = new File(context.getFilesDir(), fileName);
        InputStream in;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            temp = new byte [in.available()];
            if(in.read(temp, 0, temp.length) == -1)
            {
                return null;
            }
            in.close();
            s = new String(temp);
            FILE_NOT_FOUND = false;
        } catch (FileNotFoundException e) {
            FILE_NOT_FOUND = true;
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    public void write(String content)
    {
        FileOutputStream out;
        try {
            out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            out.flush();
            out.write(content.getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
