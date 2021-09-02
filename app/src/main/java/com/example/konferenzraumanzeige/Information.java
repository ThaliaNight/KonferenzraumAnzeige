package com.example.konferenzraumanzeige;

import com.microsoft.graph.models.Event;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * single source of truth
 */

public class Information {

    private static final Information instance = new Information();
    private Information(){}

    private Boolean SignedIn = false;
    private String UserName = "Bitte loggen Sie sich ein";
    private String UserEmail;
    private String UserTimeZone;
    private List<Event> EventList;
    private ZonedDateTime mTime;


    public static Information getInstance(){
        return instance;
    }

    public ZonedDateTime getmTime() {
        return mTime;
    }

    public void setmTime(ZonedDateTime mTime) {
        this.mTime = mTime;
    }

    public List<Event> getEventList() {
        return EventList;
    }

    public void setEventList(List<Event> eventList) {
        EventList = eventList;
    }

    public Boolean getSignedIn() {
        return SignedIn;
    }

    public void setSignedIn(Boolean signedIn) {
        SignedIn = signedIn;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getUserEmail() {
        return UserEmail;
    }

    public void setUserEmail(String userEmail) {
        UserEmail = userEmail;
    }

    public String getUserTimeZone() {
        return UserTimeZone;
    }

    public void setUserTimeZone(String userTimeZone) {
        UserTimeZone = userTimeZone;
    }
}
