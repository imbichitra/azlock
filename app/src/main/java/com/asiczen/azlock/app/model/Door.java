package com.asiczen.azlock.app.model;

import java.io.Serializable;
import java.util.HashMap;

/*
 * Created by user on 8/13/2015.
 */
public class Door implements Serializable {

    private String id;
    private String name;
    private final String doorIp;
    private RouterInfo routerInfo;
    public int status;

    public static int KEY_SHARED = 0;
    public static int KEY_EXPIRED = 1;
    public static int KEY_DELETED = 2;
    public static int KEY_UNKNOWN = -1;

    public static HashMap<Integer, String> statusString = new HashMap<Integer, String>(){{
        put(KEY_DELETED,"Key Deleted");
        put(KEY_EXPIRED,"Key Expired");
        put(KEY_UNKNOWN,"Key Unknown");
        put(KEY_SHARED,"Key Shared");
    }};

    public Door(){
        id = null;
        name = null;
        routerInfo = null;
        doorIp = null;
        status = KEY_UNKNOWN;
    }
    public Door(String id, String name)
    {
        this.id = id;
        this.name = name;
        routerInfo = null;
        doorIp = null;
        status = KEY_UNKNOWN;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    private RouterInfo getRouterInfo() {
        return routerInfo;
    }

    public void setRouterInfo(RouterInfo routerInfo) {
        this.routerInfo = routerInfo;
    }

    @Override
    public String toString() {
        return "Door ID:"+getId()+"\nDoor Name:"+getName()+"\nRouter Info:"+getRouterInfo();
    }
}
