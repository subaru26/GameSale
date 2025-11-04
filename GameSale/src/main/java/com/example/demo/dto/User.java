package com.example.demo.dto;

import java.util.Map;

public class User {
    private String id;
    private String email;
    private String phone;
    private Boolean confirmed_at;
    private Map<String, Object> user_metadata;

    // getter/setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Boolean getConfirmed_at() { return confirmed_at; }
    public void setConfirmed_at(Boolean confirmed_at) { this.confirmed_at = confirmed_at; }

    public Map<String, Object> getUser_metadata() { return user_metadata; }
    public void setUser_metadata(Map<String, Object> user_metadata) { this.user_metadata = user_metadata; }
}
