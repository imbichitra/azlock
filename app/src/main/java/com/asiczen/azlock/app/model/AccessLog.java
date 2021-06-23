package com.asiczen.azlock.app.model;


/*
 * Created by user on 8/13/2015.
 */
public class AccessLog {

    private final String accessDateTime;
    private final String accessStatus;
    private final String failureReason;
    private int id;
    private User user;
    private Door door;

    public AccessLog(String userId, String accessDateTime, String accessStatus, String failureReason, String doorId){
        this.user = new User();
        this.door = new Door();
        this.user.setId(userId);
        this.door.setId(doorId);
        this.accessDateTime = accessDateTime;
        this.accessStatus = accessStatus;
        this.failureReason = failureReason;
        this.id = 0;
    }

    /*public void setId(int id) {
        this.id = id;
    }*/

    /*public int getId() {
        return id;
    }*/

    /*public void setDoor(Door door) {
        this.door = door;
    }*/

    /*public void setUser(User user) {
        this.user = user;
    }*/

    public String getAccessDateTime() {
        return accessDateTime;
    }

    public String getAccessStatus() {
        return accessStatus;
    }

    public Door getDoor() {
        return door;
    }

    public User getUser() {
        return user;
    }

    public String getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AccessLog)) {
            return false;
        }

        AccessLog dObj = (AccessLog) obj;

        return this.user.getId().equals(dObj.user.getId()) && this.accessDateTime.equalsIgnoreCase(dObj.accessDateTime)
                && this.accessStatus.equalsIgnoreCase(dObj.accessStatus) && this.door.getId().equalsIgnoreCase(dObj.door.getId());
    }

    @Override
    public int hashCode()
    {
        return id;
    }

    @Override
    public String toString() {
        return this.user+"Access Time:"+this.accessDateTime+"\nStatus:"+this.accessStatus+"\nReason:"
                +this.failureReason+"\n"+this.door;
    }
}
