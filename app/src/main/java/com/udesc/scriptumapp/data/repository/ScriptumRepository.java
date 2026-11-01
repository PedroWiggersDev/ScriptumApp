package com.udesc.scriptumapp.data.repository;

import android.content.Context;

import com.udesc.scriptumapp.data.local.LocalDatabase;
import com.udesc.scriptumapp.data.remote.ScriptumApi;
import com.udesc.scriptumapp.data.remote.ScriptumApiFactory;
import com.udesc.scriptumapp.model.Category;

import java.util.List;

public class ScriptumRepository {
    private final LocalDatabase database;
    private final ScriptumApi api;

    public ScriptumRepository(Context context) {
        this(context, ScriptumApiFactory.create(context));
    }

    ScriptumRepository(Context context, ScriptumApi api) {
        Context applicationContext = context.getApplicationContext();
        database = new LocalDatabase(applicationContext);
        this.api = api;
    }

    public LocalDatabase.Session getSession() {
        return database.getSession();
    }

    public boolean hasSession() {
        return getSession() != null;
    }

    public void saveSession(String token, String username) {
        database.saveSession(token, username);
    }

    public void logout() {
        database.clearSession();
    }

    /** Autentica o usuário e entrega o resultado ao fluxo de login. */
    public void authenticate(String username, String password, ScriptumApi.AuthCallback callback) {
        api.authenticate(username, password, callback);
    }

    /** Busca o catálogo atualizado; o SQLite é usado somente para IDs e títulos recentes. */
    public void getCategories(ScriptumApi.CategoriesCallback callback) {
        LocalDatabase.Session session = getSession();
        api.getCategories(session == null ? null : session.token, callback);
    }

    /** Busca os documentos remotos pertencentes a uma categoria. */
    public void getDocuments(String categoryId, ScriptumApi.DocumentsCallback callback) {
        LocalDatabase.Session session = getSession();
        api.getDocuments(session == null ? null : session.token, categoryId, callback);
    }

    /** Solicita a criação da categoria ao serviço e não altera o catálogo localmente. */
    public void createCategory(String title, ScriptumApi.CategoryCallback callback) {
        LocalDatabase.Session session = getSession();
        api.createCategory(session == null ? null : session.token, title, callback);
    }

    /** Envia o documento como uma tarefa para o serviço associado à categoria selecionada. */
    public void sendTask(String categoryId, String title, String content, ScriptumApi.TaskCallback callback) {
        LocalDatabase.Session session = getSession();
        api.sendTask(session == null ? null : session.token, categoryId, title, content, callback);
    }

    public List<LocalDatabase.RecentCategory> getRecentCategories() {
        return database.getRecentCategories(3);
    }

    public void viewCategory(Category category) {
        database.recordViewedCategory(category.id, category.name);
    }

}
