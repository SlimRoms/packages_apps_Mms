/*
* Copyright (C) 2015 SlimRoms Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.android.mms.data.slim;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SlimMmsDatabaseHelper extends SQLiteOpenHelper {
    public static final int TRUE = 1;
    public static final int FALSE = 0;
    public static final int DEFAULT = 2;
    public static final String CONVERSATIONS_TABLE = "conversation_settings";
    public static final String CONVERSATIONS_THREAD_ID = "thread_id";
    public static final String CONVERSATIONS_NOTIFICATION_ENABLED = "notification_enabled";
    public static final String CONVERSATIONS_NOTIFICATION_TONE = "notification_tone";
    public static final String CONVERSATIONS_VIBRATE_ENABLED = "vibrate_enabled";
    public static final String CONVERSATIONS_VIBRATE_PATTERN = "vibrate_pattern";
    public static final String[] CONVERSATIONS_COLUMNS = {
            CONVERSATIONS_THREAD_ID,
            CONVERSATIONS_NOTIFICATION_ENABLED,
            CONVERSATIONS_NOTIFICATION_TONE,
            CONVERSATIONS_VIBRATE_ENABLED,
            CONVERSATIONS_VIBRATE_PATTERN
    };
    public static final String DATABASE_CREATE = "CREATE TABLE " + CONVERSATIONS_TABLE + "("
            + CONVERSATIONS_THREAD_ID + " INTEGER PRIMARY KEY, "
            + CONVERSATIONS_NOTIFICATION_ENABLED + " INTEGER, "
            + CONVERSATIONS_NOTIFICATION_TONE + " TEXT, "
            + CONVERSATIONS_VIBRATE_ENABLED + " INTEGER, "
            + CONVERSATIONS_VIBRATE_PATTERN + " TEXT"
            + ");";
    private static final String TAG = "SlimMmsDatabaseHelper";
    private static final String DATABASE_NAME = "slim_mms.db";
    private static final int DATABASE_VERSION = 1;
    private static SlimMmsDatabaseHelper mInstance;

    private SlimMmsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static SlimMmsDatabaseHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new SlimMmsDatabaseHelper(context.getApplicationContext());
        }
        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Creating db");
        db.execSQL(DATABASE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // nothing for now
        Log.d(TAG, "Updating db");
    }

    public void insertSlimConversationSettings(SlimConversationSettings conversationSettings) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONVERSATIONS_THREAD_ID, conversationSettings.mThreadId);
        contentValues.put(CONVERSATIONS_NOTIFICATION_ENABLED, conversationSettings.mNotificationEnabled);
        contentValues.put(CONVERSATIONS_NOTIFICATION_TONE, conversationSettings.mNotificationTone);
        contentValues.put(CONVERSATIONS_VIBRATE_ENABLED, conversationSettings.mVibrateEnabled);
        contentValues.put(CONVERSATIONS_VIBRATE_PATTERN, conversationSettings.mVibratePattern);
        db.insert(CONVERSATIONS_TABLE, null, contentValues);
        db.close();
    }

    // boolean version
    public void updateSlimConversationSettingsField(long threadId, String field, int value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(field, value);
        db.update(CONVERSATIONS_TABLE, contentValues,
                " " + CONVERSATIONS_THREAD_ID + " = ?",
                new String[]{String.valueOf(threadId)});
        db.close();
    }

    // string version
    public void updateSlimConversationSettingsField(long threadId, String field, String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(field, value);
        db.update(CONVERSATIONS_TABLE, contentValues,
                " " + CONVERSATIONS_THREAD_ID + " = ?",
                new String[]{String.valueOf(threadId)});
        db.close();
    }

    public void updateSlimConversationSettings(SlimConversationSettings conversationSettings) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(CONVERSATIONS_NOTIFICATION_ENABLED, conversationSettings.mNotificationEnabled);
        contentValues.put(CONVERSATIONS_NOTIFICATION_TONE, conversationSettings.mNotificationTone);
        contentValues.put(CONVERSATIONS_VIBRATE_ENABLED, conversationSettings.mVibrateEnabled);
        contentValues.put(CONVERSATIONS_VIBRATE_PATTERN, conversationSettings.mVibratePattern);
        db.update(CONVERSATIONS_TABLE, contentValues,
                " " + CONVERSATIONS_THREAD_ID + " = ?",
                new String[]{String.valueOf(conversationSettings.mThreadId)});
    }

    public void deleteSlimConversationSettings(long threadId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(CONVERSATIONS_TABLE,
                " " + CONVERSATIONS_THREAD_ID + " = ?",
                new String[]{String.valueOf(threadId)});
    }

    public void deleteAllSlimConversationSettings() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + CONVERSATIONS_TABLE);
    }
}
