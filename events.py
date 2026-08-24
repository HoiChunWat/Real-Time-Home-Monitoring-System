import math
import time

import mediapipe as mp
import numpy as np


# MediaPipe pose namespace
mppose = mp.solutions.pose


# Shared detection state
status4 = False
status5 = False
start_time5 = 0

last_stable_time = 0
previous_angles = None

alerts = []


# ---------------------------------------------------------------------------
# Shared helpers
# ---------------------------------------------------------------------------

def add_alert(message):
    """Append a timestamped alert message to the shared alert list."""
    timestamp = time.strftime("%H:%M:%S", time.localtime())
    alerts.append(f"{timestamp} - {message}")

    # Optional: keep only the most recent 20 alerts.
    # if len(alerts) > 20:
    #     alerts.pop(0)


def get_landmarks(context):
    """Return MediaPipe pose landmarks from the shared context."""
    results = context.get("results")
    return results.pose_landmarks.landmark if results and results.pose_landmarks else None


def get_landmark(landmarks, part_name):
    """Return the x, y, and z coordinates of a named pose landmark."""
    landmark = landmarks[mppose.PoseLandmark[part_name].value]
    return [landmark.x, landmark.y, landmark.z]


def calc_angle(a, b, c):
    """Calculate the angle formed by points a-b-c in degrees."""
    a = np.array(a)
    b = np.array(b)
    c = np.array(c)

    radians = (
        np.arctan2(c[1] - b[1], c[0] - b[0])
        - np.arctan2(a[1] - b[1], a[0] - b[0])
    )

    angle = np.abs(radians * 180.0 / np.pi)
    return 360 - angle if angle > 180 else angle


def get_hip_angles(landmarks):
    """Calculate left and right hip angles."""
    right_shoulder = get_landmark(landmarks, "RIGHT_SHOULDER")
    left_shoulder = get_landmark(landmarks, "LEFT_SHOULDER")
    right_hip = get_landmark(landmarks, "RIGHT_HIP")
    left_hip = get_landmark(landmarks, "LEFT_HIP")
    right_knee = get_landmark(landmarks, "RIGHT_KNEE")
    left_knee = get_landmark(landmarks, "LEFT_KNEE")

    right_angle = calc_angle(right_shoulder, right_hip, right_knee)
    left_angle = calc_angle(left_shoulder, left_hip, left_knee)

    return [right_angle, left_angle]


def is_pose_stable(current_angles, previous_angles, threshold=5.0):
    """Check whether the current pose angles remain within the threshold."""
    if previous_angles is None:
        return False

    right_diff = abs(current_angles[0] - previous_angles[0])
    left_diff = abs(current_angles[1] - previous_angles[1])

    return right_diff < threshold and left_diff < threshold


# ---------------------------------------------------------------------------
# Event detectors
# ---------------------------------------------------------------------------

def first(context):
    """Detect a possible fall using shoulder-to-hip vertical distance."""
    landmarks = get_landmarks(context)
    if not landmarks:
        return

    threshold = 0.2
    notification_interval = 0

    left_shoulder_y = landmarks[mppose.PoseLandmark.LEFT_SHOULDER].y
    left_hip_y = landmarks[mppose.PoseLandmark.LEFT_HIP].y

    if (left_hip_y - left_shoulder_y) < threshold:
        now = time.time()
        if now - context.get("last_notification_time1", 0) > notification_interval:
            add_alert(f"[{time.strftime('%H:%M:%S')}] 警告！可能跌倒！ 1")
            context["last_notification_time1"] = now


def second(context):
    """Detect sudden or vigorous movement, such as running."""
    landmarks = get_landmarks(context)
    if not landmarks:
        return

    notification_interval = 5

    left_knee = landmarks[mppose.PoseLandmark.LEFT_KNEE]
    right_knee = landmarks[mppose.PoseLandmark.RIGHT_KNEE]
    movement = abs(left_knee.y - right_knee.y)

    if movement > 0.3:
        now = time.time()
        if now - context.get("last_notification_time2", 0) > notification_interval:
            add_alert(f"[{time.strftime('%H:%M:%S')}] 警告！突發劇烈移動（如跑步）！ 2")
            context["last_notification_time2"] = now


