package com.rayworks.customtransition;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;

import com.rayworks.customtransition.widget.StateTransitionButton;

public class MainActivity extends FragmentActivity {

    private StateTransitionButton stb;
    private Handler handler = new Handler();
    private Button loadBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loadBtn = findViewById(R.id.load_btn);
        stb = findViewById(R.id.button);

        loadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadBtn.setText("");

                showTransition();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void showTransition() {
        stb.setVisibility(View.VISIBLE);

        stb.bringToFront();

        stb.showLoading();
        doBackgroundTask();
    }

    private void doBackgroundTask() {
        // simulate the time consuming background task
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (stb != null) {
                    stb.showSuccessState();
                }
            }
        }, 3000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
    }
}
