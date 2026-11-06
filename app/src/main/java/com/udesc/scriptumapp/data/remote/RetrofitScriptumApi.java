package com.udesc.scriptumapp.data.remote;

import com.udesc.scriptumapp.model.AuthRequest;
import com.udesc.scriptumapp.model.AuthResponse;
import com.udesc.scriptumapp.model.Category;
import com.udesc.scriptumapp.model.Document;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitScriptumApi implements ScriptumApi {

    private final RetrofitEndpoints endpoints;

    public RetrofitScriptumApi() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://scriptum-kong-homol.onrender.com") // URL do Kong!
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        endpoints = retrofit.create(RetrofitEndpoints.class);
    }

    @Override
    public void getCategories(String token, CategoriesCallback callback) {

        endpoints.getCategories("Bearer " + token).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(response.body(), null);
                } else {
                    callback.onResult(null, new Exception("Erro HTTP: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                callback.onResult(null, t);
            }
        });
    }

    // TODO: Implementar getDocuments, createCategory, etc seguindo o mesmo padrão acima

    @Override
    public void authenticate(String username, String password, AuthCallback callback) {
        AuthRequest request = new AuthRequest(username, password);

        endpoints.authenticate(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onResult(response.body().token, username, null);
                } else {
                    callback.onResult(null, null, new Exception("Falha no login: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                callback.onResult(null, null, t);
            }
        });
    }

    @Override
    public void getDocuments(String token, String categoryId, DocumentsCallback callback) {
        // Implementar...
    }

    @Override
    public void createCategory(String token, String title, CategoryCallback callback) {}

    @Override
    public void sendTask(String token, String categoryId, String documentTitle, String content, TaskCallback callback) {}
}
