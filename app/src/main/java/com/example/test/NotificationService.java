package com.example.test;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import androidx.core.app.NotificationCompat;

public class NotificationService {
    private static final String CHANNEL_ID = "timer_channel";

    private final NotificationManager notificationManager;

    private final Context context;

    public NotificationService(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannelIfNeeded();  // 오레오(API 26) 이상에서 채널 생성
    }

    /**
     * Android O(26) 이상에서는 알림 채널이 반드시 필요합니다.
     */
    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "타이머 알림",          // 채널 이름 (사용자에게 보여질 텍스트)
                    NotificationManager.IMPORTANCE_HIGH  // 긴급도 설정
            );
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 500, 500, 500, 500});
            notificationManager.createNotificationChannel(channel);
        }
    }

    /**
     * 실제 알림(Notification)을 생성하고 표시합니다.
     *
     * @param title 알림 제목
     * @param text  알림 내용
     */
    public void showNotification(String title, String text) {
        PendingIntent stopAlarmIntent = stopAlarm(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_important_24dp)  // 상태바에 표시될 작은 아이콘
//                .setsmall
//                .setLargeIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setVibrate(new long[]{0, 500})
                .setContentIntent(stopAlarmIntent)
                .setOngoing(true)
                .setAutoCancel(true); // 사용자가 탭하면 자동으로 사라지도록 설정

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 1000, 500};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0); // 0: 무한 반복
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(pattern, 0); // 0: 무한 반복 (Deprecated in API 26)
        }

        // notify 호출 시 고유 ID를 사용하여 각 알림을 구분
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    public static PendingIntent stopAlarm(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction("STOP_ALARM"); // 커스텀 액션 정의
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return pendingIntent;
    }

    public Context getContext() {
        return context;
    }

}
