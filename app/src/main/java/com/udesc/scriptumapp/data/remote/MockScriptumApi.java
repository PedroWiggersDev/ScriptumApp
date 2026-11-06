package com.udesc.scriptumapp.data.remote;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.udesc.scriptumapp.data.remote.ScriptumApi;
import com.udesc.scriptumapp.model.Category;
import com.udesc.scriptumapp.model.Document;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MockScriptumApi implements ScriptumApi {
    private static final String MOCK_DIRECTORY = "mock";
    private final Context context;
    private final File dataDirectory;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public MockScriptumApi(Context context) {
        this(context, new File(context.getFilesDir(), MOCK_DIRECTORY));
    }

    public MockScriptumApi(Context context, File dataDirectory) {
        this.context = context.getApplicationContext();
        this.dataDirectory = dataDirectory;
    }

    @Override
    public void authenticate(String username, String password, AuthCallback callback) {
        execute(() -> {
            JSONArray users = new JSONArray(readAsset("users.json"));
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                if (user.getString("username").equals(username) && user.getString("password").equals(password)) {
                    return new AuthResult("mock-token-" + UUID.randomUUID(), user.getString("username"));
                }
            }
            throw new MockApiException("Usuário ou senha incorretos.");
        }, (result, error) -> callback.onResult(result == null ? null : result.token, result == null ? null : result.username, error));
    }

    @Override
    public void getCategories(String token, CategoriesCallback callback) {
        execute(() -> {
            requireToken(token);
            JSONArray source = new JSONArray(readMockFile("categories.json"));
            JSONArray documents = new JSONArray(readMockFile("documents.json"));
            List<Category> categories = new ArrayList<>();
            for (int i = 0; i < source.length(); i++) {
                JSONObject item = source.getJSONObject(i);
                int count = 0;
                for (int j = 0; j < documents.length(); j++) {
                    if (item.getString("id").equals(documents.getJSONObject(j).getString("categoryId"))) {
                        count++;
                    }
                }
                categories.add(new Category(item.getString("id"), item.getString("title"), count));
            }
            return categories;
        }, callback::onResult);
    }

    @Override
    public void getDocuments(String token, String categoryId, DocumentsCallback callback) {
        execute(() -> {
            requireToken(token);
            JSONArray source = new JSONArray(readMockFile("documents.json"));
            List<Document> documents = new ArrayList<>();
            for (int i = 0; i < source.length(); i++) {
                JSONObject item = source.getJSONObject(i);
                if (item.getString("categoryId").equals(categoryId)) {
                    documents.add(toDocument(item));
                }
            }
            return documents;
        }, callback::onResult);
    }

    @Override
    public void sendScannedTask(String token, String categoryId, String title, String imageBase64, String language, TaskCallback callback) {

        if (callback != null) {
            callback.onResult(true, null);
        }
    }


    @Override
    public void createCategory(String token, String title, CategoryCallback callback) {
        execute(() -> {
            requireToken(token);
            JSONArray categories = new JSONArray(readMockFile("categories.json"));
            for (int i = 0; i < categories.length(); i++) {
                if (categories.getJSONObject(i).getString("title").equalsIgnoreCase(title)) {
                    throw new MockApiException("Já existe uma categoria com esse nome.");
                }
            }
            String id = "cat-" + UUID.randomUUID().toString().substring(0, 8);
            JSONObject item = new JSONObject().put("id", id).put("title", title);
            categories.put(item);
            writeMockFile("categories.json", categories.toString());
            return new Category(id, title, 0);
        }, callback::onResult);
    }

    @Override
    public void sendTask(String token, String categoryId, String documentTitle, String content, TaskCallback callback) {
        execute(() -> {
            requireToken(token);
            JSONArray categories = new JSONArray(readMockFile("categories.json"));
            boolean categoryExists = false;
            for (int i = 0; i < categories.length(); i++) {
                if (categories.getJSONObject(i).getString("id").equals(categoryId)) {
                    categoryExists = true;
                    break;
                }
            }
            if (!categoryExists) {
                throw new MockApiException("A categoria selecionada não foi encontrada.");
            }
            String id = "doc-" + UUID.randomUUID().toString().substring(0, 8);
            JSONObject document = new JSONObject()
                    .put("id", id)
                    .put("categoryId", categoryId)
                    .put("title", documentTitle)
                    .put("summary", content.isEmpty() ? "0 parágrafos" : content.split("\\s+").length + " palavras")
                    .put("status", "DIGITADO")
                    .put("statusDetail", "Mock API")
                    .put("content", content);
            JSONArray documents = new JSONArray(readMockFile("documents.json"));
            documents.put(document);
            writeMockFile("documents.json", documents.toString());
            JSONArray tasks = new JSONArray(readMockFile("tasks.json"));
            tasks.put(new JSONObject()
                    .put("id", "task-" + UUID.randomUUID().toString().substring(0, 8))
                    .put("categoryId", categoryId)
                    .put("documentId", id)
                    .put("title", documentTitle)
                    .put("content", content));
            writeMockFile("tasks.json", tasks.toString());
            return true;
        }, (success, error) -> callback.onResult(Boolean.TRUE.equals(success), error));
    }

    private Document toDocument(JSONObject item) throws JSONException {

        String rawContent = item.optString("content", "");
        List<String> contentList = new ArrayList<>();
        if (!rawContent.isEmpty()) {
            contentList.add(rawContent);
        }

        return new Document(
                item.getString("title"),
                contentList,
                item.optString("status", "DIGITADO"),
                100.0
        );
    }

    private void requireToken(String token) throws MockApiException {
        if (token == null || !token.startsWith("mock-token-")) {
            throw new MockApiException("Sessão inválida. Entre novamente.");
        }
    }

    private String readAsset(String name) throws IOException {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private String readMockFile(String name) throws IOException {
        File directory = dataDirectory;
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Não foi possível preparar os arquivos de dados mock.");
        }
        File file = new File(directory, name);
        if (!file.exists()) {
            writeMockFile(name, readAsset(name));
        }
        try (InputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private void writeMockFile(String name, String contents) throws IOException {
        File directory = dataDirectory;
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("Não foi possível preparar os arquivos de dados mock.");
        }
        File file = new File(directory, name);
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(contents.getBytes(StandardCharsets.UTF_8));
        }
    }

    private <T> void execute(MockOperation<T> operation, MockCallback<T> callback) {
        executor.execute(() -> {
            T result = null;
            Throwable error = null;
            try {
                result = operation.run();
            } catch (IOException | JSONException | MockApiException exception) {
                error = exception;
            }
            T finalResult = result;
            Throwable finalError = error;
            mainHandler.post(() -> callback.onResult(finalResult, finalError));
        });
    }

    private interface MockOperation<T> {
        T run() throws IOException, JSONException, MockApiException;
    }

    private interface MockCallback<T> {
        void onResult(T result, Throwable error);
    }

    private static class AuthResult {
        final String token;
        final String username;

        AuthResult(String token, String username) {
            this.token = token;
            this.username = username;
        }
    }

    private static class MockApiException extends Exception {
        MockApiException(String message) {
            super(message);
        }
    }
}
