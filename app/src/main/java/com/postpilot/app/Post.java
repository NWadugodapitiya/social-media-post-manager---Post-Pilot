package com.postpilot.app;

public class Post {
    private String date;
    private String title;
    private String description;
    private String platform;
    private String status;
    private boolean isSelected = false;

    public Post(String date, String title, String description, String platform, String status) {
        this.date = date;
        this.title = title;
        this.description = description;
        this.platform = platform;
        this.status = status;
    }

    public String getDate() { return date; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getPlatform() { return platform; }
    public String getStatus() { return status; }
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { isSelected = selected; }
}