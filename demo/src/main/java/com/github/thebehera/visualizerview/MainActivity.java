package com.github.thebehera.visualizerview;

import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Process;
import android.view.View;

import com.github.thebehera.visualizerview.databinding.ActivityMainBinding;
import com.github.thebehera.visualizerviewlib.VisualizerView;

public class MainActivity extends Activity {
    ActivityMainBinding binding;
    private boolean reverse = false;
    Handler backgroundHandler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//            binding.visualView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
//        }
        HandlerThread ht = new HandlerThread("audio processing", Process.THREAD_PRIORITY_BACKGROUND);
        ht.start();
        backgroundHandler = new Handler(ht.getLooper());
        binding.visualView.post(waveRunnable);

//        binding.visualView.post(new Runnable() {
//            @Override
//            public void run() {
//                backgroundHandler.post(waveRunnable);
//            }
//        });
    }

    private Runnable waveRunnable = new Runnable() {
        @Override public void run() {
            if (isFinishing()) {
                return;
            }

            VisualizerView view = binding.visualView;
            float currentLevel = view.getLevel();
            if (currentLevel >= 1.0f) {
                reverse = true;
            } else if (currentLevel <= 0.0f) {
                reverse = false;
            }

            float newLevel;
            if (reverse) {
                newLevel = currentLevel - .01f;
            } else {
                newLevel = currentLevel + .01f;
            }
            binding.visualView.setLevel(newLevel);
//            backgroundHandler.postDelayed(waveRunnable, 60);
            binding.visualView.postDelayed(waveRunnable, 60);
        }
    };
}
