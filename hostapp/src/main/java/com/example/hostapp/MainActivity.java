package com.example.hostapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.dexmaker.stock.ProxyBuilder;

import org.skyun.aplugin.PluginInfo;
import org.skyun.aplugin.PluginManager;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;


public class MainActivity extends Activity {

    private PluginInfo mPluginInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView) findViewById(R.id.hello_world);
//        User user = User.getInstance();
//        user.mUsername = "hostapp username";
//        user.mImage = "hostapp image";
//        tv.setText(user.mUsername + "\n" + user.mImage);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hackInstrumentation();
            }
        });
    }

    private void hackInstrumentation() {
        try {
            // 通过currentActivityThread静态方法获得ActivityThread对象.
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            final Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);

            // 法获得Instrumentation 对象.
            final Field instrumentationField = activityThreadClass.getDeclaredField("mInstrumentation");
            instrumentationField.setAccessible(true);
            final Instrumentation instrumentation = (Instrumentation) instrumentationField.get(activityThread);
            if (instrumentation.getClass().getSimpleName().equals("Instrumentation_Proxy")) {
                if (mPluginInfo == null)
                    mPluginInfo = PluginManager.testInstallPlugin(this, "plugin.apk");

                Class clz = mPluginInfo.mClassLoader.loadClass("com.test.plugin.MainActivity");
                Intent intent = new Intent(this, clz);
                startActivity(intent);
                return;
            }

            AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {

                private ProgressDialog mProgressDialog;

                @Override
                protected void onPreExecute() {
                    mProgressDialog = new ProgressDialog(MainActivity.this);
                    mProgressDialog.setMessage("hacking instrumentation ...");
                    mProgressDialog.show();
                }

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        /*生成Instrumentation代理对象*/
                        Instrumentation minInstrumentation = ProxyBuilder
                                .forClass(Instrumentation.class)
                                .dexCache(getDir("dx", Context.MODE_PRIVATE))
                                .handler(new InstrumentationInvoker(instrumentation))
                                .build();
                        /*替换原来的Instrumentation对象.*/
                        instrumentationField.set(activityThread, minInstrumentation);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void aVoid) {
                    mProgressDialog.dismiss();
                    if (mPluginInfo == null)
                        mPluginInfo = PluginManager.testInstallPlugin(MainActivity.this, "plugin.apk");

                    try {
                        Class clz = mPluginInfo.mClassLoader.loadClass("com.test.plugin.MainActivity");
                        Intent intent = new Intent(MainActivity.this, clz);
                        startActivity(intent);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            };
            asyncTask.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class InstrumentationInvoker implements InvocationHandler {

        private Instrumentation mInstrumentation;

        public InstrumentationInvoker(Instrumentation instrumentation) {
            mInstrumentation = instrumentation;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("newActivity".equals(method.getName()) && args != null && args.length == 3) {
                Activity pluginActivity = newActivity((ClassLoader) args[0], (String) args[1], (Intent) args[2]);
                return pluginActivity;
            } else if ("callActivityOnCreate".equals(method.getName())) {
                Activity activity = (Activity) args[0];
                PluginManager.replaceResources(activity, mPluginInfo.mDexPath);
                return method.invoke(mInstrumentation, args);
            } else {
                return method.invoke(mInstrumentation, args);
            }
        }

        private Activity newActivity(ClassLoader cl, String className, Intent intent) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            return (Activity) mPluginInfo.mClassLoader.loadClass(className).newInstance();
        }
    }
}

