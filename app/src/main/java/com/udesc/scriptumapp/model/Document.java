package com.udesc.scriptumapp.model;

public class Document {
    public final String title;
    public final String summary;
    public final String status;
    public final String statusDetail;
    public final String content;

    public Document(String title, String summary, String status, String statusDetail, String content) {
        this.title = title;
        this.summary = summary;
        this.status = status;
        this.statusDetail = statusDetail;
        this.content = content;
    }
}