def third(context):
    """Detect prolonged stillness based on changes in hip angles."""
    global previous_angles, last_stable_time

    notification_interval = 3
    landmarks = get_landmarks(context)

    if not landmarks:
        return False

    try:
        hip_angles = get_hip_angles(landmarks)
    except (KeyError, IndexError, TypeError):
        hip_angles = None

    result = False

    if hip_angles:
        if is_pose_stable(hip_angles, previous_angles):
            if last_stable_time == 0:
                last_stable_time = time.time()

            elapsed = time.time() - last_stable_time

            if elapsed >= notification_interval:
                result = True
                add_alert(f"[{time.strftime('%H:%M:%S')}] 長時間靜止 3")
        else:
            last_stable_time = 0
            add_alert(f"[{time.strftime('%H:%M:%S')}] 無狀態 3")
    else:
        previous_angles = None

    previous_angles = hip_angles
    return result


def fourth(context):
    """Detect an abnormal standing posture that persists for several seconds."""
    global status4

    countdown_seconds = 3

    if not hasattr(fourth, "abnormal_started_time"):
        fourth.abnormal_started_time = 0

    if not hasattr(fourth, "abnormal_confirmed"):
        fourth.abnormal_confirmed = False

    landmarks = get_landmarks(context)

    if not landmarks:
        # Reset state when no person is detected.
        fourth.abnormal_started_time = 0
        fourth.abnormal_confirmed = False
        status4 = False
        return False

    try:
        hip_angles = get_hip_angles(landmarks)
    except (KeyError, IndexError, TypeError):
        return False

    abnormal_detected = (
        (0 < hip_angles[0] < 155)
        or (0 < hip_angles[1] < 155)
    )

    now = time.time()

    if abnormal_detected:
        if fourth.abnormal_started_time == 0:
            fourth.abnormal_started_time = now

        elapsed = now - fourth.abnormal_started_time

        if elapsed >= countdown_seconds:
            fourth.abnormal_confirmed = True
            status4 = True
            add_alert(f"[{time.strftime('%H:%M:%S')}] 異常站姿 4")
            return True

        status4 = False
        return False

    # Reset state when the posture returns to normal.
    fourth.abnormal_started_time = 0
    fourth.abnormal_confirmed = False
    status4 = False
    return False


def fifth(context):
    """Detect a sitting posture and track how long it persists."""
    global status5, start_time5

    countdown_seconds = 3
    landmarks = get_landmarks(context)

    if not landmarks:
        return False

    try:
        hip_angles = get_hip_angles(landmarks)
    except (KeyError, IndexError, TypeError):
        return False

    sitting = (
        (0 < hip_angles[0] < 130)
        or (0 < hip_angles[1] < 130)
    )

    if sitting:
        if not status5:
            start_time5 = time.time()

        status5 = True
        elapsed_time = time.time() - start_time5

        if elapsed_time >= countdown_seconds:
            add_alert(
                f"[{time.strftime('%H:%M:%S')}] "
                f"坐下持續 {countdown_seconds} 秒 5"
            )

        return True

    status5 = False
    return False


def sixth(context):
    """Detect a possible crawling posture."""
    landmarks = get_landmarks(context)
    if not landmarks:
        return

    notification_interval = 5

    left_knee = landmarks[mppose.PoseLandmark.LEFT_KNEE].y
    right_knee = landmarks[mppose.PoseLandmark.RIGHT_KNEE].y
    left_hand = landmarks[mppose.PoseLandmark.LEFT_WRIST].y
    right_hand = landmarks[mppose.PoseLandmark.RIGHT_WRIST].y

    if max(left_knee, right_knee) < min(left_hand, right_hand):
        now = time.time()

        if now - context.get("last_notification_time6", 0) > notification_interval:
            add_alert(f"[{time.strftime('%H:%M:%S')}] 警告！可能正在爬行！ 6")
            context["last_notification_time6"] = now


