# CareLink+ (SK Pinji Edition)
**Tagline:** Smart care, Safe life.

## Overview
CareLink+ is a predictive healthcare monitoring system designed specifically for the elderly and special needs students at **SK Pinji, Ipoh**. Unlike reactive apps, CareLink+ acts as a 24/7 "Secondary Guardian," utilizing real-time data to anticipate medical emergencies.

---

## 🚀 Daily Development Routine
To bypass the Firebase Spark plan limits and use the local backend, follow this sequence:

1. **Terminal:** 
   ```bash
   cd ~/Documents/carelinkapps_
   npx firebase emulators:start --import ./my_data --export-on-exit ./my_data
   ```
2. **Android Studio:** 
   * Select your **Wear OS Emulator** (Student) or **Phone Emulator** (Guardian).
   * Click the green **Run** button.
3. **Browser:** Open [http://localhost:4000](http://localhost:4000) to view live data.

---

## ✨ Key Features Implemented

### 1. 🛡️ Predictive Care (Watch Mode)
- **Auto-Detection:** App recognizes Wear OS hardware and skips to the Watch Interface.
- **Easy Pairing:** Zero typing required. Student shows a QR code; Teacher scans it to link.
- **HUGE SOS Button:** One-tap emergency trigger for students in distress.
- **Vitals Streaming:** Automatically pushes heart rate data to the cloud every 30 seconds.

### 2. 👩‍🏫 Guardian Dashboard (Teacher Mode)
- **Role-Based Redirect:** Teachers get a full dashboard with maps and doctor bookings.
- **Student Monitoring:** Dashboard tracks how many students are active (e.g., "Monitoring 3 students").
- **Live SOS Alerts:** High-priority pop-ups if a student presses HELP or has a high heart rate.
- **Student Switching:** A dropdown on the map allows teachers to switch between different students instantly.

### 3. 📍 Ipoh Localization
- **Local Hospitals:** Booking directory features KPJ Ipoh, Pantai Hospital, and Hospital Raja Permaisuri Bainun.
- **Local Doctors:** Features Malay names like Dr. Ahmad Zaki and Dr. Siti Noraini.
- **Map Focus:** Defaults to Ipoh coordinates for an authentic local demo.

### 4. 👤 Cloud Profile Sync
- **Live Sync:** Username and Profile Role are fetched from Firestore.
- **Editable Info:** Teachers can update their name, phone, and birthday (using a DatePicker).
- **Stable Navigation:** All back buttons and bottom navigation items are 100% wired.

---

## 🛠️ Technical Backend
- **Global Config:** `CareLinkApp.java` handles emulator connections globally to prevent crashes.
- **Database:** Uses Firestore for profile/alerts and Realtime Database for GPS/Pulse.
- **Stability:** Null-checks and loading spinners implemented across all major activities.
