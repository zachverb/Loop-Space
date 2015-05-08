package com.capstone.zacharyverbeck.audiorecordtest.Models;

/**
 * Created by zacharyverbeck on 4/27/15.
 */
public class Track {
    public int id;
    public String title;
    public String createdAt;
    public String updatedAt;
    public int owner_id;
    public User User;

    public Track(String title) {
        this.title = title;
    }
}
