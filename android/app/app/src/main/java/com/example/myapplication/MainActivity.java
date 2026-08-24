package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // UI components
    private Button btnCall, btnNotification, btnSettings, btnTestAlert;
    private SurfaceView surfaceView;
    private TextView tvStatus;

    // VLC player components
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private SurfaceHolder surfaceHolder;
    private boolean isSurfaceCreated = false;

    // Replace this placeholder with the RTSP stream used in your local environment.
    // Do not commit real credentials to a public repository.
    private static final String RTSP_URL =
            "rtsp://username:password@camera-ip:554/stream1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Android 13+ requires runtime permission for notifications.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100
                );
            }
        }

        // Bind UI components.
        btnCall = findViewById(R.id.btnCall);
        btnNotification = findViewById(R.id.btnNotification);
        btnSettings = findViewById(R.id.btnSettings);
        surfaceView = findViewById(R.id.surfaceView);
        tvStatus = findViewById(R.id.tvStatus);
        btnTestAlert = findViewById(R.id.btnTestAlert);

        // Configure button actions.
        if (btnNotification != null) {
            btnNotification.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, NotificationActivity.class)));
        }

        if (btnCall != null) {
            btnCall.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, Phone.class)));
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, Setting.class)));
        }

        if (btnTestAlert != null) {
            btnTestAlert.setOnClickListener(v -> {
                // Add a test alert so the UI can update immediately.
                AlertManager.getInstance().injectTestAlert("🚨 [Test] Fall event detected!");

                // Broadcast the test alert so Android also displays a system notification.
                Intent alertIntent = new Intent(MainActivity.this, AlertReceiver.class);
                alertIntent.putExtra("alert_message", "🚨 [Test] Fall event detected!");
                sendBroadcast(alertIntent);
            });
        }

        // Start background polling for alert data.
        AlertManager.getInstance().start(this);

        // Register a listener for live alert updates.
        AlertManager.getInstance().setOnAlertUpdateListener(alerts -> runOnUiThread(() -> {
            StringBuilder sb = new StringBuilder();
            for (String s : alerts) {
                sb.append(s).append("\n");
            }
            tvStatus.setText(sb.toString().trim());
        }));

        // Configure the Surface lifecycle before initializing VLC.
        setupSurfaceCallback();
        initVlcPlayer();
    }

    private void setupSurfaceCallback() {
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "Surface created");
                isSurfaceCreated = true;
                attachSurfaceAndPlay();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "Surface destroyed");
                isSurfaceCreated = false;

                if (mediaPlayer != null) {
                    try {
                        IVLCVout vout = mediaPlayer.getVLCVout();
                        vout.detachViews();
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }

    private void initVlcPlayer() {
        try {
            ArrayList<String> options = new ArrayList<>();
            options.add("--rtsp-tcp");
            options.add("--network-caching=300");

            libVLC = new LibVLC(this, options);
            mediaPlayer = new MediaPlayer(libVLC);

            Media media = new Media(libVLC, Uri.parse(RTSP_URL));
            mediaPlayer.setMedia(media);
            media.release();

        } catch (Exception e) {
            Log.e(TAG, "initVlcPlayer error: ", e);
            tvStatus.setText("RTSP initialization failed: " + e.getMessage());
        }
    }

    private void attachSurfaceAndPlay() {
        if (!isSurfaceCreated || surfaceHolder == null || mediaPlayer == null) {
            return;
        }

        try {
            IVLCVout vout = mediaPlayer.getVLCVout();
            vout.setVideoSurface(surfaceHolder.getSurface(), surfaceHolder);
            vout.attachViews();
            mediaPlayer.play();
        } catch (Exception e) {
            Log.e(TAG, "attachSurfaceAndPlay error: ", e);
            tvStatus.setText("RTSP playback error: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        AlertManager.getInstance().setOnAlertUpdateListener(alerts -> runOnUiThread(() -> {
            StringBuilder sb = new StringBuilder();
            for (String s : alerts) {
                sb.append(s).append("\n");
            }
            tvStatus.setText(sb.toString().trim());
        }));

        if (mediaPlayer != null && isSurfaceCreated) {
            attachSurfaceAndPlay();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AlertManager.getInstance().setOnAlertUpdateListener(null);

        if (mediaPlayer != null) {
            try {
                mediaPlayer.pause();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.getVLCVout().detachViews();
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }

        if (libVLC != null) {
            try {
                libVLC.release();
            } catch (Exception ignored) {
            }
            libVLC = null;
        }
    }
}
