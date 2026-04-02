package com.postpilot.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "PostPilot.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_POSTS = "posts";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_DESC = "description";
    private static final String COLUMN_DATE = "date";
    private static final String COLUMN_PLATFORM = "platform";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_IMAGES = "images"; // Store as comma-separated URIs or JSON

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_POSTS_TABLE = "CREATE TABLE " + TABLE_POSTS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT,"
                + COLUMN_DESC + " TEXT,"
                + COLUMN_DATE + " TEXT,"
                + COLUMN_PLATFORM + " TEXT,"
                + COLUMN_STATUS + " TEXT,"
                + COLUMN_IMAGES + " TEXT" + ")";
        db.execSQL(CREATE_POSTS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_POSTS);
        onCreate(db);
    }

    public long addPost(Post post) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, post.getTitle());
        values.put(COLUMN_DESC, post.getDescription());
        values.put(COLUMN_DATE, post.getDate());
        values.put(COLUMN_PLATFORM, post.getPlatform());
        values.put(COLUMN_STATUS, post.getStatus());
        values.put(COLUMN_IMAGES, post.getImages());
        long id = db.insert(TABLE_POSTS, null, values);
        db.close();
        return id;
    }

    public List<Post> getAllPosts() {
        List<Post> postList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_POSTS + " ORDER BY " + COLUMN_ID + " DESC";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Post post = new Post(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESC)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PLATFORM)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_STATUS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGES))
                );
                post.setId(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID)));
                postList.add(post);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return postList;
    }

    public void deletePost(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_POSTS, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void updatePost(Post post) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, post.getTitle());
        values.put(COLUMN_DESC, post.getDescription());
        values.put(COLUMN_STATUS, post.getStatus());
        values.put(COLUMN_IMAGES, post.getImages());
        db.update(TABLE_POSTS, values, COLUMN_ID + " = ?", new String[]{String.valueOf(post.getId())});
        db.close();
    }
}