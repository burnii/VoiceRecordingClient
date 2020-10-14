package com.example.voicerecording;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Process;
import android.os.StrictMode;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
    private String name = "";

    private boolean isUdp;

    public int len = 0;

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //try {
       //     mSocket.close();
       // } catch (IOException e) {
       //     e.printStackTrace();
        //}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        name = getIntent().getStringExtra(IntentKey.name);
        mSampleRate = Config.getInstance().sampleRate;
        isUdp = Config.getInstance().isUdp;

        System.out.print("SAMPLERATE: ");
        System.out.println(mSampleRate);

        System.out.print("ISUDP: ");
        System.out.println(isUdp);

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

        setContentView(R.layout.activity_main);

        TextView nameTextView = findViewById(R.id.nameTextView);
        nameTextView.setText(name);
    }

    public void startAudioRecord(View view) {
        if(isRecording == true) {
            mAudioRecorder.stop();
            isRecording = false;

            ImageButton button = (ImageButton)findViewById(R.id.recordButton);

            button.setImageResource(R.drawable.ic_mic_off_black_24dp);
        } else {
            ImageButton button = (ImageButton)findViewById(R.id.recordButton);

            button.setImageResource(R.drawable.ic_mic_black_24dp);

            mAudioRecorder.startRecording();
            isRecording = true;

            Thread recordingTread = new Thread(new Runnable() {
                @Override
                public void run() {
                    int n = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

                    mAudioList = new ArrayList<Byte>();

                    while (isRecording) {
                        byte[] data = new byte[n];
                        if (AudioRecord.ERROR_INVALID_OPERATION != mAudioRecorder.read(data, 0, n)) {
                            for (byte b : data) {
                                mAudioList.add(b);
                            }
                        }
                    }
                }
            });

            Thread sendThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(isRecording || mAudioList.size() > 0) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        if(isUdp) {
                            if(mAudioList.size() > 512) {
                                sendUdp();
                            }
                        } else {
                            sendTcp();
                        }
                    }
                }
            });

            recordingTread.start();

            sendThread.start();
        }
    }

    public void sendTcp() {
        ArrayList<Byte> temp = new ArrayList<Byte>(mAudioList);
        mAudioList = new ArrayList<Byte>();

        byte[] audioContent = new byte[temp.size()];
        int k = 0;
        for (Byte b : temp) {
            audioContent[k++] = b.byteValue();
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

    public void udp(View view) {
        this.sendUdp();
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

            len++;
            System.out.println("LENGTH: ");
            System.out.println(len);

            // Ohne kurz zu warten entstehen Störgeräusche TODO prüfen
            try {
                Thread.sleep(10);
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