package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NotificationActivity extends AppCompatActivity {

    private TextView resultTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        // Return to the previous screen.
        Button btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        resultTextView = findViewById(R.id.result_text);

        // AlertManager is already started by MainActivity, so only register a listener here.
        AlertManager.getInstance().setOnAlertUpdateListener(alerts -> runOnUiThread(() -> {
            StringBuilder sb = new StringBuilder("Latest detection records:\n");
            for (String s : alerts) {
                sb.append(s).append("\n");
            }
            resultTextView.setText(sb.toString().trim());
        }));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Re-register the listener when returning to this screen.
        AlertManager.getInstance().setOnAlertUpdateListener(alerts -> runOnUiThread(() -> {
            StringBuilder sb = new StringBuilder("Latest detection records:\n");
            for (String s : alerts) {
                sb.append(s).append("\n");
            }
            resultTextView.setText(sb.toString().trim());
        }));
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove the listener while the Activity is not visible.
        AlertManager.getInstance().setOnAlertUpdateListener(null);
    }
}
