# Real-Time Home Monitoring & Event Detection System

An end-to-end home monitoring system integrating real-time video streaming, pose-based event detection, a Flask backend, and an Android application for remote monitoring and alert notifications.

**Tech Stack:** `Java` · `Python` · `Flask` · `MediaPipe` · `OpenCV` · `RTSP` · `LibVLC` · `OkHttp` · `Android`

---

## Overview

This project was developed as a real-time home monitoring system that combines video streaming, human pose analysis, event detection, backend communication, and mobile alerting into a complete monitoring pipeline.

The system consists of two main components:

* **Python / Flask Backend** — captures and processes video frames, performs MediaPipe-based pose estimation and rule-based event detection, and exposes detected alerts through HTTP endpoints.
* **Android Application** — displays a live RTSP video stream, periodically retrieves alerts from the Flask backend, maintains recent alert history, and generates Android system notifications.

A primary goal of the project was to integrate multiple independent components into a working end-to-end system rather than implementing each component in isolation.

---

## System Architecture

```text
                         Home Camera
                        /           \
                       /             \
                      ▼               ▼
             RTSP Video Stream    Video Source
                      │               │
                      ▼               ▼
          ┌──────────────────┐   ┌─────────────────────┐
          │ Android App      │   │ Python Backend      │
          │                  │   │                     │
          │ LibVLC           │   │ OpenCV              │
          │ Live Video       │   │ MediaPipe Pose      │
          └──────────────────┘   │ Event Detection     │
                      │          │ Flask REST API      │
                      │          └──────────┬──────────┘
                      │                     │
                      │               /alerts endpoint
                      │                     │
                      │                     ▼
                      │          ┌─────────────────────┐
                      └─────────►│ Android App         │
                                 │                     │
                                 │ OkHttp Polling      │
                                 │ Alert History       │
                                 │ Notifications       │
                                 └─────────────────────┘
```

---

## Key Features

### Real-Time Video Monitoring

* Displays a live RTSP video stream in the Android application.
* Uses LibVLC for RTSP playback and video rendering.
* Manages video playback through the Android `SurfaceView` lifecycle.
* Integrates the live monitoring interface with event and alert information.

### Pose-Based Event Detection

The Python backend uses MediaPipe pose landmarks and rule-based analysis to detect different human activity and posture conditions.

Implemented detection logic includes:

* Possible fall detection
* Sudden or vigorous movement
* Prolonged stillness
* Abnormal standing posture
* Sitting posture
* Possible crawling behavior
* Repeated wandering movement
* Person absence detection
* Prolonged abnormal posture

### Flask Backend

The Flask backend:

* Captures and processes video frames.
* Runs MediaPipe pose estimation.
* Executes event-detection functions.
* Maintains detected alert information.
* Provides an MJPEG video stream.
* Exposes alert data as JSON for the Android client.

### Android Alert System

The Android client periodically communicates with the Flask backend using OkHttp.

`AlertManager` is responsible for:

* Polling the backend `/alerts` endpoint.
* Retrieving the latest detected event.
* Maintaining a local history of recent alerts.
* Updating the active Android UI when alert data changes.
* Filtering alerts according to user-selected keywords.
* Broadcasting qualifying events to the Android notification system.

### Android Notifications

The application uses Android `BroadcastReceiver` and notification channels to generate system notifications for qualifying monitoring events.

Users can configure notification-related preferences and event filters through the settings interface.

### Contact and Dialing Interface

The application also includes a contact interface that:

* Stores user-configured contacts with `SharedPreferences`.
* Displays saved contacts within the application.
* Opens the Android system dialer when a contact is selected.

---

## Project Structure

```text
Real-Time-Home-Monitoring-System/
│
├── backend/
│   ├── app.py
│   ├── events.py
│   └── requirements.txt
│
├── android/
│   ├── app/
│   │   └── app/
│   │       └── src/
│   │           └── main/
│   │               └── java/
│   │                   └── com/
│   │                       └── example/
│   │                           └── myapplication/
│   │                               ├── MainActivity.java
│   │                               ├── AlertManager.java
│   │                               ├── AlertReceiver.java
│   │                               ├── NotificationActivity.java
│   │                               ├── NotificationHelper.java
│   │                               ├── Phone.java
│   │                               └── Setting.java
│   │
│   ├── gradle/
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   └── settings.gradle.kts
│
├── .gitignore
└── README.md
```

---

## Android Application

### `MainActivity`

The main monitoring interface.

Responsibilities include:

* Initializing LibVLC.
* Connecting to the RTSP video stream.
* Rendering live video through `SurfaceView`.
* Managing the VLC player lifecycle.
* Navigating between monitoring, notification, contact, and settings screens.
* Starting background alert polling through `AlertManager`.
* Displaying recent alert information.

