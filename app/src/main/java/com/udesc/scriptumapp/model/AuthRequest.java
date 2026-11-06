package com.udesc.scriptumapp.model;

import com.google.gson.annotations.SerializedName;

public class AuthRequest {

    @SerializedName("usuario")
    public String username;

    @SerializedName("senha")
    public String password;

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}