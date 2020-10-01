package com.example.voicerecording;

import android.os.Build;

import androidx.core.content.res.TypedArrayUtils;

public class AudioMessageBuilder {
    public boolean isUdp;

    public AudioMessageBuilder(boolean isUdp) {
        this.isUdp = isUdp;
    }

    public byte[] build(String username, byte[] content) {
        byte[] message;

        if(this.isUdp) {
            message = this.buildUdpMessage(username, content);
        } else {
            message = this.buildTcpMessage(content);
        }

        return message;
    }

    private byte[] buildUdpMessage(String username, byte[] content) {
        byte[] ack = this.buildAcknowledgement((username));

        byte[] message = Helper.combineArrays(ack, content);

        return message;
    }

    private byte[] buildTcpMessage(byte[] content) {
        return content;
    }

    public byte[] buildAcknowledgement(String username) {
        StringBuilder ack = new StringBuilder(username + ";" + System.currentTimeMillis() + ";" + Build.MODEL);

        while(ack.length() < 25) {
            ack.append(" ");
        }

        String value = ack.toString();

        if(ack.length() > 25) {
            value = ack.substring(0, 25);
        }

        return value.getBytes();
    }
}
