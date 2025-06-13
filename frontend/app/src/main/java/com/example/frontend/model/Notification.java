package com.example.frontend.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Notification {
    private long id;
    private String title;
    private String content;
    private User user;
    private boolean read;


    public Notification(long id, String title, String content, User user, boolean read) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.user = user;
        this.read = read;
    }

    public Notification() {
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public void JsonToNotification(JSONObject notification)
    {
        try{
        long id = notification.getLong("id");
        String title = notification.getString("title");
        String content = notification.getString("content");
        boolean read = notification.getBoolean("read");
        JSONObject userJSON = notification.getJSONObject("user");
        this.setId(id);
        this.setTitle(title);
        this.setContent(content);
        this.setRead(read);
        User user = new User();
        user.JsonToUser(userJSON);
        this.setUser(user);
    }catch (JSONException e)
        {
            e.printStackTrace();
            Log.d("Notification", e.toString());
        }
    }
}
