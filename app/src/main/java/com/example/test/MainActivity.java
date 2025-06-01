package com.example.test;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 100;
    private BluetoothAdapter bluetoothAdapter;
    private String selectedDeviceAddress;
    private BluetoothSocket currentSocket;

    private Button btnConnect;
    private boolean isConnected = false;
    private boolean isSystemEnabled = false;

    private static final int REQUEST_ALL_PERMISSIONS = 1000;

    private final ActivityResultLauncher<Intent> bluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    loadPairedDevices(); // 블루투스 활성화 성공 시 장치 목록 갱신
                    Toast.makeText(this, "블루투스 활성화됨", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "블루투스 활성화 거부됨", Toast.LENGTH_SHORT).show();
                    finish();
                }

            });


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 메인 액티비티를 위한 알림서비스를 생성하지 않고, 블루투스 서비스를 사용하면 안됨
        NotificationServiceForMainActivity.init(this);


        setContentView(R.layout.activity_main);
        checkAllPermissions();

//        startBluetoothDataReceiver();

        System.out.println(BluetoothService.getInstance().toString());
        BluetoothService.getInstance().startMonitoring();


        btnConnect = findViewById(R.id.btn_connect);
        btnConnect.setOnClickListener(v -> {
            if (isConnected) {
                // 연결 해제 로직 (BluetoothService.disconnect() 호출)
                BluetoothService.getInstance().disconnect();
                isConnected = false;
                btnConnect.setText("장치 연결");
            } else {
                // DeviceListActivity로 이동
                Intent intent = new Intent(MainActivity.this, DeviceListActivity.class);
//                intent.
                startActivityForResult(intent, 1);
            }
        });

        // 블루투스 어댑터 초기화
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "이 기기는 블루투스를 지원하지 않습니다.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 블루투스 활성화 상태 확인
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            bluetoothLauncher.launch(enableBtIntent);
        } else {
            loadPairedDevices();
        }
        checkReceivedData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // 알림 클릭으로 인한 인텐트인지 확인
        if (intent != null && "STOP_ALARM".equals(intent.getAction())) {
            PersistentNotificationService.cancelNotification(this);
            BluetoothService.getInstance().sendCommand("0");
            BluetoothService.getInstance().disconnect();

            // 진동 중지
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.cancel();
            }

            // 모니터링 재시작
            BluetoothService.getInstance().startMonitoring();
        }
    }



    private void checkReceivedData() {
        new Thread(() -> {
            while (true) {
                if (BluetoothService.isConnected()) {

                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.e("MainActivity", "스레드 중단됨", e);
                }
            }
        }).start();
    }

    private void toggleSystem() {
        if (!isConnected) {
            Toast.makeText(this, "먼저 장치를 연결하세요", Toast.LENGTH_SHORT).show();
            return;
        }
        isSystemEnabled = !isSystemEnabled;
        sendBluetoothData(isSystemEnabled ? "1" : "0");
        updateUI();
    }

    private void updateUI() {
        runOnUiThread(() -> {
            // btnPower.setText(isSystemEnabled ? "시스템 끄기" : "시스템 켜기");
            // btnPower.setBackgroundColor(isSystemEnabled ? Color.RED : Color.GREEN);
            btnConnect.setText(isConnected ? "연결 해제" : "연결 시작");
            //btnReset.setEnabled(isConnected); // 연결 상태에 따라 버튼 활성화
        });
    }

    private void sendBluetoothData(String data) {
        if (BluetoothService.getInstance().getSocket() != null && !BluetoothService.isConnected()) {
            Toast.makeText(this, "장치가 연결되지 않았습니다", Toast.LENGTH_SHORT).show();
            return;
        }
        BluetoothService.getInstance().sendCommand(data);
    }

    private void loadPairedDevices() {

        try {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            List<String> deviceList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                deviceList.add(device.getName() + "\n" + device.getAddress());
            }
            //deviceAdapter.clear();
            //deviceAdapter.addAll(deviceList);
        } catch (SecurityException e) {
            Log.e("BluetoothError", "SecurityException: " + e.getMessage());
        }
    }

    private void toggleConnection() {
        if (isConnected) {
            disconnectDevice();
        } else {
            if (selectedDeviceAddress != null) {
                connectToDevice(selectedDeviceAddress);
            } else {
                Toast.makeText(this, "먼저 장치를 선택해주세요.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void connectToDevice(String deviceAddress) {

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            BluetoothService.connect(deviceAddress); // BluetoothService 사용
            isConnected = true;
            runOnUiThread(() -> {
                btnConnect.setText("연결 해제");
                Toast.makeText(this, "연결 성공!", Toast.LENGTH_SHORT).show();
                updateUI();

                // ControlActivity 시작 추가
                Intent intent = new Intent(MainActivity.this, ControlActivity.class);
                intent.putExtra("DEVICE_ADDRESS", deviceAddress);
                startActivity(intent);
            });
        } catch (SecurityException e) {
            runOnUiThread(() -> Toast.makeText(this, "권한 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            Log.e("BluetoothError", "SecurityException: " + e.getMessage());
        }
    }

    private void disconnectDevice() {
        try {
            if (currentSocket != null) {
                currentSocket.close();
                isConnected = false;
                runOnUiThread(() -> {
                    btnConnect.setText("연결 시작");  // btnToggle → btnConnect로 변경
                    Toast.makeText(this, "연결 해제됨", Toast.LENGTH_SHORT).show();
                    updateUI();  // UI 상태 업데이트 추가
                });
            }
        } catch (IOException e) {
            Log.e("BluetoothError", "IOException: " + e.getMessage());
        }
    }

    private void checkAllPermissions() {
        List<String> requiredPermissions = new ArrayList<>();

        // 블루투스 관련 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }

        // 위치 관련 권한
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        // 푸시 알림 관련 권한
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        // 권한 요청
        if (!requiredPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    requiredPermissions.toArray(new String[0]),
                    REQUEST_ALL_PERMISSIONS // 새로운 상수 정의 (예: 1000)
            );
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ALL_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // 모든 권한 허용됨
                loadPairedDevices();
            } else {
                // 권한 거부 처리
                Toast.makeText(this, "모든 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            isConnected = true;
            btnConnect.setText("연결 해제");
            // ControlActivity로 이동
            String deviceAddress = data.getStringExtra("DEVICE_ADDRESS");
            Intent controlIntent = new Intent(this, ControlActivity.class);
            controlIntent.putExtra("DEVICE_ADDRESS", deviceAddress);
            startActivity(controlIntent);
        }
    }
}