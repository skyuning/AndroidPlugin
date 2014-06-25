package com.test.plugin;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.example.user.User;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView;

        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.hello_world);

//        textView = new TextView(this);
//        setContentView(textView);
//        textView.setText("Myapplication MainActivity");
//        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
//        textView.setGravity(Gravity.CENTER);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NextActivity.class);
                startActivity(intent);
            }
        });
    }
}
