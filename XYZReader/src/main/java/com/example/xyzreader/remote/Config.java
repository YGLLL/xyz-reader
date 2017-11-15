package com.example.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;
    private static String TAG = Config.class.toString();

    static {
        URL url = null;
        try {
            //This API has seriously misled me
            //Udacity should provide the correct API
            //https://go.udacity.com/xyz-reader-json

            //http://192.168.43.247/docs/a.json
            //https://raw.githubusercontent.com/TNTest/xyzreader/master/data.json
            url = new URL("https://raw.githubusercontent.com/TNTest/xyzreader/master/data.json");
        } catch (MalformedURLException ignored) {
            // TODO: throw a real error
            Log.e(TAG, "Please check your internet connection.");
        }

        BASE_URL = url;
    }
}
