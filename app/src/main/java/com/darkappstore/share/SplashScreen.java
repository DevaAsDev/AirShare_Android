package com.darkappstore.share;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;

public class SplashScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);


        new Thread() {
            @Override
            public void run() {
                try {
                    sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                    Intent i = new Intent(SplashScreen.this, MainActivity.class);
                    startActivity(i);
                    finish();

            }
        }.start();

    }
}