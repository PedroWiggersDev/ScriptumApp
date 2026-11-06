package com.udesc.scriptumapp.data.remote;

import com.udesc.scriptumapp.model.AuthRequest;
import com.udesc.scriptumapp.model.AuthResponse;
import com.udesc.scriptumapp.model.Category;
import com.udesc.scriptumapp.model.Document;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface RetrofitEndpoints {

    @GET("/categories")
    Call<List<Category>> getCategories(@Header("Authorization") String token);

    @GET("/documents/category/{categoryId}")
    Call<List<Document>> getDocumentsByCategory(
            @Header("Authorization") String token,
            @Path("categoryId") Long categoryId
    );

    @POST("/documents/manual")
    Call<Document> createManualDocument(
            @Header("Authorization") String token,
            @Body ManualDocumentRequest request
    );

    @POST("/categories")
    Call<Category> createCategory(
            @Header("Authorization") String token,
            @Body CategoryRequest request
    );

    public static class NoteRequest {
        public Long categoryId;
        public String title;
        public String imageBase64;
        public String language;

        public NoteRequest(Long categoryId, String title, String imageBase64, String language) {
            this.categoryId = categoryId;
            this.title = title;
            this.imageBase64 = imageBase64;
            this.language = language;
        }
    }

    @POST("/documents/scanned")
    Call<Document> sendScannedDocument(
            @Header("Authorization") String token,
            @Body NoteRequest request
    );

    @POST("/auth/login")
    Call<AuthResponse> authenticate(@Body AuthRequest request);

    public static class ManualDocumentRequest {
        public Long categoryId;
        public String title;
        public List<String> paragraphs;

        public ManualDocumentRequest(Long categoryId, String title, List<String> paragraphs) {
            this.categoryId = categoryId;
            this.title = title;
            this.paragraphs = paragraphs;
        }
    }

    class CategoryRequest {
        public String title;
        public CategoryRequest(String title) {
            this.title = title;
        }
    }
}