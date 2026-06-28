package id.aqilaadam.wisatasuara;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "wisata_reminder_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        int menit = intent.getIntExtra("waktu", 0);
        String pesan = "Waktu tur Anda sudah berlalu " + menit + " menit. Tetap waspada.";

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pengingat Wisata",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifikasi untuk tunanetra");
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Pengingat Waktu Tur")
                .setContentText(pesan)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (notificationManager != null) {
            notificationManager.notify(101, builder.build());
        }
    }
}