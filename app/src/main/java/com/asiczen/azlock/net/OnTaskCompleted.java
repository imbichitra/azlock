package com.asiczen.azlock.net;

/**
 * Created by Somnath on 9/19/2016.
 */
public interface OnTaskCompleted<T> {
    void onTaskCompleted(int resultCode, T data);
}
