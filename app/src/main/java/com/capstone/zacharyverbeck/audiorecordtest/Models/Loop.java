package com.capstone.zacharyverbeck.audiorecordtest.Models;

import com.capstone.zacharyverbeck.audiorecordtest.Java.LoopButton;

/**
 * Created by zacharyverbeck on 4/23/15.
 */
public class Loop {

    private LoopButton mLoopButton;
    private String name;
    private String filePath;
    private String endpoint;
    private int id;

    public Loop(LoopButton loopButton) {
        this.setLoopButton(loopButton);
        this.setId(loopButton.getId());
        this.setName("ZGV");
        this.setFilePath(null);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public LoopButton getLoopButton() {
        return mLoopButton;
    }

    public void setLoopButton(LoopButton loopButton) {
        mLoopButton = loopButton;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }


}
