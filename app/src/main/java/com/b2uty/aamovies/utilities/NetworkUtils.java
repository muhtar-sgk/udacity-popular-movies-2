package com.b2uty.aamovies.utilities;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Muhtar 24/7/2017.
 */

public class NetworkUtils {

    public static final String LOG_TAG = NetworkUtils.class.getSimpleName();

    private static OkHttpClient client = new OkHttpClient();

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static String makeHTTPRequest(String finalUrl) {

        Request request = new Request.Builder()
                .url(finalUrl)
                .build();

        String jsonData = null;
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful())
                jsonData = response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (jsonData != null)
            return jsonData;
        else
            return null;
    }
}
