package me.chunyu.plugintest.hostapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.hostapp.R;
import com.example.user.User;

import me.chunyu.aplugin.PluginManager;
import me.chunyu.aplugin.PluginService;


public class MainActivity extends Activity {

    private static String sPluginPackageName = "com.test.plugin";

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
                    Intent realIntent = new Intent(MainActivity.this, clz);
                    Intent intent = new Intent(MainActivity.this, PluginService.class);
                    intent.putExtra(PluginService.EXTRA_PLUGIN_PACKAGE, sPluginPackageName);
                    intent.putExtra(PluginService.EXTRA_REAL_INTENT, realIntent);
                    startService(intent);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}

