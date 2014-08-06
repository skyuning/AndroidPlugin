package me.chunyu.plugintest.hostapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.hostapp.R;
import com.example.user.User;

import me.chunyu.aplugin.PluginManager;


public class MainActivity extends Activity {

    private static String sPluginPackageName = "me.chunyu.plugintest.plugin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle("Host MainActivity");
        setContentView(R.layout.activity_main);
        TextView tv = (TextView) findViewById(R.id.hello_world);

        String text = "Class Name:\n" + getClass().getName();
        text += "\n\nUsername: " + User.getInstance(this).getUsername();
        tv.setText(text);

        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginManager.hackInstrumentation(MainActivity.this);

                PluginManager.testInstallPlugin(MainActivity.this, sPluginPackageName);

                try {
                    Class clz = PluginManager.getPluginInfoMap().get(sPluginPackageName)
                            .mClassLoader.loadClass(sPluginPackageName + ".MainActivity");
                    Intent intent = new Intent(MainActivity.this, clz);
                    startActivity(intent);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}

