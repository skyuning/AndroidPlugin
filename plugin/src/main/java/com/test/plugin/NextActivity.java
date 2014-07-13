package com.test.plugin;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

import com.example.user.User;
import com.google.gson.Gson;

/**
 * Created by linyun on 14-6-17.
 */
public class NextActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getClass().getName());

        TextView textView = new TextView(this);
        textView.setText("Myapplication NextActivity");
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
        textView.setGravity(Gravity.CENTER);
        textView.setText(Gson.class.getName());

        User user = User.getInstance();
        textView.setText(user.mUsername + "\n" + user.mImage);

        setContentView(textView);
    }
}
