package me.chunyu.plugintest.plugin;

import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.user.User;
import com.test.plugin.R;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        setTitle("Plugin MainActivity");
        User user = User.getInstance(getApplicationContext());

        TextView textView = (TextView) findViewById(R.id.hello_world);
        String text = "";
        text += "Class Name:\n" + getClass().getName();
        text += "\n\nUsername: " + user.getUsername();
        textView.setText(text);

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NextActivity.class);
                startActivity(intent);
            }
        });
    }
}
