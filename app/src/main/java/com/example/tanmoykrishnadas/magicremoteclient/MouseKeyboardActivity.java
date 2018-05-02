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
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.tanmoykrishnadas.magicremoteclient.backend.BluetoothConnectionService;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.List;

import static com.example.tanmoykrishnadas.magicremoteclient.backend.Constants.DELIM;

public class MouseKeyboardActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = "MouseKeyboardActivity";
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
    public static volatile boolean keyboardOn;

    private EditText typeText;
    private String previousText = "";

    private volatile boolean capsOn = false;
    private volatile boolean boxBusy = false;

    Button a, b, c, d, e, f, g, h, i, j, k, l, m, n, o, p, q, r, s, t, u, v, w, x, y, z, caps;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mouse_keyboard);

        InputMethodManager imeManager = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
        List<InputMethodInfo> list = imeManager.getInputMethodList();
        for (InputMethodInfo el : list) {
            Log.e(TAG, el.getPackageName());
            Log.i(TAG, "ID: " + el.getId());
            // do something to check whatever IME we want.
            // in this case "com.google.android.googlequicksearchbox"
        }
//        final String id = "com.bangla.keyboard/.MyKeyboard";
//        imeManager.setInputMethod(id);


        bluetoothConnection = BluetoothConnectionService.getInstance();

        context = this;

        middleButton = (Button) findViewById(R.id.middleButton);
        leftButton = (Button) findViewById(R.id.leftButton);
        rightButton = (Button) findViewById(R.id.rightButton);

        a = findViewById(R.id.A);
        b = findViewById(R.id.B);
        c = findViewById(R.id.C);
        d = findViewById(R.id.D);
        e = findViewById(R.id.E);
        f = findViewById(R.id.F);
        g = findViewById(R.id.G);
        h = findViewById(R.id.H);
        i = findViewById(R.id.I);
        j = findViewById(R.id.J);
        k = findViewById(R.id.K);
        l = findViewById(R.id.L);
        m = findViewById(R.id.M);
        n = findViewById(R.id.N);
        o = findViewById(R.id.O);
        p = findViewById(R.id.P);
        q = findViewById(R.id.Q);
        r = findViewById(R.id.R);
        s = findViewById(R.id.S);
        t = findViewById(R.id.T);
        u = findViewById(R.id.U);
        v = findViewById(R.id.V);
        w = findViewById(R.id.W);
        x = findViewById(R.id.X);
        y = findViewById(R.id.Y);
        z = findViewById(R.id.Z);
        caps = findViewById(R.id.caps);

        typeText = (EditText) findViewById(R.id.typeText);

        middleButton.setOnClickListener(this);
        leftButton.setOnClickListener(this);
        rightButton.setOnClickListener(this);


        mousePad = (TextView) findViewById(R.id.mousePad);

        keyboardOn = true;
        activityManager.start();

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

                if (pressDownTime.getTimeInMillis() - pressReleaseTime.getTimeInMillis() < 180) {
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

                double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);
                double multiplicationFactor = Math.max(1, distance / 7.00);
