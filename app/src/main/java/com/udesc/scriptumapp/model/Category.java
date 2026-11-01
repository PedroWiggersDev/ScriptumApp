package com.udesc.scriptumapp.model;

public class Category {
    public final String id;
    public final String name;
    public final int documentCount;

    public Category(String id, String name, int documentCount) {
        this.id = id;
        this.name = name;
        this.documentCount = documentCount;
    }
}