### `AlertManager`

A singleton manager responsible for background communication with the Flask backend.

It:

* Polls the `/alerts` endpoint at regular intervals.
* Processes the most recent backend event.
* Maintains a limited alert history.
* Updates registered UI listeners.
* Checks notification keyword preferences.
* Broadcasts qualifying alerts to `AlertReceiver`.

### `AlertReceiver`

Receives alert broadcasts from `AlertManager` and generates Android system notifications using a high-priority notification channel.

### `NotificationActivity`

Displays recent monitoring and detection records received through `AlertManager`.

### `NotificationHelper`

Provides helper functionality for creating Android notification channels and displaying notifications.

### `Setting`

Manages application preferences using Android `SharedPreferences`, including:

* Notification enable/disable
* Event keyword filters
* Custom alert keywords
* Contact names and phone numbers

### `Phone`

Loads saved contacts and provides an interface for opening the Android system dialer.

---

## Backend

The backend is implemented using Python, Flask, OpenCV, MediaPipe, and NumPy.

### `app.py`

The main backend application.

Responsibilities include:

* Opening a video source.
* Processing video frames with MediaPipe.
* Drawing detected pose landmarks.
* Calling event-detection functions.
* Displaying detection states on processed frames.
* Encoding frames for MJPEG streaming.
* Serving Flask endpoints for video and alert data.

### `events.py`

Contains the rule-based event-detection logic.

It processes MediaPipe pose landmarks and evaluates posture, movement, and timing conditions to determine whether monitored events have occurred.

Shared helper functions are used for:

* Pose landmark extraction
* Joint-angle calculation
* Pose stability analysis
* Alert generation

---

## Flask Endpoints

| Endpoint      | Description                                          |
| ------------- | ---------------------------------------------------- |
| `/`           | Returns backend status information                   |
| `/video_feed` | Provides the processed MJPEG video stream            |
| `/alerts`     | Returns detected alerts as JSON                      |
| `/video`      | Provides a simple browser-based monitoring interface |

---

## Technologies

| Area               | Technologies                           |
| ------------------ | -------------------------------------- |
| Mobile Application | Android, Java                          |
| Backend            | Python, Flask                          |
| Computer Vision    | MediaPipe, OpenCV, NumPy               |
| Video Streaming    | RTSP, LibVLC, MJPEG                    |
| Networking         | REST/HTTP, JSON, OkHttp                |
| Local Storage      | Android SharedPreferences              |
| Notifications      | BroadcastReceiver, NotificationManager |
| Development        | Android Studio, Git, GitHub            |

---

## Backend Setup

### 1. Install Dependencies

```bash
pip install -r backend/requirements.txt
```

### 2. Run the Backend

```bash
python backend/app.py
```

The public version is configured to use a local webcam by default.

Camera addresses, RTSP credentials, and environment-specific server addresses are intentionally excluded from this public repository.

---

## Android Configuration

Before running the Android application, configure the appropriate local endpoints for your environment.

Example placeholders used in the public version:

```text
RTSP_URL = rtsp://username:password@camera-ip:554/stream1
FLASK_URL = http://server-ip:5000/alerts
```

Real credentials and private network configuration should not be committed to a public repository.

---

## Engineering Experience

The project involved integration across multiple technical areas, including:

- Real-time video streaming
- Android application development
- Python backend development
- REST-based client-server communication
- MediaPipe pose estimation
- Rule-based event detection
- Background network polling
- Android lifecycle management
- Android notification handling
- SharedPreferences-based configuration
- End-to-end system integration

The system architecture connects video streaming, computer vision, backend processing, network communication, alert management, and a mobile interface into an end-to-end monitoring pipeline.

## Future Improvements

Potential improvements include:

* Replacing periodic HTTP polling with WebSocket or Server-Sent Events
* Improving event classification and false-positive suppression
* Moving environment-specific endpoints into external configuration
* Adding secure authentication between the mobile client and backend
* Adding persistent alert and event storage
* Improving event-history visualization
* Adding automated testing
* Adding CI workflows
* Improving deployment support for persistent backend hosting

---

## Project Context & Contributions

This project was developed as a team-based capstone project.

My primary contributions focused on system integration and Android application development, including:

- Integrated the Android application with the real-time monitoring backend.
- Implemented RTSP video streaming and playback using LibVLC.
- Developed backend-to-Android alert communication and event retrieval.
- Implemented alert history, notification filtering, and Android notification workflows.
- Contributed to integrating video streaming, backend processing, event detection, and the mobile interface into an end-to-end monitoring system.

The repository contains selected components of the project that demonstrate the system architecture and implementation.