def seventh(context):
    """Detect repeated horizontal movement that may indicate wandering."""
    landmarks = get_landmarks(context)
    if not landmarks:
        return

    max_positions = 10
    notification_interval = 5

    nose = landmarks[mppose.PoseLandmark.NOSE]
    context.setdefault("nose_x_positions", []).append(nose.x)

    if len(context["nose_x_positions"]) > max_positions:
        context["nose_x_positions"].pop(0)

    if len(context["nose_x_positions"]) == max_positions:
        movement_range = (
            max(context["nose_x_positions"])
            - min(context["nose_x_positions"])
        )

        if movement_range > 0.1:
            now = time.time()

            if now - context.get("last_notification_time7", 0) > notification_interval:
                add_alert(f"[{time.strftime('%H:%M:%S')}] 警告！異常徘徊！ 7")
                context["last_notification_time7"] = now


def eighth(context):
    """Return True when no person is currently detected."""
    landmarks = get_landmarks(context)

    if not landmarks:
        add_alert(f"[{time.strftime('%H:%M:%S')}] 沒偵測到人 8")
        return True

    return False


def ninth(context):
    """Detect abnormal posture conditions that persist over time."""
    landmarks = get_landmarks(context)
    if not landmarks:
        return

    abnormal_duration = 5
    now = time.time()

    required = [
        mppose.PoseLandmark.NOSE,
        mppose.PoseLandmark.LEFT_HIP,
        mppose.PoseLandmark.RIGHT_HIP,
        mppose.PoseLandmark.LEFT_KNEE,
        mppose.PoseLandmark.RIGHT_KNEE,
        mppose.PoseLandmark.LEFT_SHOULDER,
        mppose.PoseLandmark.RIGHT_SHOULDER,
        mppose.PoseLandmark.LEFT_ANKLE,
        mppose.PoseLandmark.RIGHT_ANKLE,
    ]

    if not all(landmarks[landmark].visibility > 0.2 for landmark in required):
        return

    # Extract pose measurements used by the abnormal-posture rules.
    nose_x = landmarks[mppose.PoseLandmark.NOSE].x
    nose_y = landmarks[mppose.PoseLandmark.NOSE].y

    hip_y = (
        landmarks[mppose.PoseLandmark.LEFT_HIP].y
        + landmarks[mppose.PoseLandmark.RIGHT_HIP].y
    ) / 2

    shoulder_y = (
        landmarks[mppose.PoseLandmark.LEFT_SHOULDER].y
        + landmarks[mppose.PoseLandmark.RIGHT_SHOULDER].y
    ) / 2

    ankle_diff = abs(
        landmarks[mppose.PoseLandmark.LEFT_ANKLE].y
        - landmarks[mppose.PoseLandmark.RIGHT_ANKLE].y
    )

    shoulder_diff = abs(
        landmarks[mppose.PoseLandmark.LEFT_SHOULDER].y
        - landmarks[mppose.PoseLandmark.RIGHT_SHOULDER].y
    )

    head_tilt = nose_y - shoulder_y

    body_angle = math.degrees(
        math.atan2(nose_y - hip_y, 0.001 + abs(nose_x))
    )

    abnormal = False

    if body_angle < 70:
        abnormal = True
        add_alert(f"[{time.strftime('%H:%M:%S')}] 警告！身體過度前傾！ 9")

    if shoulder_diff > 0.1:
        abnormal = True
        add_alert(f"[{time.strftime('%H:%M:%S')}] 警告！身體歪斜！ 9")

    if ankle_diff > 0.15:
        abnormal = True
        add_alert(f"[{time.strftime('%H:%M:%S')}] 警告！可能單腳站立！ 9")

    if head_tilt > 0.15:
        abnormal = True
        add_alert(f"[{time.strftime('%H:%M:%S')}] 警告！可能長時間低頭！ 9")

    if abnormal:
        if context.get("abnormal_start_time") is None:
            context["abnormal_start_time"] = now
        elif now - context["abnormal_start_time"] > abnormal_duration:
            add_alert(f"[{time.strftime('%H:%M:%S')}] 持續異常站姿超過 5 秒！ 9")
    else:
        context["abnormal_start_time"] = None
