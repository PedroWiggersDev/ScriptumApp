package com.udesc.scriptumapp.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "scriptum.db";
    private static final int DATABASE_VERSION = 2;
    private static final String SESSION_TABLE = "session";
    private static final String RECENT_CATEGORIES_TABLE = "recent_categories";

    public LocalDatabase(Context context) {
        this(context, DATABASE_NAME);
    }

    public LocalDatabase(Context context, String databaseName) {
        super(context, databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + SESSION_TABLE + " (id INTEGER PRIMARY KEY CHECK (id = 1), token TEXT NOT NULL, username TEXT NOT NULL)");
        createRecentCategoriesTable(database);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            database.execSQL("ALTER TABLE " + RECENT_CATEGORIES_TABLE + " RENAME TO recent_categories_legacy");
            createRecentCategoriesTable(database);
            database.execSQL("INSERT INTO " + RECENT_CATEGORIES_TABLE + " (category_id, title) SELECT category_id, name FROM recent_categories_legacy ORDER BY last_opened ASC");
            database.execSQL("DROP TABLE recent_categories_legacy");
        }
    }

    private void createRecentCategoriesTable(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + RECENT_CATEGORIES_TABLE + " (category_id TEXT PRIMARY KEY, title TEXT NOT NULL)");
    }

    public void saveSession(String token, String username) {
        ContentValues values = new ContentValues();
        values.put("id", 1);
        values.put("token", token);
        values.put("username", username);
        long rowId = getWritableDatabase().insertWithOnConflict(SESSION_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            throw new IllegalStateException("Não foi possível salvar a sessão local.");
        }
    }

    public Session getSession() {
        try (Cursor cursor = getReadableDatabase().query(SESSION_TABLE, new String[]{"token", "username"}, "id = ?", new String[]{"1"}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new Session(cursor.getString(0), cursor.getString(1));
            }
        }
        return null;
    }

    public void clearSession() {
        getWritableDatabase().delete(SESSION_TABLE, null, null);
    }

    public void recordViewedCategory(String categoryId, String title) {
        ContentValues values = new ContentValues();
        values.put("category_id", categoryId);
        values.put("title", title);
        long rowId = getWritableDatabase().insertWithOnConflict(RECENT_CATEGORIES_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            throw new IllegalStateException("Não foi possível salvar a categoria recente.");
        }
    }

    public List<RecentCategory> getRecentCategories(int limit) {
        List<RecentCategory> categories = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                RECENT_CATEGORIES_TABLE,
                new String[]{"category_id", "title"},
                null,
                null,
                null,
                null,
                "rowid DESC",
                String.valueOf(limit))) {
            while (cursor.moveToNext()) {
                categories.add(new RecentCategory(
                        cursor.getString(0),
                        cursor.getString(1)));
            }
        }
        return categories;
    }

    public static class RecentCategory {
        public final String id;
        public final String title;

        public RecentCategory(String id, String title) {
            this.id = id;
            this.title = title;
        }
    }

    public static class Session {
        public final String token;
        public final String username;

        public Session(String token, String username) {
            this.token = token;
            this.username = username;
        }
    }
}
