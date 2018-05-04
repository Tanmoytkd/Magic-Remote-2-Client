package com.example.tanmoykrishnadas.magicremoteclient;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.tanmoykrishnadas.magicremoteclient.backend.BluetoothConnectionService;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;

import static com.example.tanmoykrishnadas.magicremoteclient.backend.Constants.DELIM;
import static java.lang.Thread.sleep;

public class MouseActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MouseActivity";
    Context context;
    Button leftButton, middleButton, rightButton;
    TextView mousePad;
    private boolean isConnected = false;
    private boolean mouseMoved = false;
    private Socket socket;
    private PrintWriter out;
    private float initialX = 0;
    private float initialY = 0;
    private float distanceX = 0;
    private float distanceY = 0;
    private Calendar pressDownTime=Calendar.getInstance(), pressReleaseTime=Calendar.getInstance();
    BluetoothConnectionService bluetoothConnection;
    private volatile boolean mouseOn;
    int fingers;
    float currentX, currentY;

    Thread activityManager = new Thread() {
        @Override
        public void run() {
            while(mouseOn) {
                try {
                    String status = bluetoothConnection.getBluetoothStatus();
                    if(!status.equals("connected")) {
                        Log.e(TAG, "Disconnected from host");
                        finish();
                    }
                    sleep(80);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    Thread mouseManager = new Thread() {
        @Override
        public void run() {
            while(mouseOn) {
                try {
                    String status = bluetoothConnection.getBluetoothStatus();
                    if(!status.equals("connected")) {
                        Log.e(TAG, "Disconnected from host");
                        finish();
                    }

                    if(fingers==1) {
                        distanceX = currentX - initialX;
                        distanceY = currentY - initialY;

                        double distance = Math.sqrt(distanceX*distanceX + distanceY*distanceY);
//                        runOnUiThread(()->{
//                            mousePad.setText(String.valueOf(distance));
//                        });
                        double multiplicationFactor = Math.min(Math.max(1, distance/11.00), 3.5);
//                Log.d("NEW_FEATURE", ""+distance);

                        initialX = currentX;
                        initialY = currentY;
                        if (distanceX != 0 || distanceY != 0) {
                            String finalCommand = DELIM + "MOUSE_MOVE" + DELIM + distanceX*multiplicationFactor + DELIM + distanceY*multiplicationFactor + DELIM;
                            bluetoothConnection.write(finalCommand.getBytes());
                            mouseMoved = true;
                        }
                    }



                    sleep(40);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mouse);

        bluetoothConnection = BluetoothConnectionService.getInstance();

        context = this;

        middleButton = (Button) findViewById(R.id.middleButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);


        middleButton.setOnClickListener(this);
        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);


        mousePad = (TextView) findViewById(R.id.mousePad);

        mouseOn = true;
        activityManager.start();
        mouseManager.start();

        mousePad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                fingers = event.getPointerCount();

                if (fingers == 1) handleSingleTouch(event);
                else if (fingers == 2) handleDoubleTouch(event);

                return true;
            }
        });

    }

    int GLOBAL_TOUCH_POSITION_Y = 0;
    int GLOBAL_TOUCH_CURRENT_POSITION_Y = 0;
    int prevDiff = 0;
    volatile boolean mousePressed = false;

    private void handleSingleTouch(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                pressDownTime = Calendar.getInstance();

                if((pressDownTime.getTimeInMillis()-pressReleaseTime.getTimeInMillis())<120) {
                    String finalCommand = DELIM + "LEFT_MOUSE_PRESS" + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
                    mousePressed = true;
                }

                initialX = event.getX();
                initialY = event.getY();
                currentX = initialX;
                currentY = initialY;
                mouseMoved = false;
                break;
            case MotionEvent.ACTION_MOVE:

                currentX = event.getX();
                currentY = event.getY();

                break;
            case MotionEvent.ACTION_UP:
                if(mousePressed) {
                    String finalCommand = DELIM + "LEFT_MOUSE_RELEASE" + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
                    mousePressed=false;
                }

                pressReleaseTime = Calendar.getInstance();
                Long timeDifference = pressReleaseTime.getTimeInMillis() - pressDownTime.getTimeInMillis();

                if (!mouseMoved && timeDifference < 120) {
                    String finalCommand = DELIM + "LEFT_CLICK" + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
//                            ////Log.d(TAG, "LEFT_CLICK");
                }
        }

    }

    private void handleDoubleTouch(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                GLOBAL_TOUCH_POSITION_Y = (int) event.getY(1);
                break;
            case MotionEvent.ACTION_UP:
                GLOBAL_TOUCH_CURRENT_POSITION_Y = 0;
                break;
            case MotionEvent.ACTION_MOVE:
                GLOBAL_TOUCH_CURRENT_POSITION_Y = (int) event.getY(1);
                int diff = GLOBAL_TOUCH_POSITION_Y - GLOBAL_TOUCH_CURRENT_POSITION_Y;
//                //Log.d(TAG, "scroll: " + (diff-prevDiff));

                String finalCommand = DELIM + "MOUSE_WHEEL" + DELIM + (prevDiff - diff) + DELIM;
                bluetoothConnection.write(finalCommand.getBytes());

                prevDiff = diff;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                GLOBAL_TOUCH_POSITION_Y = (int) event.getY(1);
                break;
            default:
        }
    }

    //OnClick method is called when any of the buttons are pressed
    @Override
    public void onClick(View v) {
        String finalCommand = "";
        switch (v.getId()) {
            case R.id.middleButton:
                finalCommand = DELIM + "MIDDLE_CLICK" + DELIM;
                break;
            case R.id.leftButton:
                finalCommand = DELIM + "LEFT_CLICK" + DELIM;
                break;
            case R.id.rightButton:
                finalCommand = DELIM + "RIGHT_CLICK" + DELIM;
                break;
        }
        bluetoothConnection.write(finalCommand.getBytes());
    }



    @Override
    protected void onResume() {
        super.onResume();
        mouseOn = true;
//        if(bluetoothConnection!=null) bluetoothConnection.setContext(MouseActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mouseOn = false;
    }
}