//                Log.d("NEW_FEATURE", ""+distance);

                initialX = event.getX();
                initialY = event.getY();
                if (distanceX != 0 || distanceY != 0) {
                    String finalCommand = DELIM + "MOUSE_MOVE" + DELIM + distanceX * multiplicationFactor + DELIM + distanceY * multiplicationFactor + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
                    mouseMoved = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mousePressed) {
                    String finalCommand = DELIM + "LEFT_MOUSE_RELEASE" + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
                    mousePressed = false;
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


    public void clicker(View view) {
        if (view instanceof Button) {
            if (view.getId() == R.id.caps) {
                capsOn = !capsOn;

                if (capsOn) {
                    caps.setText(caps.getText().toString().toUpperCase());
                    a.setText(a.getText().toString().toUpperCase());
                    b.setText(b.getText().toString().toUpperCase());
                    c.setText(c.getText().toString().toUpperCase());
                    d.setText(d.getText().toString().toUpperCase());
                    e.setText(e.getText().toString().toUpperCase());
                    f.setText(f.getText().toString().toUpperCase());
                    g.setText(g.getText().toString().toUpperCase());
                    h.setText(h.getText().toString().toUpperCase());
                    i.setText(i.getText().toString().toUpperCase());
                    j.setText(j.getText().toString().toUpperCase());
                    k.setText(k.getText().toString().toUpperCase());
                    l.setText(l.getText().toString().toUpperCase());
                    m.setText(m.getText().toString().toUpperCase());
                    n.setText(n.getText().toString().toUpperCase());
                    o.setText(o.getText().toString().toUpperCase());
                    p.setText(p.getText().toString().toUpperCase());
                    q.setText(q.getText().toString().toUpperCase());
                    r.setText(r.getText().toString().toUpperCase());
                    s.setText(s.getText().toString().toUpperCase());
                    t.setText(t.getText().toString().toUpperCase());
                    u.setText(u.getText().toString().toUpperCase());
                    v.setText(v.getText().toString().toUpperCase());
                    w.setText(w.getText().toString().toUpperCase());
                    x.setText(x.getText().toString().toUpperCase());
                    y.setText(y.getText().toString().toUpperCase());
                    z.setText(z.getText().toString().toUpperCase());
                } else {
                    caps.setText(caps.getText().toString().toLowerCase());
                    a.setText(a.getText().toString().toLowerCase());
                    b.setText(b.getText().toString().toLowerCase());
                    c.setText(c.getText().toString().toLowerCase());
                    d.setText(d.getText().toString().toLowerCase());
                    e.setText(e.getText().toString().toLowerCase());
                    f.setText(f.getText().toString().toLowerCase());
                    g.setText(g.getText().toString().toLowerCase());
                    h.setText(h.getText().toString().toLowerCase());
                    i.setText(i.getText().toString().toLowerCase());
                    j.setText(j.getText().toString().toLowerCase());
                    k.setText(k.getText().toString().toLowerCase());
                    l.setText(l.getText().toString().toLowerCase());
                    m.setText(m.getText().toString().toLowerCase());
                    n.setText(n.getText().toString().toLowerCase());
                    o.setText(o.getText().toString().toLowerCase());
                    p.setText(p.getText().toString().toLowerCase());
                    q.setText(q.getText().toString().toLowerCase());
                    r.setText(r.getText().toString().toLowerCase());
                    s.setText(s.getText().toString().toLowerCase());
                    t.setText(t.getText().toString().toLowerCase());
                    u.setText(u.getText().toString().toLowerCase());
                    v.setText(v.getText().toString().toLowerCase());
                    w.setText(w.getText().toString().toLowerCase());
                    x.setText(x.getText().toString().toLowerCase());
                    y.setText(y.getText().toString().toLowerCase());
                    z.setText(z.getText().toString().toLowerCase());
                }

            } else if (view.getId() == R.id.button_123) {
                finish();
                startActivity(new Intent(MouseKeyboardActivity.this, MouseKeyboardActivity2.class));
            } else if (view.getId() == R.id.button_enter) {
                typeText.append("\n");
            } else if (view.getId() == R.id.symbol_comma) {
                typeText.append(",");
            } else if (view.getId() == R.id.symbol_dot) {
                typeText.append(".");
            } else if (view.getId() == R.id.symbol_SPACE) {
                typeText.append(" ");
            } else if (view.getId() == R.id.backspace) {
                int length = typeText.getText().length();
                if (length > 0) {
                    typeText.getText().delete(length - 1, length);
                } else {
                    String finalCommand = DELIM + "TYPE_CHARACTER" + DELIM + "\b" + DELIM;
                    bluetoothConnection.write(finalCommand.getBytes());
                }
            } else {
                typeText.append(((Button) view).getText().toString());
            }
        }
    }
}
