package com.example.voicerecording;

import java.lang.reflect.Array;

public class Helper {
    public static byte[] combineArrays(byte[] arr1, byte[] arr2) {
        byte[] arr = new byte[arr1.length + arr2.length];

        System.arraycopy(arr1, 0, arr, 0, arr1.length);
        System.arraycopy(arr2, 0, arr, arr1.length, arr2.length);

        return arr;
    }
}
