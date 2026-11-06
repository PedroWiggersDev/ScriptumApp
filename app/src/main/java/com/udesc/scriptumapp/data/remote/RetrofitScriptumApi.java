package com.udesc.scriptumapp.data.remote;

import com.udesc.scriptumapp.model.AuthRequest;
import com.udesc.scriptumapp.model.AuthResponse;
import com.udesc.scriptumapp.model.Category;
import com.udesc.scriptumapp.model.Document;
import java.util.List;
import java.util.Arrays;
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
        logging.setLevel(HttpLoggingInterceptor.Level.HEADERS);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://scriptum-kong-homol.onrender.com")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        endpoints = retrofit.create(RetrofitEndpoints.class);
    }

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

    @Override
    public void getDocuments(String token, String categoryId, DocumentsCallback callback) {
        endpoints.getDocumentsByCategory("Bearer " + token, Long.parseLong(categoryId)).enqueue(new Callback<List<Document>>() {
            @Override
            public void onResponse(Call<List<Document>> call, Response<List<Document>> response) {
                if (response.isSuccessful()) {
                    callback.onResult(response.body(), null);
                } else {
                    callback.onResult(null, new Exception("Erro ao buscar documentos: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Document>> call, Throwable t) {
                callback.onResult(null, t);
            }
        });
    }

    @Override
    public void createCategory(String token, String title, CategoryCallback callback) {
        RetrofitEndpoints.CategoryRequest request = new RetrofitEndpoints.CategoryRequest(title);

        endpoints.createCategory("Bearer " + token, request).enqueue(new Callback<Category>() {
            @Override
            public void onResponse(Call<Category> call, Response<Category> response) {
                if (response.isSuccessful()) {
                    callback.onResult(response.body(), null);
                } else {
                    callback.onResult(null, new Exception("Erro ao criar categoria: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Category> call, Throwable t) {
                callback.onResult(null, t);
            }
        });
    }

    @Override
    public void sendTask(String token, String categoryId, String documentTitle, String content, TaskCallback callback) {
        // Separa o texto em parágrafos por quebra de linha
        List<String> paragraphs = Arrays.asList(content.split("\\r?\\n"));

        // Passa o categoryId (convertido para Long) junto com o título e os parágrafos
        Long catId = Long.parseLong(categoryId);
        RetrofitEndpoints.ManualDocumentRequest request = new RetrofitEndpoints.ManualDocumentRequest(catId, documentTitle, paragraphs);

        endpoints.createManualDocument("Bearer " + token, request).enqueue(new Callback<Document>() {
            @Override
            public void onResponse(Call<Document> call, Response<Document> response) {
                if (response.isSuccessful()) {
                    callback.onResult(true, null);
                } else {
                    callback.onResult(false, new Exception("Erro ao salvar documento: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Document> call, Throwable t) {
                callback.onResult(false, t);
            }
        });
    }

    @Override
    public void sendScannedTask(String token, String categoryId, String documentTitle, String imageBase64, String language, TaskCallback callback) {
        Long catId = Long.parseLong(categoryId);
        RetrofitEndpoints.NoteRequest request = new RetrofitEndpoints.NoteRequest(catId, documentTitle, imageBase64, language);

        endpoints.sendScannedDocument("Bearer " + token, request).enqueue(new Callback<Document>() {
            @Override
            public void onResponse(Call<Document> call, retrofit2.Response<Document> response) {
                if (response.isSuccessful()) {
                    // O Quarkus vai retornar 202 Accepted, liberando o app imediatamente
                    callback.onResult(true, null);
                } else {
                    callback.onResult(false, new Exception("Erro ao enviar escaneamento: " + response.code()));
                }
            }

            @Override
            public void onFailure(Call<Document> call, Throwable t) {
                callback.onResult(false, t);
            }
        });
    }
}