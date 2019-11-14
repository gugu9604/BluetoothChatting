package com.example.bluetoothchatting;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends Activity {

    static final int REQUEST_ENABLE_BT = 1;
    Button onBtn, scanBtn;
    ListView deviceListView;

    static BluetoothAdapter mbluetoothAdapter = null;
    private ArrayAdapter<String> devicesArrayAdapter;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        onBtn = findViewById(R.id.onBtn);
        scanBtn = findViewById(R.id.scanBtn);
        deviceListView = findViewById(R.id.deviceListView);

        // 블루투스 지원여부 확인. 블루투스 어댑터 객체
        mbluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mbluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않습니다.",
                    Toast.LENGTH_LONG).show();
        }

        onBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!mbluetoothAdapter.isEnabled()) {
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    //새로 띄운 액티비티로부터 응답을 받아야 할 경우
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                    // Otherwise, setup the chat session
                } else  {
                    Toast.makeText(getApplicationContext(), "블루투스가 켜져 있습니다.",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        devicesArrayAdapter = new ArrayAdapter<>(this, R.layout.device_name);
        deviceListView.setAdapter(devicesArrayAdapter);
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
                // 기기를선택하면 진행하고 있던 탐색을 중지함
                mbluetoothAdapter.cancelDiscovery();

                // Get the device MAC address, which is the last 17 chars in the View
                String info = ((TextView) v).getText().toString();
                String address = info.substring(info.length() - 17);

                // Create the result Intent and include the MAC address
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra("DeviceAddress", address);
                startActivity(intent);
            }
        });

        //다른 장치에서 사용자의 기기를 찾을 수 있는 권한 요청. 다른 장치에서 내 기기를 검색 가능하도록 설정
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        //기기 스캔 버튼을 눌렀을 경우 블루투스 기기 탐색
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                devicesArrayAdapter.clear();

                // 이미 페어링되어 있는 기기들의 리스트를 가지고 옴
                Set<BluetoothDevice> pairedDevices = mbluetoothAdapter.getBondedDevices();
                if (pairedDevices.size() > 0) {
                    // Loop through paired devices
                    for (BluetoothDevice device : pairedDevices) {
                        // Add the name and address to an array adapter to show in a ListView
                        devicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        //리스트 목록갱신
                        devicesArrayAdapter.notifyDataSetChanged();
                    }
                }

                // 안드로이드 6.0 이상 기기에서 기기들의 스캔을 위해 coarse_location 퍼미션 요청 필요
                int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                //scanBtn.setEnabled(false);
                if (mbluetoothAdapter.isDiscovering()) {
                    mbluetoothAdapter.cancelDiscovery();
                }
                mbluetoothAdapter.startDiscovery();
                //Toast.makeText(getApplicationContext(), "기기를 검색합니다.", Toast.LENGTH_LONG).show();

            }
        });

        // 기기가 발견되었을 때 브로드캐스트를 위해 등록 - 인텐트필터 설정
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
    }

    //새로 띄운 액티비티로부터의 응답을 처리하는 메소
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "블루투스를 ON 시킵니다.",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "블루투스를 ON 없습니다.",
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
    // 탐색한 기기들이 발견되었을 때 안드로이드로부터 방송을 수신
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 탐색한 기기가 발견되었을 경우
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                devicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                // When discovery is finished, change the Activity title
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        this.unregisterReceiver(mReceiver);
    }
}
