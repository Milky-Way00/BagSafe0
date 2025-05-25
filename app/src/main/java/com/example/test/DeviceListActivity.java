package com.example.test;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.example.test.R;
import android.view.View;
import com.example.test.ControlActivity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import java.lang.reflect.Method;
import android.util.Log;
import androidx.core.content.ContextCompat;
import java.lang.reflect.InvocationTargetException;


public class DeviceListActivity extends AppCompatActivity {

    private ListView listDevices;
    private ArrayAdapter<BluetoothDevice> deviceAdapter;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver receiver;

    @Override



    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        listDevices = findViewById(R.id.list_devices);

        deviceAdapter = new ArrayAdapter<BluetoothDevice>(this, R.layout.list_item_device) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                BluetoothDevice device = getItem(position);
                ((TextView) view).setText(device.getName()); // 이름만 표시
                return view;
            }
        };

        listDevices.setAdapter(deviceAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // 페어링된 기기 로드
        loadPairedDevices();

        // 주변 기기 검색 시작
        startDiscovery();

        // "페어링된 기기 보기" 버튼 추가
        Button btnShowPaired = findViewById(R.id.btn_show_paired);
        btnShowPaired.setOnClickListener(v -> {
            Intent intent = new Intent(this, PairedDevicesActivity.class);
            startActivity(intent);
        });

        // 장치 클릭 시 다이얼로그 표시
        listDevices.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice device = deviceAdapter.getItem(position);
//            String deviceAddress = deviceInfo.split("\n")[1];
//            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            // ✅ 페어링 상태 확인
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Toast.makeText(this, "이미 페어링된 기기입니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            else {
                // 페어링되지 않은 기기: 페어링 다이얼로그 표시
                showPairingDialog(device.getAddress());
            }
        });
    }

    private void loadPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            deviceAdapter.add(device);
        }
    }

    // 주변 기기 검색 시작
    private void startDiscovery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "권한이 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        bluetoothAdapter.startDiscovery();

        // 브로드캐스트 리시버 등록
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

                        // ✅ 이름이 null 또는 빈 문자열인 경우 필터링
                        if (device.getName() == null || device.getName().isEmpty()) {
                            return; // 리스트에 추가하지 않음
                        }


                        deviceAdapter.add(device);
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //Toast.makeText(DeviceListActivity.this, "검색 완료", Toast.LENGTH_SHORT).show();
                }
            }
        };
        registerReceiver(receiver, filter);
    }

    // 페어링 다이얼로그 표시
    private void showPairingDialog(String deviceAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("페어링 요청")
                .setMessage("기기와 페어링하시겠습니까?")
                .setPositiveButton("예", (dialog, which) -> pairDevice(deviceAddress))
                .setNegativeButton("아니오", null)
                .show();
    }


    // ✅ 연결 확인 다이얼로그 추가
    private void showConnectionDialog(String deviceAddress) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        builder.setView(dialogView);

        Button btnYes = dialogView.findViewById(R.id.btn_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_no);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnYes.setOnClickListener(v -> {
            boolean isConnected = connectToDevice(deviceAddress); // ✅ 연결 시도
            if (isConnected) {
                dialog.dismiss();
                startActivity(new Intent(this, ControlActivity.class)); // 성공 시 이동
            } else {
                Toast.makeText(this, "연결을 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
    }



    // ✅ 아두이노와 블루투스 연결
    private boolean connectToDevice(String deviceAddress) {
        BluetoothService.connect(deviceAddress);
        if (BluetoothService.isConnected()) {
            Toast.makeText(this, "연결되었습니다!", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Toast.makeText(this, "연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    // 기기 페어링 시도
    private void pairDevice(String deviceAddress) {
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);

        try {
            Method method = device.getClass().getMethod("createBond");
            method.invoke(device);
        } catch (NoSuchMethodException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException e) {
            Log.e("BT", "페어링 실패", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
    }



}