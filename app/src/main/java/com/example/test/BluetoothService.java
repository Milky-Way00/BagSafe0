package com.example.test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService {

    private static BluetoothService instance;

    private BluetoothSocket socket;
    private volatile boolean isMonitoringThreadRunning = false;
    private Thread monitoringThread;
    private Thread sendingThread;

    public static synchronized BluetoothService getInstance() {

        if (instance == null) {
            instance = new BluetoothService();
        }
        return instance;
    }


    // 1. 연결 상태 확인 메서드 추가
    public static boolean isConnected() {
        return instance.socket != null && instance.socket.isConnected(); // ✅ isClosed() 제거
    }

    // 2. 연결 해제 메서드 추가
    public void disconnect() {
        isMonitoringThreadRunning = false;

        if (monitoringThread != null) monitoringThread.interrupt();

        try {
            if (instance.socket != null) {
                instance.socket.close();
                instance.socket = null;


            }
        } catch (IOException e) {
            Log.e("BT", "연결 해제 실패", e);
        }
    }

    // 3. 기존 connect() 메서드 유지
    public static void connect(String address) {
        try {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = adapter.getRemoteDevice(address);
            instance.socket = device.createRfcommSocketToServiceRecord(
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
            );
            instance.socket.connect(); // 실제 연결 시도
        } catch (IOException e) {
            Log.e("BT", "연결 실패", e);
        }
    }

    // 4. 명령 전송 메서드 개선
    public void sendCommand(String cmd) {
        if (instance.socket == null) {
            System.out.println("sendCommand: 소켓이 닫혔습니다");
            return;
        }

        try {
            OutputStream out = instance.socket.getOutputStream();
            out.write(cmd.getBytes());
        } catch (IOException e) {
            Log.e("BT", "전송 실패", e);
        }
//        if (sendingThread != null && sendingThread.isAlive()) return;
//
//        sendingThread = new Thread(() -> {
//            try {
//                OutputStream out = instance.socket.getOutputStream();
//                out.write(cmd.getBytes());
//            } catch (IOException e) {
//                Log.e("BT", "전송 실패", e);
//            }
//        });
//        sendingThread.start();
    }
    
    // TODO: 스레드가 절대 멈추지 않음
    public void startMonitoring() {
        if (isMonitoringThreadRunning) return;
        isMonitoringThreadRunning = true;

//        isRunning = true;
        monitoringThread = new Thread(() -> {
            while(true) {
                if (instance.socket != null && instance.socket.isConnected()) {
                    try {
                        InputStream in = instance.socket.getInputStream();
                        InputStreamReader inputStreamReader = new InputStreamReader(in);
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                        // UI 업데이트
                        String receivedData;
                        while (isMonitoringThreadRunning && ((receivedData = bufferedReader.readLine()) != null)) { // 줄 단위로 읽기
                            final String finalData = receivedData.trim(); // "\r\n" 제거

                            System.out.println("파이날: " + finalData);

                            if (finalData.equals("ALARM")) {
//                                NotificationService notificationService = new NotificationService(context);
                                NotificationServiceForMainActivity.getInstanceForOtherActivity().showNotification("\uD83D\uDEA8 긴급 경보!", "\uD83D\uDEA8 도난이 발생하였습니다!!!!! \uD83D\uDEA8");
                            }
//                            runOnUiThread(() -> {
//                                if (finalData.equals("ALARM")) {
//                                    Toast.makeText(this, "알람 발생!", Toast.LENGTH_SHORT).show();
//                                    Log.d("BT_ALARM", "수신: " + finalData);
//
//                                }
//                            });
                        }

                    } catch (IOException e) {
                        System.out.println("모니터링 IO 예외");
                        e.printStackTrace();
                        //                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        System.out.println("모니터링 알람 서비스 실패");
                        e.printStackTrace();
                    }

                }

                try {
                    Thread.sleep(500); // CPU 부하 줄이기
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        monitoringThread.start();
    }


    public BluetoothSocket getSocket() {
        return socket;
    }

}