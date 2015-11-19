package me.genesis.gsandroid.util;

import android.util.Log;

/**
 * Created by genesisli on 2015/11/19.
 */
public class LogUtil {

    public static void v(String tag, String msg) {
        Log.v(tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
    }

}
