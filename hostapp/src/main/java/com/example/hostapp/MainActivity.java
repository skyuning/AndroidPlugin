package com.example.hostapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.user.User;

import org.skyun.aplugin.PluginInfo;
import org.skyun.aplugin.PluginManager;


public class MainActivity extends Activity {

    private static String sPluginPackageName = "com.test.plugin";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(getClass().getName());

        setContentView(R.layout.activity_main);
        TextView tv = (TextView) findViewById(R.id.hello_world);
        tv.setText("Hello world" + User.getInstance().mUsername);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PluginManager.hackInstrumentation(MainActivity.this);

                if (PluginManager.getPluginInfoMap().get(sPluginPackageName) == null) {
                    PluginInfo pluginInfo = PluginManager.testInstallPlugin(MainActivity.this, sPluginPackageName + ".apk");
                    PluginManager.getPluginInfoMap().put(sPluginPackageName, pluginInfo);
                }

                try {
                    Class clz = PluginManager.getPluginInfoMap().get(sPluginPackageName).mClassLoader.loadClass("com.test.plugin.MainActivity");
                    Intent intent = new Intent(MainActivity.this, clz);
                    startActivity(intent);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

