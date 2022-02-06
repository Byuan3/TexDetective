package com.example.texdetective.data;

public class HistoryWrapper {
    private String id;
    private String date;
    private String text;
    private String url;

    public HistoryWrapper(String id, String date, String text, String url) {
        this.id = id;
        this.date = date;
        this.text = text;
        this.url = url;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
