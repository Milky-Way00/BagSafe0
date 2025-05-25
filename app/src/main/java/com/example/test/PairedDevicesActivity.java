package com.example.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Set;


public class PairedDevicesActivity extends AppCompatActivity {

    private ListView listPairedDevices;
    private ArrayAdapter<BluetoothDevice> pairedDeviceAdapter;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paired_devices);

        listPairedDevices = findViewById(R.id.list_paired_devices);

        pairedDeviceAdapter = new ArrayAdapter<>(this, R.layout.list_item_device){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                BluetoothDevice device = getItem(position);
                ((TextView) view).setText(device.getName()); // 이름만 표시
                return view;
            }



        };

        listPairedDevices.setAdapter(pairedDeviceAdapter);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        loadPairedDevices();


//        pairedDeviceAdapter = new ArrayAdapter<>(
//                this,
//                R.layout.list_item_device, // 커스텀 레이아웃 사용
//                R.id.device_name           // 데이터를 표시할 TextView ID
//        );





        listPairedDevices.setOnItemClickListener((parent, view, position, id) -> {

            BluetoothDevice device = pairedDeviceAdapter.getItem(position);

//            String deviceInfo = pairedDeviceAdapter.getItem(position);
//            String deviceAddress = deviceInfo.split("\n")[1];
            showConnectionDialog(device.getAddress());
        });
    }

    private void loadPairedDevices() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();


        for (BluetoothDevice device : pairedDevices) {
              pairedDeviceAdapter.add(device);
        }
    }

    private void showConnectionDialog(String deviceAddress) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_confirm, null);
        builder.setView(dialogView);

        Button btnYes = dialogView.findViewById(R.id.btn_yes);
        Button btnNo = dialogView.findViewById(R.id.btn_no);

        AlertDialog dialog = builder.create();
        dialog.show();

        btnYes.setOnClickListener(v -> {
            connectToDevice(deviceAddress);
            dialog.dismiss();
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());
    }

    private void connectToDevice(String deviceAddress) {
        BluetoothService.connect(deviceAddress);
        if (BluetoothService.isConnected()) {
            Toast.makeText(this, "연결되었습니다!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, ControlActivity.class);
            intent.putExtra("DEVICE_ADDRESS", deviceAddress);
            startActivity(intent); // ✅ 연결 성공 시에만 화면 전환
        } else {
            Toast.makeText(this, "연결에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}