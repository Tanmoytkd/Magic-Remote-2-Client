package com.example.tanmoykrishnadas.magicremoteclient;

import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;

public class HomeActivity extends AppCompatActivity {
    public static String TAG = "HomeActivity";
    RelativeLayout root;
    Animation zoomOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        root = findViewById(R.id.root);

        findViewById(R.id.btnConnect).setOnClickListener(e->{
            Log.d(TAG, "anim start");
            zoomOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.zoom_out);
            e.startAnimation(zoomOut);
            Log.d(TAG, "Anim end");
        });

    }

    public void showTransition(View view) {

    }
}
