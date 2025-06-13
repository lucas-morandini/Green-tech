package com.example.frontend.model;

import android.util.Log;

import com.example.frontend.adapter.ListItem;
import com.example.frontend.adapter.ListItemType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class User implements ListItem {
    private long id;
    private String name;
    private String first_name;
    private String role;
    private ArrayList<Identification> identifications;

    private String location;

    private boolean is_friend;


    public User(long id, String name, String first_name, String role,  ArrayList<Identification> identifications, String location) {
        this.id = id;
        this.name = name;
        this.first_name = first_name;
        this.role = role;
        this.identifications = new ArrayList<>();
        this.location = location;
    }

    public User()
    {
        this.identifications = new ArrayList<>();
    }

    @Override
    public ListItemType getType() {
        return ListItemType.USER;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public ArrayList<Identification> getIdentifications() {
        return this.identifications;
    }

    public void addIdentification (Identification identification) {
        this.identifications.add(identification);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInitials() {
        if (this.getFirst_name() == null || this.getFirst_name().isEmpty() || this.getName() == null || this.getName().isEmpty()) {
            return "";
        }
        return (Character.toUpperCase(this.getName().charAt(0)) + " . " + Character.toLowerCase(this.getFirst_name().charAt(0)));
    }

    public boolean getIsFriend()
    {
        return this.is_friend;
    }

    public void setIsFriend(boolean is_friend){
        this.is_friend = is_friend;
    }


    public void JsonToUser(JSONObject user)
    {
        try {
            long id = user.getLong("id");
            String name = user.getString("name");
            String first_name = user.getString("first_name");
            String role = user.getString("role");
            String location = user.getString("location");
            this.setId(id);
            this.setName(name);
            this.setFirst_name(first_name);
            this.setRole(role);
            this.setLocation(location);
            JSONArray user_identifications = user.getJSONArray("identifications");
            for(int i = 0; i<user_identifications.length(); i++)
            {
                JSONObject identificationJSON = user_identifications.getJSONObject(i);
                Identification identification = new Identification();
                identification.JsonToIdentification(identificationJSON);
                this.addIdentification(identification);
            }
            if(user.has("is_friend"))
            {
                boolean is_friend = user.getBoolean("is_friend");
                this.setIsFriend(is_friend);
            }
        }catch (JSONException e)
        {
            e.printStackTrace();
            Log.d("User", e.toString());
        }
    }
}