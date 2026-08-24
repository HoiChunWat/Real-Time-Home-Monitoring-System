package com.example.myapplication;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Setting extends AppCompatActivity {

    // SharedPreferences file name and keys.
    public static final String PREFS_NAME = "home_monitor_prefs";

    public static final String KEY_ENABLE_NOTIFICATION = "notify_enable";
    public static final String KEY_VIBRATE = "notify_vibrate";
    public static final String KEY_SOUND = "notify_sound";

    public static final String KEY_KW_FALL = "kw_fall";
    public static final String KEY_KW_ALARM = "kw_alarm";
    public static final String KEY_KW_STAY = "kw_stay";
    public static final String KEY_KW_IDLE = "kw_idle";
    public static final String KEY_KW_EXTRA = "kw_extra";

    public static final String KEY_CONTACT1_NAME = "contact1_name";
    public static final String KEY_CONTACT1_PHONE = "contact1_phone";
    public static final String KEY_CONTACT2_NAME = "contact2_name";
    public static final String KEY_CONTACT2_PHONE = "contact2_phone";
    public static final String KEY_CONTACT3_NAME = "contact3_name";
    public static final String KEY_CONTACT3_PHONE = "contact3_phone";

    private CheckBox cbEnableNotification, cbVibrate, cbSound;
    private CheckBox cbKwFall, cbKwAlarm, cbKwIntrude, cbKwIdle;
    private EditText etKeywords;

    private EditText etContact1Name, etContact1Phone;
    private EditText etContact2Name, etContact2Phone;
    private EditText etContact3Name, etContact3Phone;

    private Button btnBack, btnSaveKeywords, btnSaveSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        // Bind UI components.
        btnBack = findViewById(R.id.btnBack);
        cbEnableNotification = findViewById(R.id.cbEnableNotification);
        cbVibrate = findViewById(R.id.cbVibrate);
        cbSound = findViewById(R.id.cbSound);

        cbKwFall = findViewById(R.id.cbKwFall);
        cbKwAlarm = findViewById(R.id.cbKwAlarm);
        cbKwIntrude = findViewById(R.id.cbKwStay);
        cbKwIdle = findViewById(R.id.cbKwIdle);

        etKeywords = findViewById(R.id.etKeywords);

        etContact1Name = findViewById(R.id.etContact1Name);
        etContact1Phone = findViewById(R.id.etContact1Phone);
        etContact2Name = findViewById(R.id.etContact2Name);
        etContact2Phone = findViewById(R.id.etContact2Phone);
        etContact3Name = findViewById(R.id.etContact3Name);
        etContact3Phone = findViewById(R.id.etContact3Phone);

        btnSaveKeywords = findViewById(R.id.btnSaveKeywords);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        btnBack.setOnClickListener(v -> finish());

        // Load existing preferences.
        loadSettings();

        // Save notification preferences and keyword filters only.
        btnSaveKeywords.setOnClickListener(v -> {
            saveNotificationSettings();
            Toast.makeText(this, "Notification settings saved", Toast.LENGTH_SHORT).show();
        });

        // Save all notification and contact settings.
        btnSaveSettings.setOnClickListener(v -> {
            saveNotificationSettings();
            saveContactSettings();
            Toast.makeText(this, "All settings saved", Toast.LENGTH_SHORT).show();
        });
    }

    /** Load saved preferences into the settings screen. */
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Notification options.
        cbEnableNotification.setChecked(prefs.getBoolean(KEY_ENABLE_NOTIFICATION, true));
        cbVibrate.setChecked(prefs.getBoolean(KEY_VIBRATE, true));
        cbSound.setChecked(prefs.getBoolean(KEY_SOUND, true));

        // Keyword filters.
        cbKwFall.setChecked(prefs.getBoolean(KEY_KW_FALL, true));
        cbKwAlarm.setChecked(prefs.getBoolean(KEY_KW_ALARM, true));
        cbKwIntrude.setChecked(prefs.getBoolean(KEY_KW_STAY, true));
        cbKwIdle.setChecked(prefs.getBoolean(KEY_KW_IDLE, false));

        etKeywords.setText(prefs.getString(KEY_KW_EXTRA, ""));

        // Saved contacts. Public defaults are intentionally generic.
        etContact1Name.setText(prefs.getString(KEY_CONTACT1_NAME, "Contact 1"));
        etContact1Phone.setText(prefs.getString(KEY_CONTACT1_PHONE, ""));
        etContact2Name.setText(prefs.getString(KEY_CONTACT2_NAME, "Contact 2"));
        etContact2Phone.setText(prefs.getString(KEY_CONTACT2_PHONE, ""));
        etContact3Name.setText(prefs.getString(KEY_CONTACT3_NAME, "Contact 3"));
        etContact3Phone.setText(prefs.getString(KEY_CONTACT3_PHONE, ""));
    }

    /** Save notification and keyword preferences. */
    private void saveNotificationSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();

        ed.putBoolean(KEY_ENABLE_NOTIFICATION, cbEnableNotification.isChecked());
        ed.putBoolean(KEY_VIBRATE, cbVibrate.isChecked());
        ed.putBoolean(KEY_SOUND, cbSound.isChecked());

        ed.putBoolean(KEY_KW_FALL, cbKwFall.isChecked());
        ed.putBoolean(KEY_KW_ALARM, cbKwAlarm.isChecked());
        ed.putBoolean(KEY_KW_STAY, cbKwIntrude.isChecked());
        ed.putBoolean(KEY_KW_IDLE, cbKwIdle.isChecked());

        ed.putString(KEY_KW_EXTRA, etKeywords.getText().toString().trim());

        ed.apply();
    }

    /** Save contact preferences. */
    private void saveContactSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();

        ed.putString(KEY_CONTACT1_NAME, etContact1Name.getText().toString().trim());
        ed.putString(KEY_CONTACT1_PHONE, etContact1Phone.getText().toString().trim());
        ed.putString(KEY_CONTACT2_NAME, etContact2Name.getText().toString().trim());
        ed.putString(KEY_CONTACT2_PHONE, etContact2Phone.getText().toString().trim());
        ed.putString(KEY_CONTACT3_NAME, etContact3Name.getText().toString().trim());
        ed.putString(KEY_CONTACT3_PHONE, etContact3Phone.getText().toString().trim());

        ed.apply();
    }
}
