package com.codepunk.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.codepunk.demo.interactiveimageview2.ImageViewInteractinatorActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, ImageViewInteractinatorActivity.class));
        finish();
    }
}
