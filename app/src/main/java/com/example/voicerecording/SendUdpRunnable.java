package com.example.voicerecording;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class SendUdpRunnable implements Runnable {
    private byte[] content;

    public SendUdpRunnable(byte[] content) {
        this.content = content;
    }

    public synchronized void run() {
        try {
            DatagramSocket socket = SocketHandler.ensureDatagramSocket();
            InetAddress serverAddress = InetAddress.getByName("192.168.178.45");

            DatagramPacket packet = new DatagramPacket(this.content, this.content.length, serverAddress, 4000);
            socket.send(packet);
        }
        catch (SocketException e) {
            Log.e("Udp:", "Socket Error:", e);
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
