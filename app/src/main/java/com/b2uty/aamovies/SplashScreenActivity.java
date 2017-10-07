package com.b2uty.aamovies;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

/**
 * Created by Muhtar on 23/7/2017.
 */

public class SplashScreenActivity extends Activity {

    private static int splashInterval = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.splash);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent apasih = new Intent(SplashScreenActivity.this,MovieGridActivity.class);
                startActivity(apasih);
                this.selesai();
            }
            private void selesai(){
            }
        },splashInterval);

    };
}
