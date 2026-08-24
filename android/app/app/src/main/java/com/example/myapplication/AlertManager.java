package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Singleton manager responsible for background alert polling.
 *
 * <p>The manager periodically requests the Flask /alerts endpoint, stores the
 * latest alert in a short history list, updates the active UI listener, and
 * broadcasts qualifying events to AlertReceiver for Android notifications.</p>
 */
public class AlertManager {

    private static final String TAG = "AlertManager";
    private static AlertManager instance;

    private final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinkedList<String> alertHistory = new LinkedList<>();

    private static final int MAX_SIZE = 10;
    private static final int INTERVAL_MS = 1000;

    // Replace with the Flask endpoint used in your local environment.
    private static final String FLASK_URL = "http://server-ip:5000/alerts";

    private boolean isRunning = false;
    private Runnable fetchTask;
    private OnAlertUpdateListener listener;
    private Context appContext;

    private AlertManager() {}

    public static synchronized AlertManager getInstance() {
        if (instance == null) {
            instance = new AlertManager();
        }
        return instance;
    }

    /** Start periodic background polling. */
    public void start(Context context) {
        if (isRunning) {
            return;
        }

        isRunning = true;
        appContext = context.getApplicationContext();

        fetchTask = new Runnable() {
            @Override
            public void run() {
                fetchFromFlask();
                handler.postDelayed(this, INTERVAL_MS);
            }
        };

        handler.post(fetchTask);
        Log.d(TAG, "AlertManager started background polling.");
    }

    /** Stop periodic background polling. */
    public void stop() {
        if (!isRunning) {
            return;
        }

        handler.removeCallbacks(fetchTask);
        isRunning = false;
        Log.d(TAG, "AlertManager stopped background polling.");
    }

    /** Register or clear the active UI listener. */
    public void setOnAlertUpdateListener(OnAlertUpdateListener listener) {
        this.listener = listener;

        if (listener != null) {
            listener.onAlertUpdate(new LinkedList<>(alertHistory));
        }
    }

    /** Request the latest alert list from the Flask backend. */
    private void fetchFromFlask() {
        Request request = new Request.Builder().url(FLASK_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Flask connection failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Server response error: " + response.message());
                    return;
                }

                String body = response.body() != null ? response.body().string() : "";

                try {
                    JSONArray events = new JSONArray(body);

                    if (events.length() == 0) {
                        return;
                    }

                    // Only process the most recent event returned by the backend.
                    String latest = events.optString(events.length() - 1, "");

                    if (latest.isEmpty()
                            || "null".equals(latest)
                            || "None".equals(latest)) {
                        return;
                    }

                    String timestamp = new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.getDefault()
                    ).format(new Date());

                    String record = "[" + timestamp + "] " + latest;

                    // Store the latest event at the front of the history list.
                    synchronized (alertHistory) {
                        alertHistory.addFirst(record);

                        if (alertHistory.size() > MAX_SIZE) {
                            alertHistory.removeLast();
                        }
                    }

                    // Update the active UI.
                    if (listener != null) {
                        listener.onAlertUpdate(new LinkedList<>(alertHistory));
                    }

                    // Broadcast events that match the user's notification filters.
                    if (appContext != null && shouldNotify(latest)) {
                        Intent alertIntent = new Intent(appContext, AlertReceiver.class);
                        alertIntent.putExtra("alert_message", record);
                        appContext.sendBroadcast(alertIntent);

                        Log.d(TAG, "Broadcast sent to AlertReceiver: " + record);
                    }

                } catch (Exception e) {
                    Log.e(TAG, "JSON parsing failed: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Determine whether the latest backend message should trigger a system notification.
     *
     * <p>The decision is based on the master notification switch, predefined keyword
     * filters, and optional user-defined keywords stored in SharedPreferences.</p>
     */
    private boolean shouldNotify(String latestMsg) {
        if (appContext == null) {
            return false;
        }

        SharedPreferences prefs =
                appContext.getSharedPreferences(
                        Setting.PREFS_NAME,
                        Context.MODE_PRIVATE
                );

        // Master notification switch.
        boolean enableNotification =
                prefs.getBoolean(Setting.KEY_ENABLE_NOTIFICATION, true);

        if (!enableNotification) {
            return false;
        }

        // Predefined keyword filters.
        boolean kwFall = prefs.getBoolean(Setting.KEY_KW_FALL, true);
        boolean kwAlarm = prefs.getBoolean(Setting.KEY_KW_ALARM, true);
        boolean kwStay = prefs.getBoolean(Setting.KEY_KW_STAY, true);
        boolean kwIdle = prefs.getBoolean(Setting.KEY_KW_IDLE, false);

        if (kwFall && containsKeyword(latestMsg, "跌倒")) {
            return true;
        }

        if (kwAlarm && containsKeyword(latestMsg, "警報")) {
            return true;
        }

        if (kwStay && containsKeyword(latestMsg, "靜止")) {
            return true;
        }

        if (kwIdle && containsKeyword(latestMsg, "久未移動")) {
            return true;
        }

        // Additional custom keywords are stored as a comma-separated list.
        String custom = prefs.getString(Setting.KEY_KW_EXTRA, "");

        if (!custom.isEmpty()) {
            String[] keywords = custom.split(",");

            for (String keyword : keywords) {
                keyword = keyword.trim();

                if (!keyword.isEmpty() && containsKeyword(latestMsg, keyword)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** Case-insensitive substring matching for alert keywords. */
    private boolean containsKeyword(String msg, String keyword) {
        if (msg == null || keyword == null) {
            return false;
        }

        return msg.toLowerCase().contains(keyword.toLowerCase());
    }

    /** Listener used by Activities that display the current alert history. */
    public interface OnAlertUpdateListener {
        void onAlertUpdate(LinkedList<String> alerts);
    }

    /** Add a test alert locally without contacting the Flask backend. */
    public void injectTestAlert(String msg) {
        String timestamp = new SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        String record = "[" + timestamp + "] " + msg;

        synchronized (alertHistory) {
            alertHistory.addFirst(record);

            if (alertHistory.size() > MAX_SIZE) {
                alertHistory.removeLast();
            }
        }

        if (listener != null) {
            listener.onAlertUpdate(new LinkedList<>(alertHistory));
        }
    }
}
