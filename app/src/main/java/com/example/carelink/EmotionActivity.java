package com.example.carelink;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Random;

public class EmotionActivity extends AppCompatActivity {

    private TextView tvStatus, tvHeartRate, tvEmotionResult, tvAnalysis;
    private ImageView ivHeart, ivEmotionIcon;
    private Button btnOkay;
    private CardView cardResult;
    private View pulseRing1, pulseRing2, pulseRing3;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Handler handler = new Handler();
    private Random random = new Random();
    private int currentHeartRate = 72;
    private boolean isMonitoring = true;

    private static final int HR_CALM_MIN = 60;
    private static final int HR_CALM_MAX = 75;
    private static final int HR_HAPPY_MIN = 76;
    private static final int HR_HAPPY_MAX = 90;
    private static final int HR_EXCITED_MIN = 91;
    private static final int HR_EXCITED_MAX = 110;
    private static final int HR_STRESSED_MIN = 111;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emotion);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        startHeartRateSimulation();
        animateHeart();
        setupClickListeners();
    }

    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvHeartRate = findViewById(R.id.tvHeartRate);
        tvEmotionResult = findViewById(R.id.tvEmotionResult);
        tvAnalysis = findViewById(R.id.tvAnalysis);
        ivHeart = findViewById(R.id.ivHeart);
        ivEmotionIcon = findViewById(R.id.ivEmotionIcon);
        btnOkay = findViewById(R.id.btnOkay);
        cardResult = findViewById(R.id.cardResult);
        pulseRing1 = findViewById(R.id.pulseRing1);
        pulseRing2 = findViewById(R.id.pulseRing2);
        pulseRing3 = findViewById(R.id.pulseRing3);

        cardResult.setAlpha(0f);
        cardResult.setVisibility(View.GONE);
    }

    private void startHeartRateSimulation() {
        // Simulate real-time heart rate changes
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isMonitoring) return;

                int change = random.nextInt(7) - 3; // -3 to +3
                currentHeartRate += change;

                if (currentHeartRate < 60) currentHeartRate = 60;
                if (currentHeartRate > 130) currentHeartRate = 130;

                tvHeartRate.setText(currentHeartRate + " BPM");

                adjustPulseSpeed();

                handler.postDelayed(this, 1000);
            }
        }, 1000);

        // Analyze emotion after 5 seconds
        handler.postDelayed(() -> {
            isMonitoring = false;
            analyzeEmotion();
        }, 5000);
    }

    private void adjustPulseSpeed() {
        float scale = 60f / currentHeartRate;
    }

    private void animateHeart() {
        // Continuous heartbeat animation
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivHeart, "scaleX", 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivHeart, "scaleY", 1f, 1.2f, 1f);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);

        // Duration based on heart rate (faster = shorter duration)
        long duration = (long) (60000f / currentHeartRate);
        scaleX.setDuration(duration);
        scaleY.setDuration(duration);

        scaleX.start();
        scaleY.start();

        animatePulseRing(pulseRing1, 0);
        animatePulseRing(pulseRing2, 400);
        animatePulseRing(pulseRing3, 800);
    }

    private void animatePulseRing(View ring, long delay) {
        ring.setAlpha(0.6f);
        ring.setScaleX(1f);
        ring.setScaleY(1f);

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ring, "scaleX", 1f, 2.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ring, "scaleY", 1f, 2.5f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(ring, "alpha", 0.6f, 0f);

        scaleX.setDuration(1500);
        scaleY.setDuration(1500);
        alpha.setDuration(1500);

        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        scaleX.setStartDelay(delay);
        scaleY.setStartDelay(delay);
        alpha.setStartDelay(delay);

        scaleX.start();
        scaleY.start();
        alpha.start();
    }

    private void analyzeEmotion() {
        String emotion;
        String analysis;
        int emotionIcon;
        int colorRes;
        int bgGradient;

        if (currentHeartRate >= HR_CALM_MIN && currentHeartRate <= HR_CALM_MAX) {
            emotion = "Peaceful & Calm";
            analysis = "Your heart rate indicates a relaxed state. You seem tranquil and at ease. Perfect time for meditation or light reading!";
            emotionIcon = R.drawable.ic_calm;
            colorRes = R.color.calm_blue;
            bgGradient = R.drawable.gradient_calm;
        } else if (currentHeartRate >= HR_HAPPY_MIN && currentHeartRate <= HR_HAPPY_MAX) {
            emotion = "Happy & Content";
            analysis = "Your gentle elevated heart rate suggests you're feeling positive and joyful. You're in a great mood! Keep spreading those good vibes!";
            emotionIcon = R.drawable.ic_happy;
            colorRes = R.color.happy_yellow;
            bgGradient = R.drawable.gradient_happy;
        } else if (currentHeartRate >= HR_EXCITED_MIN && currentHeartRate <= HR_EXCITED_MAX) {
            emotion = "Excited & Energetic";
            analysis = "Your elevated heart rate shows you're energized! Whether it's excitement or anticipation, you're ready to take on the world!";
            emotionIcon = R.drawable.ic_excited;
            colorRes = R.color.excited_orange;
            bgGradient = R.drawable.gradient_excited;
        } else if (currentHeartRate >= HR_STRESSED_MIN) {
            emotion = "Stressed / Anxious";
            analysis = "Your heart rate is quite elevated. You might be feeling stressed or anxious. Try taking deep breaths - inhale for 4 counts, hold for 4, exhale for 4.";
            emotionIcon = R.drawable.ic_stressed;
            colorRes = R.color.stressed_red;
            bgGradient = R.drawable.gradient_stressed;
        } else {
            emotion = "Relaxed / Sleepy";
            analysis = "Your heart rate is quite low. You might be very relaxed or feeling drowsy. Great for a power nap!";
            emotionIcon = R.drawable.ic_sleepy;
            colorRes = R.color.sleepy_purple;
            bgGradient = R.drawable.gradient_sleepy;
        }

        tvEmotionResult.setText(emotion);
        tvAnalysis.setText(analysis);
        ivEmotionIcon.setImageResource(emotionIcon);

        int color = ContextCompat.getColor(this, colorRes);
        tvEmotionResult.setTextColor(color);
        btnOkay.setBackgroundColor(color);

        cardResult.setBackgroundResource(bgGradient);

        cardResult.setVisibility(View.VISIBLE);
        cardResult.animate()
                .alpha(1f)
                .setDuration(800)
                .start();

        tvStatus.animate().alpha(0f).setDuration(500).start();

        ivHeart.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .start();

        saveEmotionData(emotion, currentHeartRate);
    }

    private void saveEmotionData(String emotion, int heartRate) {
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();

            EmotionRecord record = new EmotionRecord(
                    System.currentTimeMillis(),
                    emotion,
                    heartRate,
                    java.text.DateFormat.getDateTimeInstance().format(new java.util.Date())
            );

            db.collection("users")
                    .document(userId)
                    .collection("emotion_history")
                    .add(record);
        }
    }

    private void setupClickListeners() {
        btnOkay.setOnClickListener(v -> {
            cardResult.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction(() -> {
                        Intent intent = new Intent(EmotionActivity.this, DashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    })
                    .start();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isMonitoring = false;
        handler.removeCallbacksAndMessages(null);
    }

    public static class EmotionRecord {
        public long timestamp;
        public String emotion;
        public int heartRate;
        public String dateTime;

        public EmotionRecord() {}

        public EmotionRecord(long timestamp, String emotion, int heartRate, String dateTime) {
            this.timestamp = timestamp;
            this.emotion = emotion;
            this.heartRate = heartRate;
            this.dateTime = dateTime;
        }
    }
}