package com.example.tanmoykrishnadas.magicremoteclient;

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

import com.example.tanmoykrishnadas.magicremoteclient.backend.BluetoothConnectionService;

import static com.example.tanmoykrishnadas.magicremoteclient.backend.Constants.DELIM;

public class KeyboardActivity extends AppCompatActivity implements View.OnTouchListener, View.OnClickListener, TextWatcher {
    public static final String TAG = "KeyboardActivity";
    private EditText typeHereEditText;
    private Button ctrlButton, altButton, shiftButton, enterButton, tabButton, escButton, printScrButton, backspaceButton;
    private Button deleteButton, clearTextButton;
    private Button nButton, tButton, wButton, rButton, fButton, zButton;
    private Button cButton, xButton, vButton, aButton, oButton, sButton;
    private Button ctrlAltTButton, ctrlShiftZButton, altF4Button;
    private String previousText = "";
    BluetoothConnectionService bluetoothConnection;
    private volatile boolean keyboardOn;

    Thread activityManager = new Thread() {
        @Override
        public void run() {
            while(keyboardOn) {
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

    @Override
    protected void onResume() {
        super.onResume();
//        if(bluetoothConnection!=null) bluetoothConnection.setContext(KeyboardActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        keyboardOn = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keyboard);

        bluetoothConnection = BluetoothConnectionService.getInstance();

        typeHereEditText = (EditText) findViewById(R.id.typeHereEditText);
        ctrlButton = (Button) findViewById(R.id.ctrlButton);
        altButton = (Button) findViewById(R.id.altButton);
        shiftButton = (Button) findViewById(R.id.shiftButton);
        enterButton = (Button) findViewById(R.id.enterButton);
        tabButton = (Button) findViewById(R.id.tabButton);
        escButton = (Button) findViewById(R.id.escButton);
        printScrButton = (Button) findViewById(R.id.printScrButton);
        backspaceButton = (Button) findViewById(R.id.backspaceButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);
        clearTextButton = (Button) findViewById(R.id.clearTextButton);
        nButton = (Button) findViewById(R.id.nButton);
        tButton = (Button) findViewById(R.id.tButton);
        wButton = (Button) findViewById(R.id.wButton);
        rButton = (Button) findViewById(R.id.rButton);
        fButton = (Button) findViewById(R.id.fButton);
        zButton = (Button) findViewById(R.id.zButton);
        cButton = (Button) findViewById(R.id.cButton);
        xButton = (Button) findViewById(R.id.xButton);
        vButton = (Button) findViewById(R.id.vButton);
        aButton = (Button) findViewById(R.id.aButton);
        oButton = (Button) findViewById(R.id.oButton);
        sButton = (Button) findViewById(R.id.sButton);
        ctrlAltTButton = (Button) findViewById(R.id.ctrlAltTButton);
        ctrlShiftZButton = (Button) findViewById(R.id.ctrlShiftZButton);
        altF4Button = (Button) findViewById(R.id.altF4Button);

        ctrlButton.setOnTouchListener(this);
        altButton.setOnTouchListener(this);
        shiftButton.setOnTouchListener(this);
        backspaceButton.setOnClickListener(this);
        enterButton.setOnClickListener(this);
        tabButton.setOnClickListener(this);
        escButton.setOnClickListener(this);
        printScrButton.setOnClickListener(this);
        deleteButton.setOnClickListener(this);
        clearTextButton.setOnClickListener(this);
        nButton.setOnClickListener(this);
        tButton.setOnClickListener(this);
        wButton.setOnClickListener(this);
        rButton.setOnClickListener(this);
        fButton.setOnClickListener(this);
        zButton.setOnClickListener(this);
        cButton.setOnClickListener(this);
        xButton.setOnClickListener(this);
        vButton.setOnClickListener(this);
        aButton.setOnClickListener(this);
        oButton.setOnClickListener(this);
        sButton.setOnClickListener(this);
        ctrlAltTButton.setOnClickListener(this);
        ctrlShiftZButton.setOnClickListener(this);
        altF4Button.setOnClickListener(this);
        typeHereEditText.addTextChangedListener(this);

        keyboardOn = true;
        activityManager.start();
    }



    @Override
    public boolean onTouch(View v, MotionEvent event) {
        String action = "KEY_PRESS";
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            action = "KEY_PRESS";
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            action = "KEY_RELEASE";
        }
        int keyCode = 17;//dummy initialization
        switch (v.getId()) {
            case R.id.ctrlButton:
                keyCode = 17;
                break;
            case R.id.altButton:
                keyCode = 18;
                break;
            case R.id.shiftButton:
                keyCode = 16;
                break;
        }

        String finalComand = DELIM + action + DELIM + keyCode + DELIM;
        bluetoothConnection.write(finalComand.getBytes());
        return false;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.clearTextButton) {
            typeHereEditText.setText("");
        } else if (id == R.id.ctrlAltTButton || id == R.id.ctrlShiftZButton || id == R.id.altF4Button) {
            String message = "CTRL_SHIFT_Z";
            switch (id) {
                case R.id.ctrlAltTButton:
                    message = "CTRL_ALT_T";
                    break;
                case R.id.ctrlShiftZButton:
                    message = "CTRL_SHIFT_Z";
                    break;
                case R.id.altF4Button:
                    message = "ALT_F4";
                    break;
            }
            String finalCommand = DELIM + message + DELIM;
            bluetoothConnection.write(finalCommand.getBytes());
        } else {
            int keyCode = 17;//dummy initialization
            String action = "TYPE_KEY";
            switch (id) {
                case R.id.enterButton:
                    keyCode = 10;
                    break;
                case R.id.tabButton:
                    keyCode = 9;
                    break;
                case R.id.escButton:
                    keyCode = 27;
                    break;
                case R.id.printScrButton:
                    keyCode = 154;
                    break;
                case R.id.deleteButton:
                    keyCode = 127;
                    break;
                case R.id.nButton:
                    keyCode = 78;
                    break;
                case R.id.tButton:
                    keyCode = 84;
                    break;
                case R.id.wButton:
                    keyCode = 87;
                    break;
                case R.id.rButton:
                    keyCode = 82;
                    break;
                case R.id.fButton:
                    keyCode = 70;
                    break;
                case R.id.zButton:
                    keyCode = 90;
                    break;
                case R.id.cButton:
                    keyCode = 67;
                    break;
                case R.id.xButton:
                    keyCode = 88;
                    break;
                case R.id.vButton:
                    keyCode = 86;
                    break;
                case R.id.aButton:
                    keyCode = 65;
                    break;
                case R.id.oButton:
                    keyCode = 79;
                    break;
                case R.id.sButton:
                    keyCode = 83;
                    break;
                case R.id.backspaceButton:
                    keyCode = 8;
                    break;
            }
            String finalCommand = DELIM + action + DELIM + keyCode + DELIM;
            bluetoothConnection.write(finalCommand.getBytes());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
    }
    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String bck = getBackspaces(s, previousText);
        if(!bck.equals("")) {
            String finalCommand = DELIM + "TYPE_CHARACTER" + DELIM + bck + DELIM;
            bluetoothConnection.write(finalCommand.getBytes());
        }

        String ch = getExtraText(s, previousText);
        if (!ch.equals("")) {
            String finalCommand = DELIM + "TYPE_CHARACTER" + DELIM + ch + DELIM;
            bluetoothConnection.write(finalCommand.getBytes());
        }

        previousText = s.toString();
    }
    @Override
    public void afterTextChanged(Editable s) {
    }

    @NonNull
    private String getBackspaces(CharSequence currentText, CharSequence previousText) {
        int len = Math.min(currentText.length(), previousText.length());
        int pos=0;
        for(pos=0; pos<len; pos++) {
            if(currentText.charAt(pos)!=previousText.charAt(pos)) break;
        }
        int bck = previousText.length() - pos;

        StringBuilder x = new StringBuilder();
        for(int i=0; i<bck; i++) {
            x.append('\b');
        }
        return x.toString();
    }

    private String getExtraText (CharSequence currentText, CharSequence previousText) {
        String ch = "";
        int minlen = Math.min(currentText.length(), previousText.length());
        int pos = 0;

        for(pos=0; pos<minlen; pos++) {
            if(currentText.charAt(pos)!=previousText.charAt(pos)) break;
        }
        ch = currentText.toString().substring(pos);

        return ch;
    }
}


//key code from google
/** Refer to TKD in case of problem
 * ctrl: 17
 * alt: 18
 * shift: 16
 * enter: 10
 * tab: 9
 * esc: 27
 * prntScr: 154
 * backspace: 524
 * delete: 127
 * backspace: 8
 */
/**
 * N: 78
 * T: 84
 * W: 87
 * R: 82
 * F: 70
 * Z: 90
 * C: 67
 * X: 88
 * V: 86
 * A: 65
 * O: 79
 * S: 83

 */
