package com.kushmakar.akashjewellers;


import com.google.firebase.Timestamp;

public class UserModal {
    private String Phone;

    public UserModal(String phone, String username, Timestamp createdTimeStamp) {
        Phone = phone;
        this.username = username;
        this.createdTimeStamp = createdTimeStamp;
    }

    public String getPhone() {
        return Phone;
    }

    public UserModal() {
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Timestamp getCreatedTimeStamp() {
        return createdTimeStamp;
    }

    public void setCreatedTimeStamp(Timestamp createdTimeStamp) {
        this.createdTimeStamp = createdTimeStamp;
    }

    private String username;

    private Timestamp createdTimeStamp;


}
