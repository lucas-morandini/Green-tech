package com.example.frontend.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Message {
    private long id;
    private long userId;
    private long groupId;
    private String content;
    private Date date;
    private ArrayList<MessagePJ> pieces_jointe;

    private User user;
    public Message(long id, long userId, long groupId, String content, Date date,ArrayList<MessagePJ> pieces_jointe) {
        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.content = content;
        this.date = date;
        this.pieces_jointe = pieces_jointe;
    }

    public Message() {
        this.pieces_jointe = new ArrayList<>();
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUser(long userId) {
        this.userId = userId;
    }

    public long getGroupId() {
        return groupId;
    }

    public void setGroup(long groupId) {
        this.groupId = groupId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ArrayList<MessagePJ> getPieces_jointe() {
        return pieces_jointe;
    }

    public void setPieces_jointe(ArrayList<MessagePJ> pieces_jointe) {
        this.pieces_jointe = pieces_jointe;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void AddPieceJointe(MessagePJ messagePJ)
    {
        this.pieces_jointe.add(messagePJ);
    }

    public String getHour() {
        Date date = this.getDate();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    public void setUser(User user){
        this.user = user;
    }

    public User getUser(){
        return this.user;
    }

    public void JsonToMessage(JSONObject message)
    {
        try
        {
            long id = message.getLong("id");
            this.setId(id);
            long userId = message.getLong("userId");
            this.setUser(userId);
            long groupId = message.getLong("groupId");
            this.setGroup(groupId);
            String date_json = message.getString("created_at");
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Important pour les dates en "Z" (UTC)

            try {
                date = sdf.parse(date_json);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("MessagePJ", e.toString());
            }
            this.setDate(date);
            JSONArray messagePJArray = message.getJSONArray("pieces_jointe");
            for(int i = 0; i<messagePJArray.length(); i++)
            {
                JSONObject messagePJ_JSON = messagePJArray.getJSONObject(i);
                MessagePJ messagePJ = new MessagePJ();
                messagePJ.JsonToMessagePJ(messagePJ_JSON);
                this.AddPieceJointe(messagePJ);
            }
            String content = message.getString("content");
            this.setContent(content);
        }catch (JSONException e)
        {
            e.printStackTrace();
            Log.d("Message", e.toString());
        }
    }
}
