# CareLink+ (SK Pinji Edition)
**Tagline:** Smart care, Safe life.

## 🌟 Project Overview
CareLink+ is a high-impact, **Predictive Healthcare** and monitoring system specifically tailored for special needs students at **SK Pinji, Ipoh**. Transitioning from a traditional reactive model to a proactive one, CareLink+ serves as a 24/7 "Secondary Guardian," utilizing real-time sensor data to anticipate distress and prevent wandering.

---

## 🚀 Demo Startup Sequence
To bypass Firebase Spark limits and use the fortified local backend:

1. **Terminal:**
   ```bash
   cd ~/Documents/carelinkapps_
   npx firebase emulators:start --import ./my_data --export-on-exit ./my_data
   ```
2. **Android Studio:** 
   * Select **Wear OS Emulator** (Student) or **Phone Emulator** (Guardian).
   * Click the green **Run** button.
3. **Browser:** [http://localhost:4000](http://localhost:4000) (Live Data Monitor).

---

## ✨ Core Predictive Features

### 1. 🛡️ Proactive Vitals & Fall Detection
- **Live Status Board:** Teachers monitor the entire classroom at a glance. Cards turn **Orange (Agitated)** or **Red (Emergency)** based on real-time heart rate thresholds.
- **Predictive Vitals:** Detects stress levels (100-120 BPM) before a crisis occurs.
- **Automated Fall Alerts:** Integrated accelerometer logic sends instant cloud notifications to Guardians if a student falls.

### 2. 📍 Predictive Geofencing & Tracking
- **Safe Zone:** Pinpoint accuracy centered on **SK Pinji (4.565549, 101.081350)**.
- **Wandering Prevention:** Automatically flags students as "Wandering" if they exit the 500m school perimeter.
- **Student Switcher:** Teachers can toggle between different students on the live map instantly.

### 3. 😊 Pulse-Based Emotion Analysis
- **Heart-to-Emotion:** Translates student pulse data into emotional insights (Peaceful, Content, Excited, Distressed).
- **Behavioral Record:** Analyzes physical signals to provide a voice for non-verbal students.

### 4. 🏥 Logistics & localized Support
- **Ipoh Directory:** Features **Hospital Raja Permaisuri Bainun**, **KPJ Ipoh**, and local specialists like **Dr. Ahmad Zaki** and **Dr. Siti Noraini**.
- **90-Day History:** Permanent logging of all vitals, location breadcrumbs, and emotional states for long-term clinical review.
- **Unified Messaging:** Centralized chat for the Guardian-Doctor-Student care circle.

---

## 🛠️ Technical Fortification
- **Architecture:** Global singleton `CareLinkApp.java` ensures stable Firebase connections.
- **Frontend:** Native Android (Java/XML) optimized for both Phone and Wear OS.
- **Backend:** Firebase Firestore (History/Alerts) & Realtime Database (Live GPS/BPM).
- **Safety:** Bulletproof navigation, null-safety, and session persistence implemented.
