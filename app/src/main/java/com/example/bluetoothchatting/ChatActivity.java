package com.example.bluetoothchatting;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.UUID;

public class ChatActivity extends Activity {

    private static final String NAME = "BluetoothChat";
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private BluetoothAdapter mAdapter;

    BluetoothDevice device;

    String address;
    String chatName;
    PrintWriter out;
    BufferedReader in;

    ScrollView sc;
    LinearLayout chatView;
    EditText msgInput;
    Button connBtn, serverBtn;
    Button sendBtn;
    TextView tv;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        sc = findViewById(R.id.sc);
        chatView = findViewById(R.id.chatView);

        Intent it = getIntent();
        address = it.getStringExtra("DeviceAddress");
        device = mAdapter.getRemoteDevice(address);

        // 내 기기명을 대화명으로 사용
        chatName = mAdapter.getName();

        serverBtn = findViewById(R.id.serverBtn);
        serverBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ServerTask().execute("서버 시작");
                connBtn.setEnabled(false);
            }
        });

        connBtn = findViewById(R.id.connBtn);
        connBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new BackgroundTask(null).execute("클라이언트 시작");
                serverBtn.setEnabled(false);
            }
        });

        msgInput = findViewById(R.id.msgInput);
        sendBtn = findViewById(R.id.sendBtn);


        // 전송버튼 눌렀을 경우 메시지 전송하기
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String msg = msgInput.getText().toString();
                        sendMsg(chatName, msg, 2);
                        msgInput.post(new Runnable() {
                            @Override
                            public void run() {
                                msgInput.setText("");
                            }
                        });

                        tv = new TextView(getApplicationContext());
                        tv.setTextColor(Color.BLACK);
                        tv.setText("["+mAdapter.getName()+"]:"+msg);
                        chatView.post(new Runnable() {
                            @Override
                            public void run() {
                                chatView.addView(tv);
                            }
                        });
                        sc.fullScroll(View.FOCUS_DOWN);
                    }
                }).start();

            }
        });

        //엔터키 눌렀을 때 입력 메시지 전송하기
        msgInput.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if(i ==  KeyEvent.KEYCODE_ENTER && KeyEvent.ACTION_DOWN == keyEvent.getAction())
                {
                    String msg = msgInput.getText().toString();
                    sendMsg(chatName, msg, 2);
                    msgInput.setText("");

                    TextView tv = new TextView(getApplicationContext());
                    tv.setTextColor(Color.BLACK);
                    tv.setText("["+mAdapter.getName()+"]:"+msg);
                    chatView.addView(tv);
                    sc.fullScroll(View.FOCUS_DOWN);
                    return true;
                }
                return false;
            }
        });
    }

    public void sendMsg(String chatName, String msg, int msgType) {
        // 형식에 맞춰 서버에 메시지를 전송
        out.println("[" + chatName + "]" + ":" + msg);
        out.flush();
    }

    //클라이언트 쓰레드
    class BackgroundTask extends AsyncTask<String , String , Integer> {

        BluetoothSocket mmSocket;

        public BackgroundTask(BluetoothSocket socket) {
            this.mmSocket = socket;
        }

        protected void onPreExecute() {
            TextView tv = new TextView(getApplicationContext());

            tv.setText("연결전...");
            chatView.addView(tv);
            sc.fullScroll(View.FOCUS_DOWN);
        }

        protected Integer doInBackground(String ... value) {

            publishProgress("시작전, 연결전");

            try {

                if(mmSocket == null) {
                    mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                    mmSocket.connect();
                    publishProgress("연결되었습니다...");

                }
                in = new BufferedReader(new InputStreamReader(mmSocket.getInputStream()));
                out = new PrintWriter(new OutputStreamWriter(mmSocket.getOutputStream()));

            } catch (IOException e) {
                Log.i("connect", "Connection Error:"+e.getMessage());
                try {
                    mmSocket.close();
                } catch (IOException e2) { }
                return 0;
            }

            while (true) {
                try {
                    String msg = in.readLine();
                    publishProgress(msg);
                } catch (IOException e) { }
            }
        }

        protected void onProgressUpdate(String ... msg) {
            TextView tv = new TextView(getApplicationContext());
            tv.setTextColor(Color.BLUE);
            tv.setText(msg[0]);
            chatView.addView(tv);
            sc.fullScroll(View.FOCUS_DOWN);
        }

        protected void onPostExecute(Integer result) {
            TextView tv = new TextView(getApplicationContext());
            tv.setTextColor(Color.RED);
            tv.setText("연결을 종료합니다.");
            chatView.addView(tv);
            finish();
        }
    }

    //서버 쓰레드
    class ServerTask extends AsyncTask<String , String , Integer> {

        BluetoothServerSocket mmServerSocket;
        BluetoothSocket ssSocket;
        protected void onPreExecute() {

        }

        protected Integer doInBackground(String ... value) {

            try {
                mmServerSocket = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);

            } catch (IOException e) { }

            while(true) {
                try {
                    publishProgress("서버 동작 중..");
                    ssSocket = mmServerSocket.accept();
                    in = new BufferedReader(new InputStreamReader(ssSocket.getInputStream()));
                    out = new PrintWriter(new OutputStreamWriter(ssSocket.getOutputStream()));
                    publishProgress("연결되었습니다...");

                    while (true) {
                        try {
                            String msg = in.readLine();
                            publishProgress(msg);
                        } catch (IOException e) { }
                    }
                } catch(IOException e) {
                    break;
                }
            }

            return 0;
        }

        protected void onProgressUpdate(String ... msg) {
            TextView tv = new TextView(getApplicationContext());
            tv.setTextColor(Color.BLUE);

            tv.setText(msg[0]);
            chatView.addView(tv);
            sc.fullScroll(View.FOCUS_DOWN);
        }

        protected void onPostExecute(Integer result) {
            TextView tv = new TextView(getApplicationContext());
            tv.setTextColor(Color.RED);
            tv.setText("서버를 종료합니다.");
            chatView.addView(tv);
            finish();
        }
    }


}
