package com.capstone.zacharyverbeck.audiorecordtest.Models;

/**
 * Created by zacharyverbeck on 4/25/15.
 */
public class User {
    public String email;
    public String password;

    public User(String username, String pass) {
        this.email = username;
        this.password = pass;
    }
}
