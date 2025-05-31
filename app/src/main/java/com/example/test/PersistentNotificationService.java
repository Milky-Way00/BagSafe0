package com.example.test;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class PersistentNotificationService {
    private static final String CHANNEL_ID = "timer_channel";
    public static final int ONGOING_NOTIFICATION_ID = 1; // 고정된 알림 ID
    private final NotificationManager notificationManager;
    private final Context context;

    public PersistentNotificationService(Context context) {
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
                    "알림 상태",          // 채널 이름 (사용자에게 보여질 텍스트)
                    NotificationManager.IMPORTANCE_LOW  // 긴급도 설정
            );
            channel.setDescription("알림 켜짐/꺼짐 상태 표시");
            // 진동 및 소리 비활성화
            channel.enableVibration(false);
            channel.setVibrationPattern(null);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }


    public static void showNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent stopAlarmIntent = stopAlarm(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification_important_24dp)  // 상태바에 표시될 작은 아이콘
                .setContentTitle("알림이 활성화됨")
                .setContentText("알림을 관리하려면 탭하세요.")
                .setContentIntent(stopAlarmIntent)
                .setOngoing(true)
                .setAutoCancel(false);

        notificationManager.notify(ONGOING_NOTIFICATION_ID, builder.build());

    }

    public static void cancelNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ONGOING_NOTIFICATION_ID);
    }

    public static PendingIntent stopAlarm(Context context) {
        Intent intent = new Intent(context, ControlActivity.class);
        intent.setAction("STOP_ALARM");
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
