package com.example.test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.view.View;

public class ControlActivity extends AppCompatActivity {
    private String deviceAddress;

    private boolean btn_color = true;

    private boolean isSystemEnabled = false;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);

        deviceAddress = getIntent().getStringExtra("DEVICE_ADDRESS");

        Button btnPower = findViewById(R.id.btn_power);
        Button btnDisconnect = findViewById(R.id.btn_disconnect);

        btnPower.setOnClickListener(v -> {
            isSystemEnabled = !isSystemEnabled;
            BluetoothService.getInstance().sendCommand(isSystemEnabled ? "1" : "0");

            btn_color = !btn_color;

            //Button btnPower = findViewById(R.id.btn_power);

            if (btn_color) {
                btnPower.setBackground(
                        ContextCompat.getDrawable(
                                ControlActivity.this,

                                R.drawable.btn_able
                        )
                );

                btnPower.setText("알림 켜기");

            } else {
                btnPower.setBackground(
                        ContextCompat.getDrawable(
                                ControlActivity.this,
                                R.drawable.btn_disable
                        )
                );

                btnPower.setText("알림 끄기");

            }

            try {
                Vibrator vibrator = (Vibrator) NotificationServiceForMainActivity.getInstanceForOtherActivity().getContext().getSystemService(VIBRATOR_SERVICE);

                if (vibrator != null) {
                    vibrator.cancel();
                }
            } catch (Exception e) {
//                throw new RuntimeException(e);
                System.out.println("메인 액티비티 알림 서비스 생성되지 않음!!!");
                e.printStackTrace();
            }

        });


        // 연결 해제 버튼
        btnDisconnect.setOnClickListener(v -> {
            sendStopAlarmCommand();
        });
    }

//                try {
//        Vibrator vibrator = (Vibrator) NotificationServiceForMainActivity.getInstanceForOtherActivity().getContext().getSystemService(VIBRATOR_SERVICE);
//
//        if (vibrator != null) {
//            vibrator.cancel();
//        }
//    } catch (Exception e) {
////                throw new RuntimeException(e);
//        System.out.println("메인 액티비티 알림 서비스 생성되지 않음!!!");
//        e.printStackTrace();
//    }
//    sendStopAlarmCommand();
//});

    // 블루투스 연결 해제 메서드
    private void disconnectBluetooth() {
        if (BluetoothService.isConnected()) {
            try {
                BluetoothService.getInstance().disconnect();
                runOnUiThread(() ->
                        Toast.makeText(this, "연결이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                );

                // 메인 화면으로 이동
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } catch (Exception e) {
                Log.e("ControlActivity", "연결 해제 실패", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "연결 해제에 실패했습니다.", Toast.LENGTH_SHORT).show()
                );
            }
        } else {
            runOnUiThread(() ->
                    Toast.makeText(this, "이미 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show()
            );
        }
    }

    // 부저 중지 명령 전송 메서드
    private void sendStopAlarmCommand() {
//        BluetoothService.getInstance().sendCommand("0"); // 부저 중지
        try {
            NotificationServiceForMainActivity.stopAlarm(this).send();
            Toast.makeText(this, "연결이 해제되었습니다.", Toast.LENGTH_SHORT).show();
        } catch (PendingIntent.CanceledException e) {
            throw new RuntimeException(e);
            // TODO: 예외 처리
        }

//        BluetoothService.getInstance().stopMonitoring();

//        BluetoothService.getInstance().disconnect();


//        // 메인 화면으로 이동 (스택 정리)
//        Intent intent = new Intent(this, MainActivity.class);
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
//        startActivity(intent);
//        finish(); // 현재 액티비티 종료
    }

}