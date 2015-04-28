package com.capstone.zacharyverbeck.audiorecordtest.Models;

/**
 * Created by zacharyverbeck on 4/25/15.
 */
public class User {
    public String email;
    public String password;
    public String username;
    public String number;

    public User(String email, String pass) {
        this.email = email;
        this.password = pass;
        this.username = null;
        this.number = null;
    }

    public User(String email, String pass, String username, String number) {
        this.email = email;
        this.password = pass;
        this.username = username;
        this.number = number;
    }
}
