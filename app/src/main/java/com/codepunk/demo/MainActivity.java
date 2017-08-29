package com.codepunk.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.SeekBar;
import android.widget.Toast;

import com.codepunk.demo.support.widget.AppCompatProgressBar;
import com.codepunk.demo.support.widget.AppCompatSeekBar;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private AppCompatProgressBar mProgressBar;
    private AppCompatSeekBar mSeekBar;
    private SeekBar mSeekBar2;
    private Toast mCurrentToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //startActivity(new Intent(this, InteractiveImageViewActivity.class));
        //finish();

        mProgressBar = findViewById(R.id.progress_bar);
        mSeekBar = findViewById(R.id.seek_bar);
        mSeekBar2 = findViewById(R.id.seek_bar_2);

        SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mCurrentToast != null) {
                    mCurrentToast.cancel();
                }

                mCurrentToast = Toast.makeText(MainActivity.this, String.format(Locale.US, "progress=%d", mSeekBar.getSupportProgress()), Toast.LENGTH_SHORT);
                mCurrentToast.show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };

        SeekBar.OnSeekBarChangeListener listener2 = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mCurrentToast != null) {
                    mCurrentToast.cancel();
                }

                mCurrentToast = Toast.makeText(MainActivity.this, String.format(Locale.US, "progress=%d", mSeekBar2.getProgress()), Toast.LENGTH_SHORT);
                mCurrentToast.show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        };
        mSeekBar.setOnSeekBarChangeListener(listener);
        if (mSeekBar2 != null) {
            mSeekBar2.setOnSeekBarChangeListener(listener2);
        }
    }
}
