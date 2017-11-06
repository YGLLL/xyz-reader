package com.example.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;
    public static final String APP_LOCATION;
    private static String TAG = Config.class.toString();

    static {
        URL url = null;
        try {
            //https://go.udacity.com/xyz-reader-json
            //https://d17h27t6h515a5.cloudfront.net/topher/2017/March/58c5d68f_xyz-reader/xyz-reader.json
            //http://192.168.43.247/docs/a.json
            url = new URL("http://192.168.43.247/docs/a.json");
        } catch (MalformedURLException ignored) {
            // TODO: throw a real error
            Log.e(TAG, "Please check your internet connection.");
        }

        BASE_URL = url;
        APP_LOCATION="https://raw.githubusercontent.com/YGLLL/xyz-reader/master/apk/XYZReader-debug.apk";
    }
}
