package com.example.tanmoykrishnadas.magicremoteclient;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;

import static com.example.tanmoykrishnadas.magicremoteclient.Constants.DELIM;

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
    private volatile boolean keyboardOn;

    Thread activityManager = new Thread() {
        @Override
        public void run() {
            while(keyboardOn) {
                try {
                    if(!bluetoothConnection.getBluetoothStatus().equals("connected")) finish();
                    sleep(80);
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

        bluetoothConnection = BluetoothConnectionService.getInstance(MouseActivity.this);

        context = this;

        middleButton = (Button) findViewById(R.id.middleButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);


        middleButton.setOnClickListener(this);
        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);


        mousePad = (TextView) findViewById(R.id.mousePad);

//        mousePad.setOnLongClickListener(new View.OnLongClickListener() {
//            @Override
//            public boolean onLongClick(View view) {
//                String finalCommand = DELIM + "RIGHT_CLICK" + DELIM;
//                bluetoothConnection.write(finalCommand.getBytes());
//                return false;
//            }
//        });



        mousePad.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int fingers = event.getPointerCount();

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

                if(pressDownTime.getTimeInMillis()-pressReleaseTime.getTimeInMillis()<180) {
                    String finalCommand = DELIM + "LEFT_MOUSE_PRESS" + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
                    mousePressed = true;
                }

                initialX = event.getX();
                initialY = event.getY();
                mouseMoved = false;
                break;
            case MotionEvent.ACTION_MOVE:

                distanceX = event.getX() - initialX;
                distanceY = event.getY() - initialY;

                double distance = Math.sqrt(distanceX*distanceX + distanceY*distanceY);
                double multiplicationFactor = Math.max(1, distance/7.00);
//                Log.d("NEW_FEATURE", ""+distance);

                initialX = event.getX();
                initialY = event.getY();
                if (distanceX != 0 || distanceY != 0) {
                    String finalCommand = DELIM + "MOUSE_MOVE" + DELIM + distanceX*multiplicationFactor + DELIM + distanceY*multiplicationFactor + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
                    mouseMoved = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if(mousePressed) {
                    String finalCommand = DELIM + "LEFT_MOUSE_RELEASE" + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
                    mousePressed=false;
                }

                pressReleaseTime = Calendar.getInstance();
                Long timeDifference = pressReleaseTime.getTimeInMillis() - pressDownTime.getTimeInMillis();

                if (!mouseMoved && timeDifference < 250) {
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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_connect) {
////            ConnectPhoneTask connectPhoneTask = new ConnectPhoneTask();
////            connectPhoneTask.execute(Constants.SERVER_IP); //try to connect to server in another thread
//            Toast.makeText(this, "Connection Request", Toast.LENGTH_SHORT).show();
//            //Log.d(TAG, "Connection Request");
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
        return true;
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
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardOn = true;
        activityManager.start();
        if(bluetoothConnection!=null) bluetoothConnection.setContext(MouseActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        keyboardOn = false;
    }
}