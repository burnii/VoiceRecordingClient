package com.example.voicerecording;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Process;
import android.os.StrictMode;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private boolean isRecording = false;

    private AudioRecord mAudioRecorder = null;
    private int mSampleRate = 44100;
    AudioMessageBuilder messageBuilder = new AudioMessageBuilder(true);

    private Socket mSocket = null;

    private List<Byte> mAudioList = new ArrayList<Byte>();
    public String name = "";

    private boolean isAcknowledged = false;

    private boolean stop = true;

    AlertDialog.Builder dialogBuilder;

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get Permissions to record audio
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 123);
        }

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        buildAlertDialog();

        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if(mAudioList.size() > 0) {
                        if(Config.getInstance().isUdp) {
                            //if(mAudioList.size() > 512) {
                                sendUdp();
                            //}
                        } else {
                            sendTcp();
                        }
                    }
                }
            }
        });

        sendThread.start();

        setContentView(R.layout.activity_main);
    }

    private void startThreads() {
        mAudioRecorder.startRecording();
        stop = false;

        Thread recordingTread = new Thread(new Runnable() {
            @Override
            public void run() {
                int n = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

                mAudioList = new ArrayList<Byte>();
                byte[] data;
                while (!stop) {
                    data = new byte[n];
                    if (AudioRecord.ERROR_INVALID_OPERATION != mAudioRecorder.read(data, 0, n)) {
                        for (byte b : data) {
                            if(isRecording) {
                                mAudioList.add(b);
                            } else {
                                mAudioList.add((byte)0);
                            }

                        }
                    }
                }
            }
        });

        recordingTread.start();
    }

    private void buildAlertDialog() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(60, 20, 20, 20);
        LinearLayout layout = new LinearLayout(this);
        final EditText input = new EditText(this);
        input.setHint("Username");
        input.setLayoutParams(lp);
        input.setWidth(800);
        layout.addView(input);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Join audio meeting");
        builder.setView(layout);
        builder.setPositiveButton("Join", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                name = input.getText().toString();

                if(name.equals("")) {
                    Toast toast = Toast.makeText(
                            getApplicationContext(),
                            "You need to enter a username to start audio recording",
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    acknowledgeClient();

                    if(isAcknowledged) {
                        mSampleRate = Config.getInstance().sampleRate;

                        TextView text = findViewById(R.id.nameTextView);
                        text.setText(name);

                        // initialize audio recorder
                        final int n = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                        mAudioRecorder = new AudioRecord(
                                MediaRecorder.AudioSource.UNPROCESSED,
                                mSampleRate,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                n * 10
                        );

                        try {
                            mSocket = SocketHandler.ensureSocket();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        startThreads();
                    }
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void stopButtonClicked(View view) {
        stop = true;

        SocketHandler.closeTcpSocket();

        buildAlertDialog();
    }

    public void startAudioRecord(View view) {
        if(isRecording == true) {
            isRecording = false;

            ImageButton button = (ImageButton)findViewById(R.id.recordButton);

            button.setImageResource(R.drawable.ic_mic_off_black_24dp);
        } else {
            ImageButton button = (ImageButton)findViewById(R.id.recordButton);

            button.setImageResource(R.drawable.ic_mic_black_24dp);

            isRecording = true;
        }
    }

    public void sendTcp() {
        ArrayList<Byte> temp = new ArrayList<Byte>(mAudioList);
        mAudioList = new ArrayList<Byte>();

        byte[] audioContent = new byte[temp.size()];
        int k = 0;
        for (Byte b : temp) {
            if(b != null) {
                audioContent[k++] = b.byteValue();
            }
        }

        byte[] message = new AudioMessageBuilder(false).build(this.name, audioContent);

        if(audioContent.length > 0) {
            try {
                mSocket.getOutputStream().write(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void acknowledgeClient() {
        try {
            mSocket = SocketHandler.ensureSocket();

            // send acknowledgement
            byte[] acknowledgement = new AudioMessageBuilder(true).buildAcknowledgement(name);
            mSocket.getOutputStream().write(acknowledgement);

            // wait for config
            BufferedReader br = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            String content = br.readLine();
            System.out.println(content);

            //parse config
            Gson gson = new Gson();

            Config config = gson.fromJson(content, Config.class);
            Config.update(config);

            messageBuilder.resetUdpCount();
            isAcknowledged = true;

            System.out.println(config.sampleRate);
            System.out.println(config.isUdp);
        } catch (IOException e) {
            e.printStackTrace();

            // In UI Thread ausführen, um Toast aus einem anderen Thread anzeigen zu können.
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast toast = Toast.makeText(
                            getApplicationContext(),
                            "The connection to the server could not be established",
                            Toast.LENGTH_SHORT);

                    toast.show();
                }
            });
        };
    }

    public void sendUdp() {
        ArrayList<Byte> temp = new ArrayList<Byte>(mAudioList);
        mAudioList = new ArrayList<Byte>();

        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        int size = temp.size();
        byte[] audioContent = new byte[size];

        for(int i = 0; i < temp.size(); i++) {
            Byte value = temp.get(i);
            if(value != null) {
                audioContent[i] = value;
            }
        }

        int steps = 472;

        for(int i = 0; i < audioContent.length; i += steps) {
            int s = i + steps;

            if(s > audioContent.length) {
                s = audioContent.length;
            }

            byte[] contentStep = Arrays.copyOfRange(audioContent, i, s);

            byte[] message = messageBuilder.build(name, contentStep);

            // Ohne kurz zu warten entstehen Störgeräusche TODO prüfen
            try {
                Thread.sleep(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {
                DatagramSocket socket = SocketHandler.ensureDatagramSocket();
                InetAddress serverAddress = InetAddress.getByName("100.119.14.185");

                DatagramPacket packet = new DatagramPacket(message, message.length, serverAddress, 4001);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}