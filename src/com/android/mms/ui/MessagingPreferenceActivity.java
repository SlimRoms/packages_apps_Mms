/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.ui;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.provider.SearchRecentSuggestions;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Log;

import com.android.internal.telephony.IccCardConstants;
import com.android.internal.telephony.TelephonyIntents;

import com.android.mms.MmsApp;
import com.android.mms.MmsConfig;
import com.android.mms.R;
import com.android.mms.transaction.TransactionService;
import com.android.mms.util.Recycler;
import java.util.ArrayList;


/**
 * With this activity, users can set preferences for MMS and SMS and
 * can access and manipulate SMS messages stored on the SIM.
 */
public class MessagingPreferenceActivity extends PreferenceActivity
            implements OnPreferenceChangeListener {
    private static final String TAG = "MessagingPreferenceActivity";
    // Symbolic names for the keys used for preference lookup
    public static final String MMS_DELIVERY_REPORT_MODE = "pref_key_mms_delivery_reports";
    public static final String EXPIRY_TIME              = "pref_key_mms_expiry";
    public static final String PRIORITY                 = "pref_key_mms_priority";
    public static final String READ_REPORT_MODE         = "pref_key_mms_read_reports";
    public static final String SMS_DELIVERY_REPORT_MODE = "pref_key_sms_delivery_reports";
    public static final String SMS_DELIVERY_REPORT_PHONE1 = "pref_key_sms_delivery_reports_slot1";
    public static final String SMS_DELIVERY_REPORT_PHONE2 = "pref_key_sms_delivery_reports_slot2";
    public static final String NOTIFICATION_ENABLED     = "pref_key_enable_notifications";
    public static final String NOTIFICATION_VIBRATE     = "pref_key_vibrate";
    public static final String NOTIFICATION_VIBRATE_WHEN= "pref_key_vibrateWhen";
    public static final String NOTIFICATION_RINGTONE    = "pref_key_ringtone";
    public static final String AUTO_RETRIEVAL           = "pref_key_mms_auto_retrieval";
    public static final String RETRIEVAL_DURING_ROAMING = "pref_key_mms_retrieval_during_roaming";
    public static final String AUTO_DELETE              = "pref_key_auto_delete";
    public static final String GROUP_MMS_MODE           = "pref_key_mms_group_mms";

    // QuickMessage
    public static final String QUICKMESSAGE_ENABLED      = "pref_key_quickmessage";
    public static final String QM_LOCKSCREEN_ENABLED     = "pref_key_qm_lockscreen";
    public static final String QM_CLOSE_ALL_ENABLED      = "pref_key_close_all";
    public static final String QM_DARK_THEME_ENABLED     = "pref_dark_theme";

    // Vibrate pattern
    public static final String NOTIFICATION_VIBRATE_PATTERN =
            "pref_key_mms_notification_vibrate_pattern";

    // Menu entries
    private static final int MENU_RESTORE_DEFAULTS    = 1;

    // Preferences for enabling and disabling SMS
    private Preference mSmsDisabledPref;
    private Preference mSmsEnabledPref;

    private PreferenceCategory mStoragePrefCategory;
    private PreferenceCategory mSmsPrefCategory;
    private PreferenceCategory mMmsPrefCategory;
    private PreferenceCategory mNotificationPrefCategory;
    private PreferenceCategory mSmscPrefCate;

    private Preference mSmsLimitPref;
    private Preference mSmsDeliveryReportPref;
    private Preference mSmsDeliveryReportPrefPhone1;
    private Preference mSmsDeliveryReportPrefPhone2;
    private Preference mMmsLimitPref;
    private Preference mMmsDeliveryReportPref;
    private Preference mMmsGroupMmsPref;
    private Preference mMmsReadReportPref;
    private Preference mManageSimPref;
    private Preference mManageSim1Pref;
    private Preference mManageSim2Pref;
    private Preference mClearHistoryPref;
    private CheckBoxPreference mVibratePref;
    private CheckBoxPreference mEnableNotificationsPref;
    private CheckBoxPreference mMmsAutoRetrievialPref;
    private RingtonePreference mRingtonePref;
    private ListPreference mSmsStorePref;
    private ListPreference mSmsStoreSim1Pref;
    private ListPreference mSmsStoreSim2Pref;
    private ListPreference mSmsStoreCard1Pref;
    private ListPreference mSmsStoreCard2Pref;
    private ListPreference mSmsValidityPref;
    private ListPreference mSmsValidityCard1Pref;
    private ListPreference mSmsValidityCard2Pref;
    private Recycler mSmsRecycler;
    private Recycler mMmsRecycler;
    private static ArrayList<Preference> mSmscPrefList = new ArrayList<Preference>();
    private static final int CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG = 3;

    // Whether or not we are currently enabled for SMS. This field is updated in onResume to make
    // sure we notice if the user has changed the default SMS app.
    private boolean mIsSmsEnabled;
    private static final String SMSC_DIALOG_TITLE = "title";
    private static final String SMSC_DIALOG_NUMBER = "smsc";
    private static final String SMSC_DIALOG_PHONE_ID = "phoneid";

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
                PreferenceCategory smsCategory =
                        (PreferenceCategory)findPreference("pref_key_sms_settings");
                if (smsCategory != null) {
                    updateSimSmsPref();
                }
            } else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                    updateSmscPref();
            }
        }
    };

    // QuickMessage
    private CheckBoxPreference mEnableQuickMessagePref;
    private CheckBoxPreference mEnableQmLockscreenPref;
    private CheckBoxPreference mEnableQmCloseAllPref;
    private CheckBoxPreference mEnableQmDarkThemePref;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        loadPrefs();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean isSmsEnabled = MmsConfig.isSmsEnabled(this);
        if (isSmsEnabled != mIsSmsEnabled) {
            mIsSmsEnabled = isSmsEnabled;
            invalidateOptionsMenu();
        }

        // Since the enabled notifications pref can be changed outside of this activity,
        // we have to reload it whenever we resume.
        setEnabledNotificationsPref();
        registerListeners();
        updateSmsEnabledState();
        updateSmscPref();
    }

    private void updateSmsEnabledState() {
        // Show the right pref (SMS Disabled or SMS Enabled)
        PreferenceScreen prefRoot = (PreferenceScreen)findPreference("pref_key_root");
        if (!mIsSmsEnabled) {
            prefRoot.addPreference(mSmsDisabledPref);
            prefRoot.removePreference(mSmsEnabledPref);
        } else {
            prefRoot.removePreference(mSmsDisabledPref);
            prefRoot.addPreference(mSmsEnabledPref);
        }

        // Enable or Disable the settings as appropriate
        mStoragePrefCategory.setEnabled(mIsSmsEnabled);
        mSmsPrefCategory.setEnabled(mIsSmsEnabled);
        mMmsPrefCategory.setEnabled(mIsSmsEnabled);
        mNotificationPrefCategory.setEnabled(mIsSmsEnabled);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSmscPrefList.clear();
        mSmscPrefCate.removeAll();
    }

    private void loadPrefs() {
        addPreferencesFromResource(R.xml.preferences);

        mSmsDisabledPref = findPreference("pref_key_sms_disabled");
        mSmsEnabledPref = findPreference("pref_key_sms_enabled");

        mStoragePrefCategory = (PreferenceCategory)findPreference("pref_key_storage_settings");
        mSmsPrefCategory = (PreferenceCategory)findPreference("pref_key_sms_settings");
        mMmsPrefCategory = (PreferenceCategory)findPreference("pref_key_mms_settings");
        mNotificationPrefCategory =
                (PreferenceCategory)findPreference("pref_key_notification_settings");

        mManageSimPref = findPreference("pref_key_manage_sim_messages");
        mManageSim1Pref = findPreference("pref_key_manage_sim_messages_slot1");
        mManageSim2Pref = findPreference("pref_key_manage_sim_messages_slot2");
        mSmsLimitPref = findPreference("pref_key_sms_delete_limit");
        mSmsDeliveryReportPref = findPreference("pref_key_sms_delivery_reports");
        mSmsDeliveryReportPrefPhone1 = findPreference("pref_key_sms_delivery_reports_slot1");
        mSmsDeliveryReportPrefPhone2 = findPreference("pref_key_sms_delivery_reports_slot2");
        mMmsDeliveryReportPref = findPreference("pref_key_mms_delivery_reports");
        mMmsGroupMmsPref = findPreference("pref_key_mms_group_mms");
        mMmsReadReportPref = findPreference("pref_key_mms_read_reports");
        mMmsLimitPref = findPreference("pref_key_mms_delete_limit");
        mClearHistoryPref = findPreference("pref_key_mms_clear_history");
        mEnableNotificationsPref = (CheckBoxPreference) findPreference(NOTIFICATION_ENABLED);
        mMmsAutoRetrievialPref = (CheckBoxPreference) findPreference(AUTO_RETRIEVAL);
        mVibratePref = (CheckBoxPreference) findPreference(NOTIFICATION_VIBRATE);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibratePref != null && (vibrator == null || !vibrator.hasVibrator())) {
            mNotificationPrefCategory.removePreference(mVibratePref);
            mVibratePref = null;
        }
        mRingtonePref = (RingtonePreference) findPreference(NOTIFICATION_RINGTONE);
        mSmsStorePref = (ListPreference) findPreference("pref_key_sms_store");
        mSmsStoreSim1Pref = (ListPreference) findPreference("pref_key_sms_store_sim1");
        mSmsStoreSim2Pref = (ListPreference) findPreference("pref_key_sms_store_sim2");
        mSmsStoreCard1Pref = (ListPreference) findPreference("pref_key_sms_store_card1");
        mSmsStoreCard2Pref = (ListPreference) findPreference("pref_key_sms_store_card2");
        mSmsValidityPref = (ListPreference) findPreference("pref_key_sms_validity_period");
        mSmsValidityCard1Pref
            = (ListPreference) findPreference("pref_key_sms_validity_period_slot1");
        mSmsValidityCard2Pref
            = (ListPreference) findPreference("pref_key_sms_validity_period_slot2");

        // QuickMessage
        mEnableQuickMessagePref = (CheckBoxPreference) findPreference(QUICKMESSAGE_ENABLED);
        mEnableQmLockscreenPref = (CheckBoxPreference) findPreference(QM_LOCKSCREEN_ENABLED);
        mEnableQmCloseAllPref = (CheckBoxPreference) findPreference(QM_CLOSE_ALL_ENABLED);
        mEnableQmDarkThemePref = (CheckBoxPreference) findPreference(QM_DARK_THEME_ENABLED);

        setMessagePreferences();
    }

    private void restoreDefaultPreferences() {
        PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
        setPreferenceScreen(null);
        // Reset the SMSC preference.
        mSmscPrefList.clear();
        mSmscPrefCate.removeAll();
        loadPrefs();
        updateSmsEnabledState();

        // NOTE: After restoring preferences, the auto delete function (i.e. message recycler)
        // will be turned off by default. However, we really want the default to be turned on.
        // Because all the prefs are cleared, that'll cause:
        // ConversationList.runOneTimeStorageLimitCheckForLegacyMessages to get executed the
        // next time the user runs the Messaging app and it will either turn on the setting
        // by default, or if the user is over the limits, encourage them to turn on the setting
        // manually.
    }

    private void setMessagePreferences() {

        boolean showSmsc = getResources().getBoolean(R.bool.config_show_smsc_pref);
        mSmscPrefCate = (PreferenceCategory) findPreference("pref_key_smsc");
        if (showSmsc) {
            showSmscPref();
        } else if (mSmscPrefCate != null) {
            getPreferenceScreen().removePreference(mSmscPrefCate);
        }
        // Set SIM card SMS management preference
        updateSimSmsPref();

        if (!MmsConfig.getSMSDeliveryReportsEnabled()) {
            mSmsPrefCategory.removePreference(mSmsDeliveryReportPref);
            mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefPhone1);
            mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefPhone2);
            if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                getPreferenceScreen().removePreference(mSmsPrefCategory);
            }
        } else {
            if (TelephonyManager.getDefault().isMultiSimEnabled()) {
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPref);
            } else {
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefPhone1);
                mSmsPrefCategory.removePreference(mSmsDeliveryReportPrefPhone2);
            }
        }

        setMmsRelatedPref();

        setEnabledNotificationsPref();

        if (getResources().getBoolean(R.bool.config_savelocation)) {
            if (MessageUtils.isMultiSimEnabled()) {
                PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
                storageOptions.removePreference(mSmsStorePref);

                if (!MessageUtils.hasIccCard(MessageUtils.PHONE1)) {
                    storageOptions.removePreference(mSmsStoreSim1Pref);
                } else {
                    setSmsPreferStoreSummary(MessageUtils.PHONE1);
                }
                if (!MessageUtils.hasIccCard(MessageUtils.PHONE2)) {
                    storageOptions.removePreference(mSmsStoreSim2Pref);
                } else {
                    setSmsPreferStoreSummary(MessageUtils.PHONE2);
                }
            } else {
                PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
                storageOptions.removePreference(mSmsStoreSim1Pref);
                storageOptions.removePreference(mSmsStoreSim2Pref);

                if (!MmsApp.getApplication().getTelephonyManager().hasIccCard()) {
                    storageOptions.removePreference(mSmsStorePref);
                } else {
                    setSmsPreferStoreSummary();
                }
            }
        } else {
            PreferenceCategory storageOptions =
                    (PreferenceCategory)findPreference("pref_key_storage_settings");
            storageOptions.removePreference(mSmsStorePref);
            storageOptions.removePreference(mSmsStoreSim1Pref);
            storageOptions.removePreference(mSmsStoreSim2Pref);
        }
        setSmsValidityPeriodPref();

        // If needed, migrate vibration setting from the previous tri-state setting stored in
        // NOTIFICATION_VIBRATE_WHEN to the boolean setting stored in NOTIFICATION_VIBRATE.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (mVibratePref != null && sharedPreferences.contains(NOTIFICATION_VIBRATE_WHEN)) {
            String vibrateWhen = sharedPreferences.
                    getString(MessagingPreferenceActivity.NOTIFICATION_VIBRATE_WHEN, null);
            boolean vibrate = "always".equals(vibrateWhen);
            SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
            prefsEditor.putBoolean(NOTIFICATION_VIBRATE, vibrate);
            prefsEditor.remove(NOTIFICATION_VIBRATE_WHEN);  // remove obsolete setting
            prefsEditor.apply();
            mVibratePref.setChecked(vibrate);
        }

        mSmsRecycler = Recycler.getSmsRecycler();
        mMmsRecycler = Recycler.getMmsRecycler();

        // Fix up the recycler's summary with the correct values
        setSmsDisplayLimit();
        setMmsDisplayLimit();

        String soundValue = sharedPreferences.getString(NOTIFICATION_RINGTONE, null);
        setRingtoneSummary(soundValue);
    }

    private void setMmsRelatedPref() {
        if (!MmsConfig.getMmsEnabled()) {
            // No Mms, remove all the mms-related preferences
            getPreferenceScreen().removePreference(mMmsPrefCategory);

            mStoragePrefCategory.removePreference(findPreference("pref_key_mms_delete_limit"));
        } else {
            if (!MmsConfig.getMMSDeliveryReportsEnabled()) {
                mMmsPrefCategory.removePreference(mMmsDeliveryReportPref);
            }
            if (!MmsConfig.getMMSReadReportsEnabled()) {
                mMmsPrefCategory.removePreference(mMmsReadReportPref);
            }
            // If the phone's SIM doesn't know it's own number, disable group mms.
            if (!MmsConfig.getGroupMmsEnabled() ||
                    TextUtils.isEmpty(MessageUtils.getLocalNumber())) {
                mMmsPrefCategory.removePreference(mMmsGroupMmsPref);
            }
        }
    }

    private void setSmsValidityPeriodPref() {
        PreferenceCategory storageOptions =
                (PreferenceCategory)findPreference("pref_key_sms_settings");
        if (getResources().getBoolean(R.bool.config_sms_validity)) {
            if (MessageUtils.isMultiSimEnabled ()) {
                storageOptions.removePreference(mSmsValidityPref);
                setSmsPreferValiditySummary(MessageUtils.PHONE1);
                setSmsPreferValiditySummary(MessageUtils.PHONE2);
            } else {
                storageOptions.removePreference(mSmsValidityCard1Pref);
                storageOptions.removePreference(mSmsValidityCard2Pref);
                setSmsPreferValiditySummary(MessageUtils.PHONE_SINGLE_MODE);
            }
        } else {
            storageOptions.removePreference(mSmsValidityPref);
            storageOptions.removePreference(mSmsValidityCard1Pref);
            storageOptions.removePreference(mSmsValidityCard2Pref);
        }

        // QuickMessage
        setEnabledQuickMessagePref();
        setEnabledQmLockscreenPref();
        setEnabledQmCloseAllPref();
        setEnabledQmDarkThemePref();
    }

    private void setRingtoneSummary(String soundValue) {
        Uri soundUri = TextUtils.isEmpty(soundValue) ? null : Uri.parse(soundValue);
        Ringtone tone = soundUri != null ? RingtoneManager.getRingtone(this, soundUri) : null;
        mRingtonePref.setSummary(tone != null ? tone.getTitle(this)
                : getResources().getString(R.string.silent_ringtone));
    }

    private void showSmscPref() {
        int count = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < count; i++) {
            final Preference pref = new Preference(this);
            pref.setKey(String.valueOf(i));
            pref.setTitle(getSmscDialogTitle(count, i));

            pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    MyEditDialogFragment dialog = MyEditDialogFragment.newInstance(
                            MessagingPreferenceActivity.this,
                            preference.getTitle(),
                            preference.getSummary(),
                            Integer.valueOf(preference.getKey()));
                    dialog.show(getFragmentManager(), "dialog");
                    return true;
                }
            });

            mSmscPrefCate.addPreference(pref);
            mSmscPrefList.add(pref);
        }
        updateSmscPref();
    }

    private boolean isAirPlaneModeOn() {
        return Settings.System.getInt(getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }

    private String getSmscDialogTitle(int count, int index) {
        String title = TelephonyManager.getDefault().isMultiSimEnabled()
                ? getString(R.string.pref_more_smcs, index + 1)
                : getString(R.string.pref_one_smcs);
        return title;
    }

    private void setSmsPreferStoreSummary() {
        mSmsStorePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String summary = newValue.toString();
                int index = mSmsStorePref.findIndexOfValue(summary);
                mSmsStorePref.setSummary(mSmsStorePref.getEntries()[index]);
                mSmsStorePref.setValue(summary);
                return true;
            }
        });
        mSmsStorePref.setSummary(mSmsStorePref.getEntry());
    }

    private void setSmsPreferStoreSummary(int phoneId) {
        if (MessageUtils.PHONE1 == phoneId) {
            mSmsStoreSim1Pref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mSmsStoreSim1Pref.findIndexOfValue(summary);
                    mSmsStoreSim1Pref.setSummary(mSmsStoreSim1Pref.getEntries()[index]);
                    mSmsStoreSim1Pref.setValue(summary);
                    return false;
                }
            });
            mSmsStoreSim1Pref.setSummary(mSmsStoreSim1Pref.getEntry());
        } else {
            mSmsStoreSim2Pref.setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String summary = newValue.toString();
                    int index = mSmsStoreSim2Pref.findIndexOfValue(summary);
                    mSmsStoreSim2Pref.setSummary(mSmsStoreSim2Pref.getEntries()[index]);
                    mSmsStoreSim2Pref.setValue(summary);
                    return false;
                }
            });
            mSmsStoreSim2Pref.setSummary(mSmsStoreSim2Pref.getEntry());
        }
    }

    private void setSmsPreferValiditySummary(int phoneId) {
        switch (phoneId) {
            case MessageUtils.PHONE_SINGLE_MODE:
                mSmsValidityPref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String summary = newValue.toString();
                        int index = mSmsValidityPref.findIndexOfValue(summary);
                        mSmsValidityPref.setSummary(mSmsValidityPref.getEntries()[index]);
                        mSmsValidityPref.setValue(summary);
                        return true;
                    }
                });
                mSmsValidityPref.setSummary(mSmsValidityPref.getEntry());
                break;
            case MessageUtils.PHONE1:
                mSmsValidityCard1Pref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String summary = newValue.toString();
                        int index = mSmsValidityCard1Pref.findIndexOfValue(summary);
                        mSmsValidityCard1Pref.setSummary(mSmsValidityCard1Pref.getEntries()[index]);
                        mSmsValidityCard1Pref.setValue(summary);
                        return true;
                    }
                });
                mSmsValidityCard1Pref.setSummary(mSmsValidityCard1Pref.getEntry());
                break;
            case MessageUtils.PHONE2:
                mSmsValidityCard2Pref.setOnPreferenceChangeListener(
                        new Preference.OnPreferenceChangeListener() {
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String summary = newValue.toString();
                        int index = mSmsValidityCard2Pref.findIndexOfValue(summary);
                        mSmsValidityCard2Pref.setSummary(mSmsValidityCard2Pref.getEntries()[index]);
                        mSmsValidityCard2Pref.setValue(summary);
                        return true;
                    }
                });
                mSmsValidityCard2Pref.setSummary(mSmsValidityCard2Pref.getEntry());
                break;
            default:
                break;
        }
    }

    private void updateSimSmsPref() {
        if (MessageUtils.isMultiSimEnabled()) {
            if (!MessageUtils.hasIccCard(MessageUtils.PHONE1)) {
                mSmsPrefCategory.removePreference(mManageSim1Pref);
            }
            if (!MessageUtils.hasIccCard(MessageUtils.PHONE2)) {
                mSmsPrefCategory.removePreference(mManageSim2Pref);
            }
            mSmsPrefCategory.removePreference(mManageSimPref);
        } else {
            if (!MessageUtils.hasIccCard(MessageUtils.PHONE_DEFAULT)) {
                mSmsPrefCategory.removePreference(mManageSimPref);
            }
            mSmsPrefCategory.removePreference(mManageSim1Pref);
            mSmsPrefCategory.removePreference(mManageSim2Pref);
        }
    }

    private void setEnabledNotificationsPref() {
        // The "enable notifications" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableNotificationsPref.setChecked(getNotificationEnabled(this));
    }

    private void setEnabledQuickMessagePref() {
        // The "enable quickmessage" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableQuickMessagePref.setChecked(getQuickMessageEnabled(this));
    }

    private void setEnabledQmLockscreenPref() {
        // The "enable quickmessage on lock screen " setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableQmLockscreenPref.setChecked(getQmLockscreenEnabled(this));
    }

    private void setEnabledQmCloseAllPref() {
        // The "enable close all" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableQmCloseAllPref.setChecked(getQmCloseAllEnabled(this));
    }

    private void setEnabledQmDarkThemePref() {
        // The "Use dark theme" setting is really stored in our own prefs. Read the
        // current value and set the checkbox to match.
        mEnableQmDarkThemePref.setChecked(getQmDarkThemeEnabled(this));
    }

    private void setSmsDisplayLimit() {
        mSmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mSmsRecycler.getMessageLimit(this)));
    }

    private void setMmsDisplayLimit() {
        mMmsLimitPref.setSummary(
                getString(R.string.pref_summary_delete_limit,
                        mMmsRecycler.getMessageLimit(this)));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.clear();
        if (mIsSmsEnabled) {
            menu.add(0, MENU_RESTORE_DEFAULTS, 0, R.string.restore_default);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESTORE_DEFAULTS:
                restoreDefaultPreferences();
                return true;

            case android.R.id.home:
                // The user clicked on the Messaging icon in the action bar. Take them back from
                // wherever they came from
                finish();
                return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mSmsLimitPref) {
            new NumberPickerDialog(this,
                    mSmsLimitListener,
                    mSmsRecycler.getMessageLimit(this),
                    mSmsRecycler.getMessageMinLimit(),
                    mSmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_sms_delete).show();
        } else if (preference == mMmsLimitPref) {
            new NumberPickerDialog(this,
                    mMmsLimitListener,
                    mMmsRecycler.getMessageLimit(this),
                    mMmsRecycler.getMessageMinLimit(),
                    mMmsRecycler.getMessageMaxLimit(),
                    R.string.pref_title_mms_delete).show();
        } else if (preference == mManageSimPref) {
            startActivity(new Intent(this, ManageSimMessages.class));
        } else if (preference == mManageSim1Pref) {
            Intent intent = new Intent(this, ManageSimMessages.class);
            intent.putExtra(MessageUtils.PHONE_KEY, MessageUtils.PHONE1);
            startActivity(intent);
        } else if (preference == mManageSim2Pref) {
            Intent intent = new Intent(this, ManageSimMessages.class);
            intent.putExtra(MessageUtils.PHONE_KEY, MessageUtils.PHONE2);
            startActivity(intent);
        } else if (preference == mClearHistoryPref) {
            showDialog(CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG);
            return true;
        } else if (preference == mEnableNotificationsPref) {
            // Update the actual "enable notifications" value that is stored in secure settings.
            enableNotifications(mEnableNotificationsPref.isChecked(), this);
        } else if (preference == mEnableQuickMessagePref) {
            // Update the actual "enable quickmessage" value that is stored in secure settings.
            enableQuickMessage(mEnableQuickMessagePref.isChecked(), this);
        } else if (preference == mEnableQmLockscreenPref) {
            // Update the actual "enable quickmessage on lockscreen" value that is stored in secure settings.
            enableQmLockscreen(mEnableQmLockscreenPref.isChecked(), this);
        } else if (preference == mEnableQmCloseAllPref) {
            // Update the actual "enable close all" value that is stored in secure settings.
            enableQmCloseAll(mEnableQmCloseAllPref.isChecked(), this);
        } else if (preference == mEnableQmDarkThemePref) {
            // Update the actual "enable dark theme" value that is stored in secure settings.
            enableQmDarkTheme(mEnableQmDarkThemePref.isChecked(), this);
        } else if (preference == mMmsAutoRetrievialPref) {
            if (mMmsAutoRetrievialPref.isChecked()) {
                startMmsDownload();
            }
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /**
     * Trigger the TransactionService to download any outstanding messages.
     */
    private void startMmsDownload() {
        startService(new Intent(TransactionService.ACTION_ENABLE_AUTO_RETRIEVE, null, this,
                TransactionService.class));
    }

    NumberPickerDialog.OnNumberSetListener mSmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                mSmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setSmsDisplayLimit();
            }
    };

    NumberPickerDialog.OnNumberSetListener mMmsLimitListener =
        new NumberPickerDialog.OnNumberSetListener() {
            public void onNumberSet(int limit) {
                mMmsRecycler.setMessageLimit(MessagingPreferenceActivity.this, limit);
                setMmsDisplayLimit();
            }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case CONFIRM_CLEAR_SEARCH_HISTORY_DIALOG:
                return new AlertDialog.Builder(MessagingPreferenceActivity.this)
                    .setTitle(R.string.confirm_clear_search_title)
                    .setMessage(R.string.confirm_clear_search_text)
                    .setPositiveButton(android.R.string.ok, new AlertDialog.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            SearchRecentSuggestions recent =
                                ((MmsApp)getApplication()).getRecentSuggestions();
                            if (recent != null) {
                                recent.clearHistory();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .create();
        }
        return super.onCreateDialog(id);
    }

    public static boolean getNotificationEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificationsEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, true);
        return notificationsEnabled;
    }

    public static void enableNotifications(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();

        editor.putBoolean(MessagingPreferenceActivity.NOTIFICATION_ENABLED, enabled);

        editor.apply();
    }

    public static boolean getQuickMessageEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean quickMessageEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.QUICKMESSAGE_ENABLED, false);
        return quickMessageEnabled;
    }

    public static void enableQuickMessage(boolean enabled, Context context) {
        // Store the value of notifications in SharedPreferences
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.QUICKMESSAGE_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getQmLockscreenEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean qmLockscreenEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.QM_LOCKSCREEN_ENABLED, false);
        return qmLockscreenEnabled;
    }

    public static void enableQmLockscreen(boolean enabled, Context context) {
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.QM_LOCKSCREEN_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getQmCloseAllEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean qmCloseAllEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.QM_CLOSE_ALL_ENABLED, false);
        return qmCloseAllEnabled;
    }

    public static void enableQmCloseAll(boolean enabled, Context context) {
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.QM_CLOSE_ALL_ENABLED, enabled);
        editor.apply();
    }

    public static void enableQmDarkTheme(boolean enabled, Context context) {
        SharedPreferences.Editor editor =
            PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(MessagingPreferenceActivity.QM_DARK_THEME_ENABLED, enabled);
        editor.apply();
    }

    public static boolean getQmDarkThemeEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean qmDarkThemeEnabled =
            prefs.getBoolean(MessagingPreferenceActivity.QM_DARK_THEME_ENABLED, false);
        return qmDarkThemeEnabled;
    }

    private void registerListeners() {
        mRingtonePref.setOnPreferenceChangeListener(this);
        final IntentFilter intentFilter =
                new IntentFilter(TelephonyIntents.ACTION_SIM_STATE_CHANGED);
        intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        if (preference == mRingtonePref) {
            setRingtoneSummary((String)newValue);
            result = true;
        }
        return result;
    }


    private void showToast(int id) {
        Toast.makeText(this, id, Toast.LENGTH_SHORT).show();
    }

    /**
     * Set the SMSC preference enable or disable.
     *
     * @param id  the subscription of the slot, if the value is ALL_SUB, update all the SMSC
     *            preference
     * @param airplaneModeIsOn  the state of the airplane mode
     */
    private void setSmscPrefState(final int phoneId, boolean prefEnabled) {
        // We need update the preference summary.
        if (prefEnabled) {
            new Thread (new Runnable() {
                public void run() {
                    int[] sub = SubscriptionManager.getSubId(phoneId);
                    SmsManager sm = SmsManager.getSmsManagerForSubscriptionId(sub[0]);
                    final String scAddress = sm.getSmscAddressFromIcc();
                    Log.d(TAG, "get SMSC from sub= " + phoneId + " scAddress= " + scAddress);
                    if (scAddress == null) return ;
                    final String displaySmsc = scAddress.substring(1, scAddress.lastIndexOf("\""));
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Log.d(TAG, "update SMSC= " + displaySmsc + " phoneId = " +phoneId);
                            mSmscPrefList.get(phoneId).setSummary(displaySmsc);
                        }
                    });
                }
            }).start();
        } else {
            mSmscPrefList.get(phoneId).setSummary(null);
        }
        mSmscPrefList.get(phoneId).setEnabled(prefEnabled);
    }

    private void updateSmscPref() {
        if (mSmscPrefList == null || mSmscPrefList.size() == 0) {
            return;
        }
        int count = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < count; i++) {
            setSmscPrefState(i, !isAirPlaneModeOn() &&
                    TelephonyManager.getDefault().hasIccCard(i));
        }
    }

    public static class MyEditDialogFragment extends DialogFragment {
        private MessagingPreferenceActivity mActivity;

        public static MyEditDialogFragment newInstance(MessagingPreferenceActivity activity,
                CharSequence title, CharSequence smsc, int phoneId) {
            MyEditDialogFragment dialog = new MyEditDialogFragment();
            dialog.mActivity = activity;

            Bundle args = new Bundle();
            args.putCharSequence(SMSC_DIALOG_TITLE, title);
            args.putCharSequence(SMSC_DIALOG_NUMBER, smsc);
            args.putInt(SMSC_DIALOG_PHONE_ID, phoneId);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int phoneId = getArguments().getInt(SMSC_DIALOG_PHONE_ID);
            if (null == mActivity) {
                mActivity = (MessagingPreferenceActivity) getActivity();
                dismiss();
            }
            final EditText edit = new EditText(mActivity);
            edit.setPadding(15, 15, 15, 15);
            edit.setText(getArguments().getCharSequence(SMSC_DIALOG_NUMBER));

            Dialog alert = new AlertDialog.Builder(mActivity)
                    .setTitle(getArguments().getCharSequence(SMSC_DIALOG_TITLE))
                    .setView(edit)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            MyAlertDialogFragment newFragment = MyAlertDialogFragment.newInstance(
                                    mActivity, phoneId, edit.getText().toString());
                            newFragment.show(getFragmentManager(), "dialog");
                            dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(true)
                    .create();
            alert.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            return alert;
        }
    }

    /**
     * All subclasses of Fragment must include a public empty constructor. The
     * framework will often re-instantiate a fragment class when needed, in
     * particular during state restore, and needs to be able to find this
     * constructor to instantiate it. If the empty constructor is not available,
     * a runtime exception will occur in some cases during state restore.
     */
    public static class MyAlertDialogFragment extends DialogFragment {
        private MessagingPreferenceActivity mActivity;

        public static MyAlertDialogFragment newInstance(MessagingPreferenceActivity activity,
                                                        int phoneId, String smsc) {
            MyAlertDialogFragment dialog = new MyAlertDialogFragment();
            dialog.mActivity = activity;

            Bundle args = new Bundle();
            args.putInt(SMSC_DIALOG_PHONE_ID, phoneId);
            args.putString(SMSC_DIALOG_NUMBER, smsc);
            dialog.setArguments(args);
            return dialog;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int phoneId = getArguments().getInt(SMSC_DIALOG_PHONE_ID);
            final String displayedSmsc = getArguments().getString(SMSC_DIALOG_NUMBER);

            // When framework re-instantiate this fragment by public empty
            // constructor and call onCreateDialog(Bundle savedInstanceState) ,
            // we should make sure mActivity not null.
            if (null == mActivity) {
                mActivity = (MessagingPreferenceActivity) getActivity();
            }

            return new AlertDialog.Builder(mActivity)
                    .setIcon(android.R.drawable.ic_dialog_alert).setMessage(
                            R.string.set_smsc_confirm_message)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            int[] sub = SubscriptionManager.getSubId(phoneId);
                            SmsManager sm = SmsManager.getSmsManagerForSubscriptionId(sub[0]);
                            String scAddress = "\"" + displayedSmsc + "\"";
                            boolean ret = sm.setSmscAddressToIcc(scAddress);
                            Log.d(TAG, "set SMSC to phoneId= " + phoneId
                                    + " scAddress= " + scAddress);

                            if (ret) {
                                mSmscPrefList.get(phoneId).setSummary(displayedSmsc);
                                mActivity.showToast(R.string.set_smsc_success);
                            } else {
                                mActivity.showToast(R.string.set_smsc_error);
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .setCancelable(true)
                    .create();
        }
    }

    // For the group mms feature to be enabled, the following must be true:
    //  1. the feature is enabled in mms_config.xml (currently on by default)
    //  2. the feature is enabled in the mms settings page
    //  3. the SIM knows its own phone number
    public static boolean getIsGroupMmsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean groupMmsPrefOn = prefs.getBoolean(
                MessagingPreferenceActivity.GROUP_MMS_MODE, true);
        return MmsConfig.getGroupMmsEnabled() &&
                groupMmsPrefOn &&
                !TextUtils.isEmpty(MessageUtils.getLocalNumber());
    }
}
