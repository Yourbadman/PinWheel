package com.rocf.pinwheeldemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.leui.circleprogressbardemo.R;
import com.rocf.pinwheel.PinWheelWidget;


/**
 * @author rocf.wong@gmail.com
 */
public class MainActivity extends Activity {

    private PinWheelWidget pinWheel;

    private Button start;
    private Button stop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        setListern();
    }

    private void init() {
        pinWheel = (PinWheelWidget) findViewById(R.id.custom_progress5);
        start = (Button) findViewById(R.id.start);
        stop = (Button) findViewById(R.id.end);
        pinWheel.init();
        pinWheel.setProgress(80);
    }

    private void setListern() {
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                pinWheel.start();
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pinWheel.stop(58.2f);

            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        pinWheel.free();

    }

    @Override
    protected void onPause() {
        super.onPause();
        pinWheel.stop(58.2f);
    }

    @Override
    protected void onStop() {
        super.onStop();
        pinWheel.stop(58.2f);
    }
}
