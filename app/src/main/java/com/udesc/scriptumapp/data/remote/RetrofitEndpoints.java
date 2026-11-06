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

    @POST("/auth/login")
    Call<AuthResponse> authenticate(@Body AuthRequest request);

    class ManualDocumentRequest {
        public String title;
        public List<String> paragraphs;

        public ManualDocumentRequest(String title, List<String> paragraphs) {
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