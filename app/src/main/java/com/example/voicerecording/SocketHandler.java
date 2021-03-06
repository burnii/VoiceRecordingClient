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
                mTcpSocket = new Socket("100.119.14.185", 4001);
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

    public static void closeTcpSocket() {
        try {
            if(mTcpSocket != null) {
                mTcpSocket.close();
                mTcpSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static DatagramSocket ensureDatagramSocket() throws IOException {
        if(udpSocket == null) {
            udpSocket = new DatagramSocket(4001);
        }

        return udpSocket;
    }
}
