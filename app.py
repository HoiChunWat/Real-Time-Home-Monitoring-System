from flask import Flask, Response, jsonify
import cv2
import mediapipe as mp
import time

from events import (
    alerts,
    first,
    second,
    third,
    fourth,
    fifth,
    sixth,
    seventh,
    eighth,
    ninth,
)


app = Flask(__name__)


# ---------------------------------------------------------------------------
# MediaPipe configuration
# ---------------------------------------------------------------------------

mp_pose = mp.solutions.pose
pose = mp_pose.Pose(
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5,
)
mp_drawing = mp.solutions.drawing_utils


# RTSP stream address.
# Replace this placeholder locally or move it to an environment variable
# before using an actual camera stream.
rtsp_url = "rtsp://username:password@camera-ip:554/stream1"


# Clear old alerts when the server starts.
alerts.clear()


# ---------------------------------------------------------------------------
# Video processing
# ---------------------------------------------------------------------------

def generate():
    """Capture frames, run pose detection, and stream MJPEG output."""
    # Use 0 for the local webcam. Replace 0 with rtsp_url to use an RTSP stream.
    cap = cv2.VideoCapture(0)

    cap.set(cv2.CAP_PROP_FRAME_WIDTH, 640)
    cap.set(cv2.CAP_PROP_FRAME_HEIGHT, 480)

    if not cap.isOpened():
        print(f"[{time.strftime('%H:%M:%S')}] ERROR: Unable to open video stream.")
        return

    print(f"[{time.strftime('%H:%M:%S')}] Video stream opened.")

    # Shared state used by the event-detection functions.
    context = {
        "nose_x_positions": [],
        "abnormal_start_time": None,
        "last_notification_time3": 0,
        "last_notification_time4": 0,
        "last_notification_time5": 0,
        "last_notification_time6": 0,
        "last_notification_time7": 0,
    }

    try:
        while True:
            success, frame = cap.read()

            if not success:
                print(f"[{time.strftime('%H:%M:%S')}] WARNING: Unable to read frame.")
                break

            # Convert the frame to RGB before MediaPipe processing.
            image = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
            results = pose.process(image)

            # Store the latest frame and pose results in the shared context.
            context["results"] = results
            context["frame"] = frame

            # Always check whether a person is present.
            not_detected = eighth(context)

            # Draw the pose skeleton only when landmarks are available.
            if results.pose_landmarks and not not_detected:
                mp_drawing.draw_landmarks(
                    frame,
                    results.pose_landmarks,
                    mp_pose.POSE_CONNECTIONS,
                )

            # Display person-detection status.
            if not_detected:
                cv2.putText(
                    frame,
                    "NOT DETECTED",
                    (10, 120),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (255, 255, 255),
                    2,
                )
            else:
                cv2.putText(
                    frame,
                    "DETECTED",
                    (10, 120),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 255, 0),
                    2,
                )

            # Optional event detectors can be enabled as needed.
            # first(context)    # Possible fall
            # second(context)   # Sudden or vigorous movement

            # Detect prolonged stillness.
            is_still = third(context)

            if is_still:
                cv2.putText(
                    frame,
                    "STILL",
                    (10, 60),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 0, 255),
                    2,
                )
            else:
                cv2.putText(
                    frame,
                    "MOVING",
                    (10, 60),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 255, 0),
                    2,
                )

            # Detect abnormal standing posture.
            is_abnormal_stand = fourth(context)

            if is_abnormal_stand:
                cv2.putText(
                    frame,
                    "ABNORMAL STAND",
                    (10, 90),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 0, 255),
                    2,
                )
            else:
                cv2.putText(
                    frame,
                    "NORMAL",
                    (10, 90),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 255, 0),
                    2,
                )

            # Detect sitting posture.
            is_sitting = fifth(context)

            if is_sitting:
                cv2.putText(
                    frame,
                    "SIT",
                    (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 0, 255),
                    2,
                )
            else:
                cv2.putText(
                    frame,
                    "NOT SIT",
                    (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    (0, 255, 0),
                    2,
                )

            # Additional optional event detectors.
            # sixth(context)    # Possible crawling
            # seventh(context)  # Repeated wandering movement
            # ninth(context)    # Prolonged abnormal posture

            # Encode the processed frame as JPEG for MJPEG streaming.
            success, buffer = cv2.imencode(".jpg", frame)

            if not success:
                continue

            frame_bytes = buffer.tobytes()

            yield (
                b"--frame\r\n"
                b"Content-Type: image/jpeg\r\n\r\n"
                + frame_bytes
                + b"\r\n"
            )

            # Limit processing to approximately 30 FPS.
            time.sleep(0.03)

    finally:
        cap.release()
        print(f"[{time.strftime('%H:%M:%S')}] Video stream closed.")


# ---------------------------------------------------------------------------
# Flask routes
# ---------------------------------------------------------------------------

@app.route("/")
def index():
    return (
        "MediaPipe + Flask server is running. "
        "Visit /video_feed to view the video stream."
    )


@app.route("/video_feed")
def video_feed():
    return Response(
        generate(),
        mimetype="multipart/x-mixed-replace; boundary=frame",
    )


@app.route("/alerts")
def get_alerts():
    return jsonify(alerts)


@app.route("/video")
def video_page():
    return """
    <html>
        <head>
            <title>Real-Time Monitoring</title>
            <style>
                body { font-family: sans-serif; }
                #alerts {
                    background: #ffeeee;
                    padding: 10px;
                    border: 1px solid red;
                    height: 200px;
                    overflow-y: scroll;
                }
                img { border: 1px solid #ccc; }
            </style>
        </head>
        <body>
            <h1>Real-Time Monitoring</h1>
            <img src="/video_feed" width="640">

            <h2>Alerts</h2>
            <div id="alerts"></div>

            <script>
                async function fetchAlerts() {
                    try {
                        const res = await fetch("/alerts");
                        const data = await res.json();

                        document.getElementById("alerts").innerHTML =
                            data.reverse()
                                .map(msg => `<div>${msg}</div>`)
                                .join("");
                    } catch (error) {
                        console.error(error);
                    }
                }

                // Refresh the alert list every two seconds.
                setInterval(fetchAlerts, 2000);
                fetchAlerts();
            </script>
        </body>
    </html>
    """


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
