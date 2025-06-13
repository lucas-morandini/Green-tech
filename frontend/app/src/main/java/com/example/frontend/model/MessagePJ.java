package com.example.frontend.model;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class MessagePJ {
    private long id;
    private long messageId;
    String byte64;

    private Date date;
    private String name;


    public MessagePJ(long id, long messageId, String byte64, Date date) {
        this.id = id;
        this.messageId = messageId;
        this.byte64 = byte64;
        this.date = date;
    }

    public MessagePJ() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public String getByte64() {
        return byte64;
    }

    public void setByte64(String byte64) {
        this.byte64 = byte64;
    }

    public Date getDate()
    {
        return this.date;
    }

    public void setDate(Date date){
        this.date = date;
    }

    public String getName()
    {
        return this.name;
    }
    public void setName(String name)
    {
        this.name = name;
    }
    public void JsonToMessagePJ(JSONObject messagePJ)
    {
        try
        {
            long id = messagePJ.getLong("id");
            String byte64 = messagePJ.getString("byte64");
            long messageId = messagePJ.getLong("messageId");
            String date_json = messagePJ.getString("created_at");
            Date date = new Date();
            String name = messagePJ.getString("name");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // Important pour les dates en "Z" (UTC)
            try {
                date = sdf.parse(date_json);
            }catch(Exception e)
            {
                e.printStackTrace();
                Log.d("MessagePJ", e.toString());
            }
            this.setId(id);
            this.setByte64(byte64);
            this.setMessageId(messageId);
            this.setDate(date);
            this.setName(name);
        }catch (JSONException e)
        {
            e.printStackTrace();
            Log.d("MessagePJ", e.toString());
        }
    }
}
