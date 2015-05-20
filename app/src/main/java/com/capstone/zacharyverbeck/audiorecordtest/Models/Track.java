package com.capstone.zacharyverbeck.audiorecordtest.Models;

/**
 * Created by zacharyverbeck on 4/27/15.
 */
public class Track {
    public int id;
    public int bpm;
    public boolean type;
    public double longitude;
    public double latitude;
    public String title;
    public String createdAt;
    public String updatedAt;
    public int owner_id;
    public User User;

    public Track(String title, int bpm, double longitude, double latitude) {
        this.title = title;
        this.bpm = bpm;
        this.longitude = longitude;
        this.latitude = latitude;
    }
}
