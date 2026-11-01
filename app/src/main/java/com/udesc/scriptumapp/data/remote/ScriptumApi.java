package com.udesc.scriptumapp.data.remote;

import java.util.List;

import com.udesc.scriptumapp.model.Category;
import com.udesc.scriptumapp.model.Document;

public interface ScriptumApi {
    // Integração serviço de autenticação: substituir a implementação mock por Retrofit.
    void authenticate(String username, String password, AuthCallback callback);

    // Integração serviço de categorias: obter catálogo atualizado usando o token da sessão.
    void getCategories(String token, CategoriesCallback callback);

    // Integração serviço de documentos: obter documentos vinculados à categoria informada.
    void getDocuments(String token, String categoryId, DocumentsCallback callback);

    // Integração serviço de categorias: enviar criação ao backend e retornar a categoria criada.
    void createCategory(String token, String title, CategoryCallback callback);

    // Integração serviço de tarefas: registrar documento/conteúdo processado no backend.
    void sendTask(String token, String categoryId, String documentTitle, String content, TaskCallback callback);

    interface AuthCallback {
        void onResult(String token, String username, Throwable error);
    }

    interface CategoriesCallback {
        void onResult(List<Category> categories, Throwable error);
    }

    interface DocumentsCallback {
        void onResult(List<Document> documents, Throwable error);
    }

    interface CategoryCallback {
        void onResult(Category category, Throwable error);
    }

    interface TaskCallback {
        void onResult(boolean success, Throwable error);
    }
}
