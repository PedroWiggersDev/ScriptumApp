package com.udesc.scriptumapp.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

@Entity(tableName = "categories")
public class Category {

    @PrimaryKey
    @NonNull
    public final String id;


    @SerializedName("title")
    public final String name;
    public final int documentCount;

    public Category(@NonNull String id, String name, int documentCount) {
        this.id = id;
        this.name = name;
        this.documentCount = documentCount;
    }
}
