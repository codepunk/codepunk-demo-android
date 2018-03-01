package com.codepunk.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.codepunk.demo.interactiveimageview.version7.InteractiveImageViewActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        startActivity(new Intent(this, InteractiveImageViewActivity.class));
        finish();
    }
}
