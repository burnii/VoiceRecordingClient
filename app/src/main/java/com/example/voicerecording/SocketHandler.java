package com.example.voicerecording;

import android.content.Context;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;

public class SocketHandler {
    private static Socket mTcpSocket;
    private static DatagramSocket udpSocket;

    public static Socket ensureSocket() throws IOException {
            if(mTcpSocket == null) {
                mTcpSocket = new Socket("192.168.178.45", 4000);
            }



        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    mTcpSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        return mTcpSocket;
    }

    public static DatagramSocket ensureDatagramSocket() throws IOException {
        if(udpSocket == null) {
            udpSocket = new DatagramSocket(4000);
        }

        return udpSocket;
    }
}
