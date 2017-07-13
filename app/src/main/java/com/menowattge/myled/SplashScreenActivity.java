package com.menowattge.myled;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash_screen);


        final Thread timeout = new Thread() {
        @Override
            public void run() {
            super.run();

            try {
                sleep(1 * 1000);  // messo 1 per comodità
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        };

        timeout.start();



    }
}
