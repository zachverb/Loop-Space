package com.capstone.zacharyverbeck.loopspace.Models;

/**
 * Created by zacharyverbeck on 5/28/15.
 */
public class Comment {
    public int id;
    public String comment;
    public User User;
    public String createdAt;
    public String updatedAt;
    public String city;

    public Comment(String comment, String city) {
        this.comment = comment;
        this.city = city;
    }
}
