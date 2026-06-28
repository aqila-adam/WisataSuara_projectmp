package id.aqilaadam.wisatasuara;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int SPEECH_REQUEST_CODE = 101;

    private ImageButton btnMic;
    private TextView tvResultTitle, tvResultDesc, tvLiveText;
    private Button btnAlarm1, btnAlarm30;

    private TextToSpeech tts;
    private List<Artefak> artefakList = new ArrayList<>();
    private Artefak currentArtefak = null; // Menyimpan artefak terakhir yang dicari
    private AlarmManager alarmManager;

    // Modul 14: Variabel Sensor Accelerometer
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long waktuGuncanganTerakhir = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnMic = findViewById(R.id.btnMic);
        tvResultTitle = findViewById(R.id.tvResultTitle);
        tvResultDesc = findViewById(R.id.tvResultDesc);
        tvLiveText = findViewById(R.id.tvLiveText);
        btnAlarm1 = findViewById(R.id.btnAlarm1);
        btnAlarm30 = findViewById(R.id.btnAlarm30);

        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Modul 14: Inisialisasi Sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        checkPermissions();
        loadArtefakData();
        setupTTS();

        btnMic.setOnClickListener(v -> mulaiMendengarkanDenganGoogle());
        btnAlarm1.setOnClickListener(v -> setPengingatWaktu(1));
        btnAlarm30.setOnClickListener(v -> setPengingatWaktu(30));
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.RECORD_AUDIO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        boolean needsRequest = false;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                needsRequest = true;
                break;
            }
        }
        if (needsRequest) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    // --- MODUL 14: DETEKSI GUNCANGAN SENSOR ACCELEROMETER ---
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Hitung kekuatan guncangan
            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;

            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            // Jika guncangan cukup kuat (angka 2.0 adalah ambang batas guncangan)
            if (gForce > 2.0) {
                long waktuSekarang = System.currentTimeMillis();
                // Mencegah guncangan terdeteksi berkali-kali dalam waktu kurang dari 2 detik
                if (waktuSekarang - waktuGuncanganTerakhir > 2000) {
                    waktuGuncanganTerakhir = waktuSekarang;

                    // Ulangi pembacaan artefak terakhir jika ada
                    if (currentArtefak != null) {
                        Toast.makeText(this, "Mengulang penjelasan...", Toast.LENGTH_SHORT).show();
                        speakText("Saya ulangi. " + currentArtefak.getSejarah());
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Tidak digunakan
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Daftarkan sensor saat aplikasi aktif (Modul 14)
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hentikan sensor saat aplikasi di-background untuk hemat baterai
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    // --- FITUR ALARM (MODUL 13) ---
    private void setPengingatWaktu(int menit) {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }

        speakText("Pengingat waktu " + menit + " menit telah diaktifkan.");
        Toast.makeText(this, "Pengingat " + menit + " Menit Aktif", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("id.aqilaadam.wisatasuara.ALARM_ACTION");
        intent.putExtra("waktu", menit);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long intervalMillis = menit * 60 * 1000L;
        long triggerTime = System.currentTimeMillis() + intervalMillis;

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        }
    }

    // --- FITUR BACA JSON (MODUL 19) ---
    private void loadArtefakData() {
        try {
            InputStream is = getAssets().open("artefak.json");
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();

            String jsonStr = new String(buffer, "UTF-8");
            JSONArray jsonArray = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                artefakList.add(Artefak.fromJsonObject(obj));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- FITUR TEXT TO SPEECH (MODUL 18) ---
    private void setupTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("id", "ID"));
            }
        });
    }

    // --- FITUR SPEECH TO TEXT (MODUL 17) ---
    private void mulaiMendengarkanDenganGoogle() {
        if (tts != null && tts.isSpeaking()) {
            tts.stop();
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Sebutkan nama artefak...");

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Fitur Google Voice Search tidak ditemukan", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spokenText = matches.get(0).toLowerCase();
                tvLiveText.setVisibility(TextView.VISIBLE);
                tvLiveText.setText("Terdengar: \"" + spokenText + "\"");
                processSpokenText(spokenText);
            }
        }
    }

    private void processSpokenText(String text) {
        boolean found = false;
        for (Artefak artefak : artefakList) {
            String namaArtefak = artefak.getNama().toLowerCase();
            if (text.contains(namaArtefak) || namaArtefak.contains(text)) {
                found = true;
                currentArtefak = artefak; // Simpan ke variabel untuk fitur guncangan

                tvResultTitle.setText(artefak.getNama());
                String deskripsi = artefak.getSejarah();
                if (artefak.getDeskripsiVisual() != null && !artefak.getDeskripsiVisual().isEmpty()) {
                    deskripsi += "\n\nDeskripsi: " + artefak.getDeskripsiVisual();
                }
                tvResultDesc.setText(deskripsi);
                speakText(artefak.getSejarah());
                break;
            }
        }
        if (!found) {
            tvResultTitle.setText("Tidak Ditemukan");
            tvResultDesc.setText("Artefak dengan kata kunci '" + text + "' tidak ditemukan.");
            speakText("Maaf, artefak tidak ditemukan.");
        }
    }

    private void speakText(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) tts.shutdown();
        super.onDestroy();
    }
}