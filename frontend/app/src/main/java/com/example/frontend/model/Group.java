package com.example.frontend.model;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Group {
    private long id;
    private String name;
    private ArrayList<User> users;
    private ArrayList<Identification> identifications;
    private ArrayList<Message> messages;

    public Group(long id, String name, ArrayList<User> users, ArrayList<Identification> identifications, ArrayList<Message> messages) {
        this.id = id;
        this.name = name;
        this.users = users;
        this.identifications = identifications;
        this.messages = messages;
    }

    public Group() {
        this.identifications = new ArrayList<>();
        this.users = new ArrayList<>();
        this.messages = new ArrayList<>();
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

    public ArrayList<User> getUsers() {
        return users;
    }

    public void setUsers(ArrayList<User> users) {
        this.users = users;
    }

    public ArrayList<Identification> getIdentifications() {
        return identifications;
    }

    public void setIdentifications(ArrayList<Identification> identifications) {
        this.identifications = identifications;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    public void AddIdentification(Identification identification)
    {
        this.identifications.add(identification);
    }

    public void AddUser(User user)
    {
        this.users.add(user);
    }

    public void AddMessage(Message message)
    {
        this.messages.add(message);
    }

    public void JsonToGroup(JSONObject group)
    {
        try {
            long id = group.getLong("id");
            String name = group.getString("name");
            this.setId(id);
            this.setName(name);
            JSONArray usersArray = group.getJSONArray("users");
            for(int i = 0; i<usersArray.length(); i++)
            {
                JSONObject userJson = usersArray.getJSONObject(i);
                User user = new User();
                user.JsonToUser(userJson);
                this.AddUser(user);
            }
            JSONArray identificationsArray = group.getJSONArray("identifications");
            for(int i = 0; i<identificationsArray.length(); i++)
            {
                JSONObject identificationJson = identificationsArray.getJSONObject(i);
                Identification identification = new Identification();
                identification.JsonToIdentification(identificationJson);
                this.AddIdentification(identification);
            }
            JSONArray messageArray = group.getJSONArray("messages");
            for(int i = 0; i<messageArray.length(); i++)
            {
                JSONObject messageJson = messageArray.getJSONObject(i);
                Message message = new Message();
                message.JsonToMessage(messageJson);
                this.AddMessage(message);
            }
        }catch (JSONException e)
        {
            e.printStackTrace();
            Log.d("Group", e.toString());
        }
    }
}
