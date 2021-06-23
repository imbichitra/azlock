package com.asiczen.azlock.util;

import android.util.Log;

import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/*
 * Created by user on 8/31/2015.
 */
public class DateTimeFormat {
    /*public static String getDate()
    {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(new Date());
    }

    public static String getTime()
    {
        return new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(new Date());
    }*/

    public static String getDateTime(int formatCode)
    {
        switch (formatCode) {
            case 1:
                return new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.ENGLISH).format(new Date());
            case 2:
                return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(new Date());
            case 3:
                /* August 25, 2016 16:15:14 PM */
                return new SimpleDateFormat("MMMM dd, yyyy HH:mm:ss a", Locale.ENGLISH).format(Calendar.getInstance().getTime());
            case 4:
                /* August 25, 2016 04:15:14 PM */
                return new SimpleDateFormat("MMMM dd, yyyy hh:mm:ss a", Locale.ENGLISH).format(Calendar.getInstance().getTime());
            default:
                return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(new Date());
        }
    }

    public static int[] splitDateTime()
    {
        int[] x;
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(new Date());
        String time = new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(new Date());
        String[] sDate = date.split("/");
        String[] sTime = time.split(":");
        x=new int[sDate.length+sTime.length+1];
        int i;
        for(i = 0; i < sDate.length; i++) {
            if(i != sDate.length - 1) {
                x[i] = Integer.parseInt(sDate[i]);
            } else {
                x[i] = Integer.parseInt(sDate[i])/100;
                x[++i] = Integer.parseInt(sDate[i-1])%100;
            }
        }
        for(int j = i, k = 0; k < sTime.length; j++, k++)
            x[j] = Integer.parseInt(sTime[k]);
        return x;
    }

    public static int[] splitDateTime(String dateTime){
        /*
        * Input: yyyy-mm-dd HH:mm
        * Output: int array {dd, mm, yy, yy, HH, mm}
        */
        int[] x = new int[6];
        String[] dt = dateTime.split(" ");
        String[] d = dt[0].split("-");
        String[] t = dt[1].split(":");
        for(int i = d.length-1, j = 0; i >= 0 && j < x.length; i--, j++){
            if(i == 0){
                int n = Integer.parseInt(d[i]);
                x[j++] = n / 100;
                x[j] = n % 100;
            }
            else
            {
                x[j] = Integer.parseInt(d[i]);
            }
        }
        x[4] = Integer.parseInt(t[0]);
        x[5] = Integer.parseInt(t[1]);

        return x;
    }

    public static String getDate(int dd, int mm, int yyyy, int dateFormatCode)
    {
        /*
        * formatCode = 1; dd/mm/yyyy
        * formatCode = 2; March 2, 2015
        * formatCode = 3; yyyy-mm-dd
        * formatCode = 4; mm/dd/yyyy
        * formatCode = 2; Mar 2, 2015
        * */
        String date="";
        if(dateFormatCode == 1)
        {
            date += formatInteger(dd);
            date += "/";
            date += formatInteger(mm);
            date += "/"+ yyyy;
        } else if(dateFormatCode == 2) {
            date += getMonthName(mm)+" "+dd+", "+yyyy;
        } else if(dateFormatCode == 3) {
            date += yyyy +"-"+formatInteger(mm)+"-"+formatInteger(dd);
        } else if(dateFormatCode == 4) {
            date += formatInteger(mm)+"/"+formatInteger(dd)+"/"+ yyyy;
        } else if(dateFormatCode == 5) {
            date += getMonthName(mm).substring(0,3)+" "+dd+", "+yyyy;
        }
        return date;
    }
    public static String getDate(String dateTimeFromDatabase, int dateFormatCode)
    {
        /*
        * @param dateTimeFromDatabase = yyyy-mm-dd HH:MM; [Input]
        * @return July 8, 2015 10:09 AM [if dateFormatCode = 1]
        * or July 8, 2015 [if dateFormatCode = 2] or Jul 8, 2015 [if dateFormatCode = 3]
        * */

        //Log.d(TAG, "getDate/"+dateTimeFromDatabase);
        String[] dateTimeArray = dateTimeFromDatabase.split(" ");
        String[] date = dateTimeArray[0].split("-");
        String temp = date[0];
        date[0] = date[2];
        date[2] = temp;
        if(dateFormatCode == 1) {
            return getDate(date, 2) + "\t" + convertTimeTo12Hours(dateTimeArray[1]);
        } else if(dateFormatCode == 2) {
            return getDate(date, 2);
        } else if(dateFormatCode == 3) {
            return getDate(date, 4) + "\t" + convertTimeTo12Hours(dateTimeArray[1]);
        }
        return null;
    }
    public static String getDate(String[] date, int dateFormatCode)
    {
        /*
        * date[0] = dd; date[1] = mm; date[2] = yyyy;
        * formatCode = 1; dd/mm/yyyy
        * formatCode = 2; March 2, 2015
        * formatCode = 3; yyyy-mm-dd
        * formatCode = 4; Mar 2, 2015
        * */
        String resultDate = "";
        if(dateFormatCode == 1)
        {
            resultDate += formatInteger(Integer.parseInt(date[0]));
            resultDate += "/";
            resultDate += formatInteger(Integer.parseInt(date[1]));
            resultDate += "/" + date[2];
        } else if(dateFormatCode == 2) {
            resultDate += getMonthName(Integer.parseInt(date[1])) + " " + date[0] + ", " + date[2];
        } else if(dateFormatCode == 3) {
            resultDate += date[2] + "-" + formatInteger(Integer.parseInt(date[1]))
                    + "-" + formatInteger(Integer.parseInt(date[0]));
        } else if(dateFormatCode == 4) {
            resultDate += getMonthName(Integer.parseInt(date[1])).substring(0,3) + " " + date[0] + ", " + date[2];
        }
        return resultDate;
    }
   /* public static String getTime(int hh, int mm)
    {
        String time="";
        time += formatInteger(hh);
        time += ":";
        time += formatInteger(mm);
        return time;
    }*/
    private static String formatInteger(int n)
    {
        String s="";
        String number = String.valueOf(n);
        s += (number.length() == 1 ? "0"+number : number);
        return s;
    }
    public static String getMonthName(int month) {
        return new DateFormatSymbols().getMonths()[month-1];
    }
    public static String convertTimeTo12Hours(String timeIn24)
    {
        Date dateObj = new Date();
        //Log.d(TAG, "convertTimeTo12Hours/"+timeIn24);
        try {
            final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            dateObj = sdf.parse(timeIn24);
            System.out.println(dateObj);
            if (dateObj != null) {
                System.out.println(new SimpleDateFormat("hh:mm a",Locale.getDefault()).format(dateObj));
            }
        } catch (final ParseException e) {
            e.printStackTrace();
        }
        return new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(dateObj);
    }

    public static String parseDateFormat(String date, int start)
    { /*
       * consider lower byte only as these indexes hold Integer values
       * Generate format from received packet YYYY-MM-DD HH:MM
       */
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        Date d = null;
        try {
            d = formatter.parse(String.valueOf(date.charAt(start + 2) & 0xFF) + (date.charAt(start + 3) & 0xFF) + "-"
                    + (date.charAt(start + 1) & 0xFF) + "-" + (date.charAt(start) & 0xFF) + " "
                    + (date.charAt(start + 4) & 0xFF) + ":" + (date.charAt(start + 5) & 0xFF));
            //Log.d("DateTimeFormat", "parsed Date:"+d+" <> formatted String:"+formatter.format(d));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (d != null) {
            return formatter.format(d);
        }else{
            return "";
        }
    }

    public static String toString(Date date, SimpleDateFormat simpleDateFormat){
        return simpleDateFormat.format(date);
    }

    public static Date toDate(String date, SimpleDateFormat simpleDateFormat){
        Date convertedDate=null;
        try {
            convertedDate=simpleDateFormat.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return convertedDate;
    }

    // convert seconds (stored in byte array) to date time
    // 4-bytes represents the datetime in seconds
    public static String parseTime(byte[] bytes, int startIndex){
        byte[] b = new byte[4];
        b[0]=bytes[startIndex++];
        b[1]=bytes[startIndex++];
        b[2]=bytes[startIndex++];
        b[3]=bytes[startIndex];

        int x = getUInt32(b);
        Date d=new Date((long)x*1000);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);

        Log.d("DateTimeFormat", "parsedTime:"+d+" <> formatted Date:"+formatter.format(d));
        return formatter.format(d);
    }

    // utility function to generate int from 4 bytes
    private static int getUInt32(byte[] bytes) {
        int value = bytes[3] & 0xFF;
        value |= ((bytes[2] & 0xFF) << 8);
        value |= ((bytes[1] & 0xFF) << 16);
        value |= ((bytes[0] & 0xFF) << 24);
        return value;
    }
}
