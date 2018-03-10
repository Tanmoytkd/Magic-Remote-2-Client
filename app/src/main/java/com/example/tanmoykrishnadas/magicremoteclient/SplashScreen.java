package com.example.tanmoykrishnadas.magicremoteclient;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.example.tanmoykrishnadas.magicremoteclient.backend.GlideApp;

import static java.lang.Thread.sleep;

public class SplashScreen extends AppCompatActivity {

    FrameLayout frame;
    ImageView splashForeground, splashBackground;
    AutoTransition transition;
    boolean started = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        started = true;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        frame = findViewById(R.id.frame);
        splashForeground = findViewById(R.id.splashForeground);
        splashBackground = findViewById(R.id.splashBackground);

        Glide.with(this).load(R.drawable.splash_foreground_screen).into(splashForeground);
        //GlideApp.with(this).load(R.drawable.splash_background_screen).placeholder(R.drawable.splash_background_placeholder).into(splashBackground);

        transition = new AutoTransition();
        transition.setDuration(1000);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        started = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        started = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        started = true;

        new Thread(() -> {
            try {
                sleep(200);
                runOnUiThread(() -> {
                    TransitionManager.beginDelayedTransition(frame, transition);
                    splashForeground.setVisibility(View.VISIBLE);
                });
                sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (started) {
                startActivity(new Intent(SplashScreen.this, HomeActivity.class));
                finish();
            }
        }).start();
    }
}
