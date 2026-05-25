# 🤟 Speech to Sign Converter

> A 2nd Semester Android project that makes communication more accessible  
> for the deaf and hard-of-hearing community — in real time.

---

## 📱 What Does This App Do?

This Android app works in **two directions**:

1. **Speech → Sign** — You speak, the app converts your words into Indian Sign Language (ISL) visuals
2. **Gesture → Text/Speech** — You show a hand sign to the camera, the app detects it and reads it out loud

---

## 🔄 App Flow

```
Launch App
    │
    ▼
Login Screen          ← Enter name, email, phone (saved to Firebase)
    │
    ▼
OTP Verification      ← Enter OTP to confirm identity
    │
    ▼
Main Screen           ← Two modes available:
    │
    ├──▶ 🎤 Speak Button     → Speech recognised → ISL sign displayed
    │
    └──▶ 📷 Camera Button    → Live camera → Hand detected → Sign classified → Spoken aloud
```

---

## 🧩 Features Breakdown

### 🔐 Login & Authentication (`LoginActivity.java`)
- User enters **name, email, and 10-digit phone number**
- Input is validated (email format check, phone length check)
- User data is stored in **Firebase Firestore** under a `users` collection
- On success, moves to OTP screen

### 🔑 OTP Verification (`OtpActivity.java`)
- Simple OTP entry screen to verify user identity
- On correct OTP, proceeds to the main app screen

### 🎤 Speech to Sign (`Main2Activity.java`)
- Uses Android's built-in **SpeechRecognizer** to capture spoken audio
- Converts recognised speech into ISL signs using a 3-tier lookup system:

| Priority | Type | Output |
|----------|------|--------|
| 1st | Whole words (`hello`, `good`, `morning`, `you`) | Animated GIF |
| 2nd | Numbers (`0-9` or spoken like `one`, `two`) | Static image |
| 3rd | Unknown words | Spelled letter-by-letter using alphabet images |

- Signs are displayed **sequentially with timing delays** — words show for ~3 seconds, letters for 700ms each
- Uses **Glide** library to render both GIFs and static images smoothly

### 📷 Live Gesture Detection (`CameraActivity.java`)
- Opens the **front camera** using CameraX at 640×480
- Each frame is **mirrored** (to handle front camera flip) and passed to MediaPipe
- **MediaPipe HandLandmarker** detects 21 key points on the hand in real time
- The 21 landmarks (x, y, z coordinates = 63 floats) are fed into a custom **TFLite ISL model**
- The model normalises the landmark data relative to the wrist position and classifies the gesture
- Only predictions with **confidence > 70%** are accepted — anything lower shows `?`
- Detected gesture is displayed on screen **and spoken aloud** via Android TextToSpeech
- Supports **up to 2 hands** simultaneously

### 🖼️ Hand Skeleton Overlay (`OverlayView.java`)
- A custom `View` drawn directly on top of the camera preview
- Draws **green dots** on all 21 hand landmarks
- Draws **white lines** connecting the dots following the hand skeleton (thumb, index, middle, ring, pinky, palm)
- Uses **GPU-accelerated hardware layer** for smooth 30+ fps rendering
- Handles all camera rotation degrees (0°, 90°, 180°, 270°) correctly

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java |
| Platform | Android (API 21+) |
| Camera | CameraX |
| Hand Detection | MediaPipe HandLandmarker |
| Sign Classification | TensorFlow Lite (custom ISL model) |
| Speech Input | Android SpeechRecognizer |
| Speech Output | Android TextToSpeech |
| Image/GIF Loading | Glide |
| Authentication | Firebase Firestore |
| Build System | Gradle 8.13 |

---

## 📦 Project Structure

```
app/
├── src/main/
│   ├── java/com/example/speechtotext/
│   │   ├── LoginActivity.java       ← Firebase login & user registration
│   │   ├── OtpActivity.java         ← OTP verification screen
│   │   ├── Main2Activity.java       ← Speech → Sign conversion
│   │   ├── CameraActivity.java      ← Live gesture detection via camera
│   │   └── OverlayView.java         ← Hand skeleton drawing on camera
│   │
│   ├── assets/
│   │   ├── isl_model.tflite         ← Custom trained ISL gesture classifier
│   │   ├── hand_landmarker.task     ← MediaPipe hand detection model (7.8MB)
│   │   └── isl_labels.txt           ← Sign labels for the TFLite model
│   │
│   └── AndroidManifest.xml
│
├── build.gradle.kts
└── google-services.json             ← Firebase config
```

---

## 🤖 How the AI Works

### Step 1 — Hand Detection (MediaPipe)
MediaPipe's `HandLandmarker` scans each camera frame and returns **21 landmarks** per hand — key points like fingertips, knuckles, and the wrist — as normalised (x, y, z) coordinates.

### Step 2 — Feature Extraction
The 21 landmarks produce **63 float values**. These are then:
- **Translated** — wrist position subtracted so the hand position on screen doesn't affect classification
- **Normalised** — divided by the maximum absolute value so hand size doesn't matter

### Step 3 — TFLite Classification
The 63 normalised floats are passed into a custom-trained TFLite model that outputs a confidence score for each ISL sign label. The highest scoring label above **70% confidence** is returned as the detected gesture.

---

## 🚀 How to Run

1. Clone the repo
2. Open in **Android Studio**
3. Connect a Firebase project and replace `google-services.json`
4. Build and run on a physical Android device (camera required)

> ⚠️ Speech recognition and camera features require a real device — they don't work on emulators.

---

## 📸 Permissions Required

| Permission | Purpose |
|------------|---------|
| `RECORD_AUDIO` | Speech recognition |
| `CAMERA` | Live hand gesture detection |
| `INTERNET` | Firebase Firestore + Speech API |

---

---

## ⚠️ Current Status

This project is **~85% complete** and actively being improved.  
Core architecture, camera pipeline, Firebase login, and speech-to-sign conversion all work.  
The primary remaining work is expanding the ISL training dataset.

---

## 🚧 Known Limitations & TODO

### Dataset
- [ ] ISL model currently recognises a **limited set of signs** (partial A–Z range — up to around M/R)
- [ ] Signs like closed fist and several others are not yet in the training data
- [ ] **Next step:** Collect and train a full A–Z + common words ISL dataset

### Features
- [ ] OTP is hardcoded as `1234` for demo purposes — needs dynamic generation
- [ ] Word GIF library is small — only covers `hello`, `good`, `you`, `morning`
- [ ] UI polish on camera detection screen

### Why the Dataset Problem is Hard
Training a sign language model requires:
- Hundreds of samples per gesture, from multiple people and lighting conditions
- Careful landmark collection and labelling
- Model retraining and validation

This is an **ongoing research problem** — even academic papers struggle with full ISL datasets.  
The model pipeline and architecture are fully built and ready to scale once more training data is added.

---

## 💡 Key Takeaway

> This project combines **speech recognition, computer vision, and deep learning**  
> into a single Android app — built in a 2nd semester college project.  
> The full ML pipeline is production-ready — expanding the dataset is the only remaining step. 🎓
