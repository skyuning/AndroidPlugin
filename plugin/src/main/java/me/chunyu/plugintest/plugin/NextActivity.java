package me.chunyu.plugintest.plugin;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.TextView;

import com.example.user.User;

/**
 * Created by linyun on 14-6-17.
 */
public class NextActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Plugin NextActivity");
        User user = User.getInstance(getApplicationContext());

        TextView textView = new TextView(this);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        textView.setPadding(40, 40, 40, 40);

        String text = "";
        text += "Class Name:\n" + getClass().getName();
        text += "\n\nUsername: " + user.getUsername();
        textView.setText(text);

        setContentView(textView);
    }
}
