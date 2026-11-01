package com.udesc.scriptumapp.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.udesc.scriptumapp.data.local.LocalDatabase;
import com.udesc.scriptumapp.data.repository.ScriptumRepository;
import com.udesc.scriptumapp.model.Category;
import com.udesc.scriptumapp.model.Document;

import java.util.ArrayList;
import java.util.List;

public class ScriptumViewModel extends AndroidViewModel {
    private final ScriptumRepository repository;
    private final MutableLiveData<Event<OperationResult<String>>> authenticationResult = new MutableLiveData<>();
    private final MutableLiveData<Event<OperationResult<CategoriesData>>> categoriesResult = new MutableLiveData<>();
    private final MutableLiveData<Event<OperationResult<Category>>> categoryCreationResult = new MutableLiveData<>();
    private final MutableLiveData<Event<OperationResult<List<Document>>>> documentsResult = new MutableLiveData<>();
    private final MutableLiveData<Event<OperationResult<Boolean>>> taskSubmissionResult = new MutableLiveData<>();

    public ScriptumViewModel(@NonNull Application application) {
        super(application);
        repository = new ScriptumRepository(application);
    }

    public LiveData<Event<OperationResult<String>>> getAuthenticationResult() {
        return authenticationResult;
    }

    public LiveData<Event<OperationResult<CategoriesData>>> getCategoriesResult() {
        return categoriesResult;
    }

    public LiveData<Event<OperationResult<Category>>> getCategoryCreationResult() {
        return categoryCreationResult;
    }

    public LiveData<Event<OperationResult<List<Document>>>> getDocumentsResult() {
        return documentsResult;
    }

    public LiveData<Event<OperationResult<Boolean>>> getTaskSubmissionResult() {
        return taskSubmissionResult;
    }

    public void authenticate(String username, String password) {
        repository.authenticate(username, password, (token, authenticatedUsername, error) -> {
            if (error != null) {
                authenticationResult.postValue(new Event<>(OperationResult.failure(error)));
                return;
            }
            repository.saveSession(token, authenticatedUsername);
            authenticationResult.postValue(new Event<>(OperationResult.success(authenticatedUsername)));
        });
    }

    public boolean hasSession() {
        return repository.hasSession();
    }

    public String getUsername() {
        LocalDatabase.Session session = repository.getSession();
        return session == null ? null : session.username;
    }

    public void logout() {
        repository.logout();
    }

    public void loadCategories() {
        repository.getCategories((categories, error) -> {
            if (error != null) {
                categoriesResult.postValue(new Event<>(OperationResult.failure(error)));
                return;
            }
            List<Category> recent = new ArrayList<>();
            for (LocalDatabase.RecentCategory saved : repository.getRecentCategories()) {
                for (Category category : categories) {
                    if (category.id.equals(saved.id)) {
                        recent.add(category);
                        break;
                    }
                }
            }
            CategoriesData data = new CategoriesData(categories, recent);
            categoriesResult.postValue(new Event<>(OperationResult.success(data)));
        });
    }

    public void createCategory(String title) {
        repository.createCategory(title, (category, error) -> {
            OperationResult<Category> result = error == null
                    ? OperationResult.success(category)
                    : OperationResult.failure(error);
            categoryCreationResult.postValue(new Event<>(result));
        });
    }

    public void loadDocuments(String categoryId) {
        repository.getDocuments(categoryId, (documents, error) -> {
            OperationResult<List<Document>> result = error == null
                    ? OperationResult.success(documents)
                    : OperationResult.failure(error);
            documentsResult.postValue(new Event<>(result));
        });
    }

    public void submitTask(String categoryId, String title, String content) {
        repository.sendTask(categoryId, title, content, (success, error) -> {
            OperationResult<Boolean> result = error == null
                    ? OperationResult.success(success)
                    : OperationResult.failure(error);
            taskSubmissionResult.postValue(new Event<>(result));
        });
    }

    public void recordViewedCategory(Category category) {
        repository.viewCategory(category);
    }

    static final class CategoriesData {
        final List<Category> categories;
        final List<Category> recentCategories;

        CategoriesData(List<Category> categories, List<Category> recentCategories) {
            this.categories = categories;
            this.recentCategories = recentCategories;
        }
    }
}
