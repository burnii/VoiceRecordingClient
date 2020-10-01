package com.example.voicerecording;

public class Config {
    public int sampleRate;
    public boolean isUdp;

    private static final Config instance = new Config();

    private Config(){}

    public static Config getInstance(){
        return instance;
    }

    public static void update(Config config) {
        instance.sampleRate = config.sampleRate;
        instance.isUdp = config.isUdp;
    }
}
