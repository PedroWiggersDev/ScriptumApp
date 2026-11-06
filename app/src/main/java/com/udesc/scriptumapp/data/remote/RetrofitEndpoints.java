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
import retrofit2.http.Query;

public interface RetrofitEndpoints {

    @GET("/categories")
    Call<List<Category>> getCategories(@Header("Authorization") String token);

    @GET("/documents")
    Call<List<Document>> getDocuments(
            @Header("Authorization") String token,
            @Query("category") String categoryId
    );


    @POST("/auth/login")
    Call<AuthResponse> authenticate(@Body AuthRequest request);
}
