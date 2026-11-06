package com.udesc.scriptumapp.data.remote;

import android.content.Context;

import com.udesc.scriptumapp.data.remote.MockScriptumApi;

public final class ScriptumApiFactory {
    private ScriptumApiFactory() {
    }

    public static ScriptumApi create(Context context) {
        return new RetrofitScriptumApi();
    }
}
