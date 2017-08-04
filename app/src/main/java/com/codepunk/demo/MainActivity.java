package com.codepunk.demo;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO LATER setContentView(R.layout.activity_main);
        startActivity(new Intent(this, InteractiveImageViewActivity.class));
        finish();
    }
}
