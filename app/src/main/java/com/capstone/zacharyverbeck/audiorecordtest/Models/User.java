package com.capstone.zacharyverbeck.audiorecordtest.Models;

/**
 * Created by zacharyverbeck on 4/25/15.
 */
public class User {
    public String email;
    public String password;
    public String name;
    public String number;

    public User(String email, String pass) {
        this.email = email;
        this.password = pass;
        this.name = null;
        this.number = null;
    }

    public User(String email, String pass, String name, String number) {
        this.email = email;
        this.password = pass;
        this.name = name;
        this.number = number;
    }
}
