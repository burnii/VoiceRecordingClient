package com.example.voicerecording;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;

public class NameActivity extends AppCompatActivity {
    private Socket mSocket = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get Permissions to record audio
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 123);
        }

        Log.d("asd", Build.MODEL);

        setContentView(R.layout.activity_name);
    }

    public void continueButtonClicked(View view) {
        EditText nameEditText = findViewById(R.id.nameEditText);
        String name = nameEditText.getText().toString();
        if(name.equals("")) {
            Toast toast = Toast.makeText(
                    getApplicationContext(),
                    "Es muss ein Name eingegeben werden, um die Sprachaufnahme zu beginnen",
                    Toast.LENGTH_SHORT);
            toast.show();
        } else {

            Thread tcpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    EditText nameEditText = findViewById(R.id.nameEditText);
                    String name = nameEditText.getText().toString();

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

                        System.out.println(config.sampleRate);
                        System.out.println(config.isUdp);

                        Intent intent = new Intent(getBaseContext(), MainActivity.class);
                        intent.putExtra(IntentKey.name, name);

                        startActivity(intent);

                    } catch (IOException e) {
                        e.printStackTrace();

                        // In UI Thread ausführen, um Toast aus einem anderen Thread anzeigen zu können.
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast toast = Toast.makeText(
                                        getApplicationContext(),
                                        "Die Verbindung zum Server konnte nicht hergestellt werden",
                                        Toast.LENGTH_SHORT);

                                toast.show();
                            }
                        });
                    }
                }
            });

            tcpThread.start();
        }
    }


}
