package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Phone extends AppCompatActivity {

    private LinearLayout familyContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnPolice = findViewById(R.id.btnPolice);
        Button btnHospital = findViewById(R.id.btnHospital);

        btnPolice.setOnClickListener(v -> dialPhoneNumber("110"));
        btnHospital.setOnClickListener(v -> dialPhoneNumber("119"));

        familyContainer = findViewById(R.id.familyContainer);

        // Load saved contacts when the screen is first opened.
        loadFamilyContacts();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload contacts after returning from the settings screen.
        loadFamilyContacts();
    }

    /** Load saved family contacts from SharedPreferences. */
    private void loadFamilyContacts() {
        familyContainer.removeAllViews();

        SharedPreferences prefs =
                getSharedPreferences(Setting.PREFS_NAME, MODE_PRIVATE);

        String name1 = prefs.getString(Setting.KEY_CONTACT1_NAME, "Contact 1");
        String phone1 = prefs.getString(Setting.KEY_CONTACT1_PHONE, "");
        String name2 = prefs.getString(Setting.KEY_CONTACT2_NAME, "Contact 2");
        String phone2 = prefs.getString(Setting.KEY_CONTACT2_PHONE, "");
        String name3 = prefs.getString(Setting.KEY_CONTACT3_NAME, "Contact 3");
        String phone3 = prefs.getString(Setting.KEY_CONTACT3_PHONE, "");

        addFamilyContact(name1, phone1);
        addFamilyContact(name2, phone2);
        addFamilyContact(name3, phone3);
    }

    /** Add one saved family contact to the screen. */
    private void addFamilyContact(String name, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            // Hide contacts that do not have a phone number.
            return;
        }

        TextView tv = new TextView(this);
        tv.setText(name + ": " + phone);
        tv.setTextSize(18);

        int padding = (int) (getResources().getDisplayMetrics().density * 8);
        tv.setPadding(padding, padding, padding, padding);

        tv.setTextColor(getResources().getColor(android.R.color.black));
        tv.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);
        tv.setOnClickListener(v -> dialPhoneNumber(phone));

        familyContainer.addView(tv);
    }

    /** Open the system dialer with the selected phone number. */
    private void dialPhoneNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
    }
}
