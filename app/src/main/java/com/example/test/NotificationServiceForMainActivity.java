package com.example.test;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.net.Uri;
import android.media.RingtoneManager;
import android.media.AudioAttributes;
import android.os.VibrationAttributes;
import android.os.VibratorManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.media.AudioManager;

import androidx.core.app.NotificationCompat;

public class NotificationServiceForMainActivity {
    private static NotificationServiceForMainActivity instance;
    private static final String CHANNEL_ID = "timer_channel";

    private final NotificationManager notificationManager;

    private final Context context;

    private final AudioManager audioManager;

    public static synchronized NotificationServiceForMainActivity init(Context context) {
        if (instance == null) {
            instance = new NotificationServiceForMainActivity(context);
        }

        return instance;
    }

    public static synchronized NotificationServiceForMainActivity getInstanceForOtherActivity() throws Exception {
        if (instance == null) {
            throw new Exception("메인 엑티비티를 통해 생성된 객체 없음");
        }

        return instance;
    }

    public NotificationServiceForMainActivity(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        createChannelIfNeeded();  // 오레오(API 26) 이상에서 채널 생성
    }

    /**
     * Android O(26) 이상에서는 알림 채널이 반드시 필요합니다.
     */
    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.deleteNotificationChannel(CHANNEL_ID);

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "타이머 알림",
                    NotificationManager.IMPORTANCE_HIGH
            );

            // 알림 채널에 기본 알림음 설정
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(soundUri, audioAttributes);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            // 진동 설정 강화
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500, 200, 500, 200, 500});

            // 무음 모드에서도 진동이 울리도록 플래그 추가 (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                channel.setBypassDnd(true); // 방해 금지 모드 우회
            }

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
                .setContentTitle(title)
                .setContentText(text)
                .setVibrate(new long[]{0, 500})
                .setContentIntent(stopAlarmIntent)
                .setOngoing(true)
                .setAutoCancel(true) // 사용자가 탭하면 자동으로 사라지도록 설정
                .setPriority(NotificationCompat.PRIORITY_HIGH); // 중요도 높음

        // 현재 링거 모드 확인
        int ringerMode = audioManager.getRingerMode();

        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            // 소리 모드: 알림 소리 활성화
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
            startPriorityVibration();
        } else if (ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            // 진동 모드: 소리 비활성화, 진동만
            builder.setSound(null);
            startPriorityVibration();
        } else { // RINGER_MODE_SILENT
            // 무음 모드: 소리 비활성화, 진동만
            builder.setSound(null);
            startPriorityVibration();
        }

        // notify 호출 시 고유 ID를 사용하여 각 알림을 구분
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());

    }

    // 5. 무음 모드에서도 작동하는 진동 메서드
    private void startPriorityVibration() {
        Vibrator vibrator;

        // 안드로이드 13+ 대응
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager manager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = manager.getDefaultVibrator();
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator == null || !vibrator.hasVibrator()) return;

        long[] pattern = {0, 1000, 500}; // 진동 패턴

        // 무음 모드에서도 작동하는 진동 속성 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createWaveform(pattern, 0); // 무한 반복

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+에서 무음 모드 우회
                VibrationAttributes attributes = new VibrationAttributes.Builder()
                        .setUsage(VibrationAttributes.USAGE_ALARM) // 알람 용도로 설정
                        .setFlags(VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY, // The flag
                                VibrationAttributes.FLAG_BYPASS_INTERRUPTION_POLICY)
                        .build();

                vibrator.vibrate(effect, attributes);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10에서 무음 모드 우회
                vibrator.vibrate(effect, new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build());
            } else {
                // Android 8-9
                vibrator.vibrate(effect);
            }
        } else {
            // 레거시 방식 (API 26 미만)
            vibrator.vibrate(pattern, 0);
        }
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
