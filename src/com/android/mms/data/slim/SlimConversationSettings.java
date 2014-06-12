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
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.mms.ui.MessagingPreferenceActivity;

public class SlimConversationSettings {
    private static final String TAG = "SlimConversationSettings";

    private Context mContext;
    /* package */
    long mThreadId;
    int mNotificationEnabled;
    String mNotificationTone;
    int mVibrateEnabled;
    String mVibratePattern;

    private static final int DEFAULT_NOTIFICATION_ENABLED = SlimMmsDatabaseHelper.DEFAULT;
    private static final String DEFAULT_NOTIFICATION_TONE = "";
    private static final int DEFAULT_VIBRATE_ENABLED = SlimMmsDatabaseHelper.DEFAULT;
    private static final String DEFAULT_VIBRATE_PATTERN = "";

    private SlimConversationSettings(Context context, long threadId, int notificationEnabled,
        String notificationTone, int vibrateEnabled, String vibratePattern) {
        mContext = context;
        mThreadId = threadId;
        mNotificationEnabled = notificationEnabled;
        mNotificationTone = notificationTone;
        mVibrateEnabled = vibrateEnabled;
        mVibratePattern = vibratePattern;
    }

    public static SlimConversationSettings getOrNew(Context context, long threadId) {
        SlimMmsDatabaseHelper helper = SlimMmsDatabaseHelper.getInstance(context);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor cursor = db.query(SlimMmsDatabaseHelper.CONVERSATIONS_TABLE,
            SlimMmsDatabaseHelper.CONVERSATIONS_COLUMNS,
            " thread_id = ?",
            new String[] { String.valueOf(threadId) },
            null, null, null, null);

        // we should only have one result
        int count = cursor.getCount();
        SlimConversationSettings convSetting;
        if (cursor != null && count == 1) {
            cursor.moveToFirst();
            convSetting = new SlimConversationSettings(context,
                threadId,
                cursor.getInt(1),
                cursor.getString(2),
                cursor.getInt(3),
                cursor.getString(4)
            );
        } else if (count > 1) {
            Log.wtf(TAG, "More than one settings with the same thread id is returned!");
            return null;
        } else {
            convSetting = new SlimConversationSettings(context, threadId,
                DEFAULT_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_TONE,
                DEFAULT_VIBRATE_ENABLED, DEFAULT_VIBRATE_PATTERN);

            helper.insertSlimConversationSettings(convSetting);
        }

        return convSetting;
    }

    public static void delete(Context context, long threadId) {
        SlimMmsDatabaseHelper helper = SlimMmsDatabaseHelper.getInstance(context);
        helper.deleteSlimConversationSettings(threadId);
    }

    public static void deleteAll(Context context) {
        SlimMmsDatabaseHelper helper = SlimMmsDatabaseHelper.getInstance(context);
        helper.deleteAllSlimConversationSettings();
    }

    public long getThreadId() {
        return mThreadId;
    }

    public boolean getNotificationEnabled() {
        if (mNotificationEnabled == SlimMmsDatabaseHelper.DEFAULT) {
            SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
            return sharedPreferences.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED,
                DEFAULT_NOTIFICATION_ENABLED == SlimMmsDatabaseHelper.TRUE);
        }
        return mNotificationEnabled == SlimMmsDatabaseHelper.TRUE;
    }

    public void setNotificationEnabled(boolean enabled) {
        mNotificationEnabled = enabled ? SlimMmsDatabaseHelper.TRUE : SlimMmsDatabaseHelper.FALSE;
        setNotificationEnabled(mNotificationEnabled);
    }

    public void setNotificationEnabled(int enabled) {
        SlimMmsDatabaseHelper helper = SlimMmsDatabaseHelper.getInstance(mContext);
        helper.updateSlimConversationSettingsField(mThreadId,
            SlimMmsDatabaseHelper.CONVERSATIONS_NOTIFICATION_ENABLED, enabled);
    }

    public String getNotificationTone() {
        if (mNotificationTone.equals("")) {
            SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
            return sharedPreferences.getString(MessagingPreferenceActivity.NOTIFICATION_RINGTONE,
                null);
        }
        return mNotificationTone;
    }

    public void setNotificationTone(String tone) {
        mNotificationTone = tone;
        SlimMmsDatabaseHelper helper = SlimMmsDatabaseHelper.getInstance(mContext);
        helper.updateSlimConversationSettingsField(mThreadId,
            SlimMmsDatabaseHelper.CONVERSATIONS_NOTIFICATION_TONE, tone);
    }

    public boolean getVibrateEnabled() {
        if (mVibrateEnabled == SlimMmsDatabaseHelper.DEFAULT) {
            SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
            return sharedPreferences.getBoolean(MessagingPreferenceActivity.NOTIFICATION_VIBRATE,
                DEFAULT_VIBRATE_ENABLED == SlimMmsDatabaseHelper.TRUE);
        }
        return mVibrateEnabled == SlimMmsDatabaseHelper.TRUE;
    }

    public void setVibrateEnabled(boolean enabled) {
        mVibrateEnabled = enabled ? SlimMmsDatabaseHelper.TRUE : SlimMmsDatabaseHelper.FALSE;
        setVibrateEnabled(mVibrateEnabled);
    }

    public void setVibrateEnabled(int enabled) {
        SlimMmsDatabaseHelper helper = SlimMmsDatabaseHelper.getInstance(mContext);
        helper.updateSlimConversationSettingsField(mThreadId,
            SlimMmsDatabaseHelper.CONVERSATIONS_VIBRATE_ENABLED, enabled);
    }

    public String getVibratePattern() {
        if (mVibratePattern.equals("")) {
            SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(mContext);
            return sharedPreferences.getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_PATTERN,
                "0,1200");
        }
        return mVibratePattern;
    }

    public void setVibratePattern(String pattern) {
        mVibratePattern = pattern;
        SlimMmsDatabaseHelper helper = SlimMmsDatabaseHelper.getInstance(mContext);
        helper.updateSlimConversationSettingsField(mThreadId,
            SlimMmsDatabaseHelper.CONVERSATIONS_VIBRATE_PATTERN, pattern);
    }

    public void resetToDefault() {
        mNotificationEnabled = DEFAULT_NOTIFICATION_ENABLED;
        mNotificationTone = DEFAULT_NOTIFICATION_TONE;
        mVibrateEnabled = DEFAULT_VIBRATE_ENABLED;
        mVibratePattern = DEFAULT_VIBRATE_PATTERN;
        SlimMmsDatabaseHelper helper = SlimMmsDatabaseHelper.getInstance(mContext);
        helper.updateSlimConversationSettings(this);
    }
}
