package com.example.tanmoykrishnadas.magicremoteclient;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.tanmoykrishnadas.magicremoteclient.backend.BluetoothConnectionService;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;

import static com.example.tanmoykrishnadas.magicremoteclient.backend.Constants.DELIM;

public class MouseKeyboardActivity2 extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MouseKeyboardActivity2";
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
    private Calendar pressDownTime = Calendar.getInstance(), pressReleaseTime = Calendar.getInstance();
    BluetoothConnectionService bluetoothConnection;
    private volatile boolean keyboardOn;
    private volatile boolean boxBusy = false;
    int fingers;
    float currentX, currentY;

    Thread activityManager = new Thread() {
        boolean written = false;

        @Override
        public void run() {
            while (keyboardOn) {
                try {
                    String status = bluetoothConnection.getBluetoothStatus();
                    if (!status.equals("connected")) {
                        Log.e(TAG, "Disconnected from host");
                        finish();
                    }

                    if (!boxBusy) {
                        String s = typeText.getText().toString();
                        String bck = getBackspaces(s, previousText);
                        if (!bck.equals("")) {
                            String finalCommand = DELIM + "TYPE_CHARACTER" + DELIM + bck + DELIM;
                            bluetoothConnection.write(finalCommand.getBytes());
                        }
                        String ch = getExtraText(s, previousText);
                        if (!ch.equals("")) {
                            written = false;
                            String finalCommand = DELIM + "TYPE_CHARACTER" + DELIM + ch + DELIM;
                            bluetoothConnection.write(finalCommand.getBytes());
                        }
                        previousText = s;
                    }

                    sleep(150);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    Thread mouseManager = new Thread() {
        @Override
        public void run() {
            while(keyboardOn) {
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

    private EditText typeText;
    private String previousText = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mouse_keyboard2);

        bluetoothConnection = BluetoothConnectionService.getInstance();

        context = this;

        middleButton = (Button) findViewById(R.id.middleButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);

        typeText = (EditText) findViewById(R.id.typeText);

        middleButton.setOnClickListener(this);
        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);


        mousePad = (TextView) findViewById(R.id.mousePad);

        keyboardOn = true;
        activityManager.start();
        mouseManager.start();

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
        keyboardOn = true;
//        if(bluetoothConnection!=null) bluetoothConnection.setContext(MouseActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        keyboardOn = false;
    }

    @NonNull
    private String getBackspaces(CharSequence currentText, CharSequence previousText) {
        int len = Math.min(currentText.length(), previousText.length());
        int pos = 0;
        for (pos = 0; pos < len; pos++) {
            if (currentText.charAt(pos) != previousText.charAt(pos)) break;
        }
        int bck = previousText.length() - pos;

        StringBuilder x = new StringBuilder();
        for (int i = 0; i < bck; i++) {
            x.append('\b');
        }
        return x.toString();
    }

    private String getExtraText(CharSequence currentText, CharSequence previousText) {
        String ch = "";
        int minlen = Math.min(currentText.length(), previousText.length());
        int pos = 0;

        for (pos = 0; pos < minlen; pos++) {
            if (currentText.charAt(pos) != previousText.charAt(pos)) break;
        }
        ch = currentText.toString().substring(pos);

        return ch;
    }

    public void clicker(View v) {
        if(v.getId()==R.id.num0) {
            typeText.append("0");
        }
        if(v.getId()==R.id.num1) {
            typeText.append("1");
        }
        if(v.getId()==R.id.num2) {
            typeText.append("2");
        }
        if(v.getId()==R.id.num3) {
            typeText.append("3");
        }
        if(v.getId()==R.id.num4) {
            typeText.append("4");
        }
        if(v.getId()==R.id.num5) {
            typeText.append("5");
        }
        if(v.getId()==R.id.num6) {
            typeText.append("6");
        }
        if(v.getId()==R.id.num7) {
            typeText.append("7");
        }
        if(v.getId()==R.id.num8) {
            typeText.append("8");
        }
        if(v.getId()==R.id.num9) {
            typeText.append("9");
        }
        if(v.getId()==R.id.symbol_at) {
            typeText.append("@");
        }
        if(v.getId()==R.id.symbol_hash) {
            typeText.append("#");
        }
        if(v.getId()==R.id.symbol_dollar) {
            typeText.append("$");
        }
        if(v.getId()==R.id.symbol_percent) {
            typeText.append("%");
        }
        if(v.getId()==R.id.symbol_ampersand) {
            typeText.append("&");
        }
        if(v.getId()==R.id.symbol_asterisk) {
            typeText.append("*");
        }
        if(v.getId()==R.id.symbol_minus) {
            typeText.append("-");
        }
        if(v.getId()==R.id.symbol_add) {
            typeText.append("+");
        }
        if(v.getId()==R.id.symbol_open_brace) {
            typeText.append("(");
        }
        if(v.getId()==R.id.symbol_closed_brace) {
            typeText.append(")");
        }
        if(v.getId()==R.id.symbol_underscore) {
            typeText.append("_");
        }
        if(v.getId()==R.id.symbol_exclamation) {
            typeText.append("!");
        }
        if(v.getId()==R.id.symbol_double_quote) {
            typeText.append("\"");
        }
        if(v.getId()==R.id.symbol_single_quote) {
            typeText.append("'");
        }
        if(v.getId()==R.id.symbol_colon) {
            typeText.append(":");
        }
        if(v.getId()==R.id.symbol_semicolon) {
            typeText.append(";");
        }
        if(v.getId()==R.id.symbol_slash) {
            typeText.append("/");
        }
        if(v.getId()==R.id.symbol_question) {
            typeText.append("?");
        }
        if(v.getId()==R.id.backspace) {
            int length = typeText.getText().length();
            if (length > 0) {
                typeText.getText().delete(length - 1, length);
            } else {
                String finalCommand = DELIM + "TYPE_CHARACTER" + DELIM + "\b" + DELIM;
                bluetoothConnection.write(finalCommand.getBytes());
            }
        }
        if(v.getId()==R.id.button_symbol_abc){
            finish();
            startActivity(new Intent(MouseKeyboardActivity2.this, MouseKeyboardActivity.class));
        }
        if(v.getId()==R.id.symbol_comma) {
            typeText.append(",");
        }
        if(v.getId()==R.id.symbol_dot) {
            typeText.append(".");
        }
        if(v.getId()==R.id.symbol_SPACE) {
            typeText.append(" ");
        }
        if(v.getId()==R.id.button_enter) {
            String str = typeText.getText().toString() + "\n";
            typeText.setText(str);
        }
    }
}
