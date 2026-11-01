package com.udesc.scriptumapp;

import android.content.Context;
import android.content.ContentValues;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.action.ViewActions;
import androidx.recyclerview.widget.RecyclerView;

import com.udesc.scriptumapp.data.local.LocalDatabase;
import com.udesc.scriptumapp.data.remote.MockScriptumApi;
import com.udesc.scriptumapp.model.Category;
import com.udesc.scriptumapp.model.Document;
import com.udesc.scriptumapp.ui.MainActivity;
import com.udesc.scriptumapp.ui.ScreenFragment;
import com.udesc.scriptumapp.worker.DocumentProcessingWorker;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void mockApiAuthenticatesAndLoadsRemoteFixtures() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockScriptumApi api = new MockScriptumApi(context);
        AuthResult auth = authenticate(api, "admin", "scriptum123");
        assertNotNull(auth.token);
        assertEquals("admin", auth.username);

        AtomicReference<List<Category>> categoriesResult = new AtomicReference<>();
        AtomicReference<Throwable> categoriesError = new AtomicReference<>();
        CountDownLatch categoriesLatch = new CountDownLatch(1);
        api.getCategories(auth.token, (categories, error) -> {
            categoriesResult.set(categories);
            categoriesError.set(error);
            categoriesLatch.countDown();
        });
        assertTrue(categoriesLatch.await(5, TimeUnit.SECONDS));
        assertNull(categoriesError.get());
        assertTrue(categoriesResult.get().size() >= 6);

        AtomicReference<List<Document>> documentsResult = new AtomicReference<>();
        AtomicReference<Throwable> documentsError = new AtomicReference<>();
        CountDownLatch documentsLatch = new CountDownLatch(1);
        api.getDocuments(auth.token, "trabalho", (documents, error) -> {
            documentsResult.set(documents);
            documentsError.set(error);
            documentsLatch.countDown();
        });
        assertTrue(documentsLatch.await(5, TimeUnit.SECONDS));
        assertNull(documentsError.get());
        assertEquals(3, documentsResult.get().size());
    }

    @Test
    public void databaseUpgradeKeepsRecentCategoryIdAndTitle() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = "scriptum-upgrade-test-" + System.currentTimeMillis() + ".db";
        SQLiteOpenHelper versionOne = new SQLiteOpenHelper(context, databaseName, null, 1) {
            @Override
            public void onCreate(SQLiteDatabase database) {
                database.execSQL("CREATE TABLE session (id INTEGER PRIMARY KEY CHECK (id = 1), token TEXT NOT NULL, username TEXT NOT NULL)");
                database.execSQL("CREATE TABLE recent_categories (category_id TEXT PRIMARY KEY, name TEXT NOT NULL, document_count INTEGER NOT NULL, last_opened INTEGER NOT NULL)");
            }

            @Override
            public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
            }
        };
        SQLiteDatabase legacyDatabase = versionOne.getWritableDatabase();
        ContentValues oldCategory = new ContentValues();
        oldCategory.put("category_id", "old-category");
        oldCategory.put("name", "Categoria antiga");
        oldCategory.put("document_count", 8);
        oldCategory.put("last_opened", 1L);
        legacyDatabase.insertOrThrow("recent_categories", null, oldCategory);
        ContentValues recentCategory = new ContentValues(oldCategory);
        recentCategory.put("category_id", "recent-category");
        recentCategory.put("name", "Categoria recente");
        recentCategory.put("last_opened", 2L);
        legacyDatabase.insertOrThrow("recent_categories", null, recentCategory);
        versionOne.close();

        LocalDatabase upgradedDatabase = new LocalDatabase(context, databaseName);
        try {
            List<LocalDatabase.RecentCategory> recent = upgradedDatabase.getRecentCategories(1);
            assertEquals(1, recent.size());
            assertEquals("recent-category", recent.get(0).id);
            assertEquals("Categoria recente", recent.get(0).title);
        } finally {
            upgradedDatabase.close();
            context.deleteDatabase(databaseName);
        }
    }

    @Test
    public void categoryRowsFillWidthAndCreationFieldsHaveSideMargins() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                RecyclerView parent = new RecyclerView(activity);
                parent.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(activity));
                View categoryItem = LayoutInflater.from(activity).inflate(R.layout.item_category, parent, false);
                categoryItem.setLayoutParams(new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, categoryItem.getLayoutParams().width);
                TextView categoryTitle = categoryItem.findViewById(R.id.category_item_title);
                assertEquals(TextUtils.TruncateAt.END, categoryTitle.getEllipsize());
                assertEquals(1, categoryTitle.getMaxLines());

                View categoriesLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_create_category, null, false);
                EditText categoryField = categoriesLayout.findViewById(R.id.category_title);
                LinearLayout.LayoutParams categoryParams = (LinearLayout.LayoutParams) categoryField.getLayoutParams();
                assertTrue(categoryParams.leftMargin > 0);
                assertEquals(categoryParams.leftMargin, categoryParams.rightMargin);

                View documentLayout = LayoutInflater.from(activity).inflate(R.layout.dialog_create_document, null, false);
                EditText documentField = documentLayout.findViewById(R.id.document_title);
                LinearLayout.LayoutParams documentParams = (LinearLayout.LayoutParams) documentField.getLayoutParams();
                assertTrue(documentParams.leftMargin > 0);
                assertEquals(documentParams.leftMargin, documentParams.rightMargin);
                assertEquals(View.GONE, documentLayout.findViewById(R.id.document_manual_panel).getVisibility());
                assertEquals(View.VISIBLE, documentLayout.findViewById(R.id.document_scan_panel).getVisibility());
                assertEquals(View.GONE, documentLayout.findViewById(R.id.document_selected_file).getVisibility());

                Button saveButton = documentLayout.findViewById(R.id.document_save);
                Button cancelButton = documentLayout.findViewById(R.id.document_cancel);
                assertEquals(Color.WHITE, saveButton.getTextColors().getDefaultColor());
                assertEquals(Color.WHITE, cancelButton.getTextColors().getDefaultColor());
                assertEquals(activity.getColor(R.color.scriptum_green),
                        ((GradientDrawable) cancelButton.getBackground()).getColor().getDefaultColor());
                RadioButton scanMode = documentLayout.findViewById(R.id.document_mode_scan);
                ColorStateList radioTint = scanMode.getButtonTintList();
                assertNotNull(radioTint);
                assertEquals(activity.getColor(R.color.scriptum_green),
                        radioTint.getColorForState(
                                new int[]{android.R.attr.state_enabled, android.R.attr.state_checked},
                                Color.TRANSPARENT));
            });
        }
    }

    @Test
    public void sqliteStoresSessionAndOnlyRecentCategoryIdentity() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String databaseName = "scriptum-test-" + System.currentTimeMillis() + ".db";
        LocalDatabase database = new LocalDatabase(context, databaseName);
        try {
            database.saveSession("token-test", "ana");
            LocalDatabase.Session session = database.getSession();
            assertNotNull(session);
            assertEquals("token-test", session.token);
            assertEquals("ana", session.username);

            database.recordViewedCategory("category-test", "Categoria de teste");
            List<LocalDatabase.RecentCategory> recent = database.getRecentCategories(3);
            assertEquals(1, recent.size());
            assertEquals("category-test", recent.get(0).id);
            assertEquals("Categoria de teste", recent.get(0).title);

            try (Cursor cursor = database.getReadableDatabase().rawQuery("PRAGMA table_info(recent_categories)", null)) {
                int columnCount = 0;
                while (cursor.moveToNext()) {
                    columnCount++;
                }
                assertEquals(2, columnCount);
            }

            database.clearSession();
            assertNull(database.getSession());
        } finally {
            database.close();
            context.deleteDatabase(databaseName);
        }
    }

    @Test
    public void formErrorsAreShownInlineOnXmlScreens() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        LocalDatabase sessionDatabase = new LocalDatabase(context);
        sessionDatabase.clearSession();
        sessionDatabase.close();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            Espresso.onView(ViewMatchers.withId(R.id.login_submit)).perform(ViewActions.click());
            Espresso.onView(ViewMatchers.withId(R.id.login_username_error))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Informe o usuário.")));
            Espresso.onView(ViewMatchers.withId(R.id.login_password_error))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Informe a senha.")));

            scenario.onActivity(activity -> activity.showPage(ScreenFragment.CATEGORIES));
            Espresso.onView(ViewMatchers.withId(R.id.category_create)).perform(ViewActions.click());
            Espresso.onView(ViewMatchers.withId(R.id.category_create_submit)).perform(ViewActions.click());
            Espresso.onView(ViewMatchers.withId(R.id.category_title_error))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Informe o nome da categoria.")));
            Espresso.pressBack();

            Bundle arguments = new Bundle();
            arguments.putString("category_id", "trabalho");
            arguments.putString("category_title", "Trabalho");
            scenario.onActivity(activity -> activity.showPage(ScreenFragment.DOCUMENTS, arguments));
            Espresso.onView(ViewMatchers.withId(R.id.document_create)).perform(ViewActions.click());
            Espresso.onView(ViewMatchers.withId(R.id.document_mode_manual)).perform(ViewActions.click());
            Espresso.onView(ViewMatchers.withId(R.id.document_save)).perform(ViewActions.click());
            Espresso.onView(ViewMatchers.withId(R.id.document_title_error))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Informe o título do documento.")));
            Espresso.onView(ViewMatchers.withId(R.id.document_content_error))
                    .check(ViewAssertions.matches(ViewMatchers.withText("Digite o conteúdo do documento.")));
            Espresso.onView(ViewMatchers.withId(R.id.document_scan_panel))
                    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
            Espresso.onView(ViewMatchers.withId(R.id.document_manual_panel))
                    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            Espresso.onView(ViewMatchers.withId(R.id.document_mode_scan)).perform(ViewActions.click());
            Espresso.onView(ViewMatchers.withId(R.id.document_scan_panel))
                    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)));
            Espresso.onView(ViewMatchers.withId(R.id.document_manual_panel))
                    .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        } finally {
            LocalDatabase cleanupDatabase = new LocalDatabase(context);
            cleanupDatabase.clearSession();
            cleanupDatabase.close();
        }
    }

    @Test
    public void mockApiRejectsInvalidCredentials() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        MockScriptumApi api = new MockScriptumApi(context);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> errorResult = new AtomicReference<>();
        api.authenticate("admin", "senha-incorreta", (token, username, error) -> {
            errorResult.set(error);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNotNull(errorResult.get());
    }

    @Test
    public void mockApiCreatesCategoriesAndStoresSubmittedTasks() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File temporaryDirectory = new File(context.getCacheDir(), "mock-api-test-" + System.currentTimeMillis());
        MockScriptumApi api = new MockScriptumApi(context, temporaryDirectory);
        try {
            AuthResult auth = authenticate(api, "ana", "123456");
            AtomicReference<Category> createdCategory = new AtomicReference<>();
            AtomicReference<Throwable> categoryError = new AtomicReference<>();
            CountDownLatch categoryLatch = new CountDownLatch(1);
            api.createCategory(auth.token, "Teste automatizado", (category, error) -> {
                createdCategory.set(category);
                categoryError.set(error);
                categoryLatch.countDown();
            });
            assertTrue(categoryLatch.await(5, TimeUnit.SECONDS));
            assertNull(categoryError.get());
            assertNotNull(createdCategory.get());

            AtomicReference<Boolean> taskCreated = new AtomicReference<>();
            AtomicReference<Throwable> taskError = new AtomicReference<>();
            CountDownLatch taskLatch = new CountDownLatch(1);
            api.sendTask(auth.token, createdCategory.get().id, "Documento de teste", "Conteúdo mock", (success, error) -> {
                taskCreated.set(success);
                taskError.set(error);
                taskLatch.countDown();
            });
            assertTrue(taskLatch.await(5, TimeUnit.SECONDS));
            assertNull(taskError.get());
            assertEquals(Boolean.TRUE, taskCreated.get());

            AtomicReference<List<Document>> documents = new AtomicReference<>();
            AtomicReference<Throwable> documentsError = new AtomicReference<>();
            CountDownLatch documentsLatch = new CountDownLatch(1);
            api.getDocuments(auth.token, createdCategory.get().id, (result, error) -> {
                documents.set(result);
                documentsError.set(error);
                documentsLatch.countDown();
            });
            assertTrue(documentsLatch.await(5, TimeUnit.SECONDS));
            assertNull(documentsError.get());
            assertEquals(1, documents.get().size());
            assertEquals("Documento de teste", documents.get().get(0).title);
            File taskFile = new File(temporaryDirectory, "tasks.json");
            assertTrue(taskFile.exists());
            assertTrue(new String(Files.readAllBytes(taskFile.toPath()), StandardCharsets.UTF_8).contains("Documento de teste"));
        } finally {
            File[] files = temporaryDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    file.delete();
                }
            }
            temporaryDirectory.delete();
        }
    }

    private AuthResult authenticate(MockScriptumApi api, String username, String password) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AuthResult> result = new AtomicReference<>();
        AtomicReference<Throwable> errorResult = new AtomicReference<>();
        api.authenticate(username, password, (token, authenticatedUsername, error) -> {
            result.set(new AuthResult(token, authenticatedUsername));
            errorResult.set(error);
            latch.countDown();
        });
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertNull(errorResult.get());
        return result.get();
    }

    private static class AuthResult {
        final String token;
        final String username;

        AuthResult(String token, String username) {
            this.token = token;
            this.username = username;
        }
    }
}
