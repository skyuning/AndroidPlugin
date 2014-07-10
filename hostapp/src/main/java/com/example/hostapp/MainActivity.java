package com.example.hostapp;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.dexmaker.stock.ProxyBuilder;

import org.skyun.aplugin.PluginInfo;
import org.skyun.aplugin.PluginManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;


public class MainActivity extends Activity {

    private static String sPluginPackageName = "com.test.plugin";

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
                InstrumentationHacker.hackInstrumentation(MainActivity.this);

                if (InstrumentationHacker.getPluginInfoMap().get(sPluginPackageName) == null) {
                    PluginInfo pluginInfo = PluginManager.testInstallPlugin(MainActivity.this, sPluginPackageName + ".apk");
                    InstrumentationHacker.getPluginInfoMap().put(sPluginPackageName, pluginInfo);
                }

                try {
                    Class clz = InstrumentationHacker.getPluginInfoMap().get(sPluginPackageName).mClassLoader.loadClass("com.test.plugin.MainActivity");
                    Intent intent = new Intent(MainActivity.this, clz);
                    MainActivity.this.startActivity(intent);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}

class InstrumentationHacker {

    private static HashMap<String, PluginInfo> sPluginInfoMap;

    public static HashMap<String, PluginInfo> getPluginInfoMap() {
        if (sPluginInfoMap == null)
            sPluginInfoMap = new HashMap<String, PluginInfo>();
        return sPluginInfoMap;
    }

    private static PluginInfo getPluginInfo(String activityClassName) {
        for (String pluginPackageName : getPluginInfoMap().keySet()) {
            if (activityClassName.startsWith(pluginPackageName))
                return getPluginInfoMap().get(pluginPackageName);
        }
        return null;
    }

    public static void hackInstrumentation(final Activity activity) {
        ProgressDialog mProgressDialog = new ProgressDialog(activity);
        mProgressDialog.setMessage("hacking instrumentation ...");

        try {
            // 通过currentActivityThread静态方法获得ActivityThread对象.
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            final Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);

            // 法获得Instrumentation 对象.
            final Field instrumentationField = activityThreadClass.getDeclaredField("mInstrumentation");
            instrumentationField.setAccessible(true);
            final Instrumentation instrumentation = (Instrumentation) instrumentationField.get(activityThread);
            if (instrumentation.getClass().getSimpleName().equals("Instrumentation")) {
                mProgressDialog.show();
                /*生成Instrumentation代理对象*/
                Instrumentation minInstrumentation = ProxyBuilder
                        .forClass(Instrumentation.class)
                        .dexCache(activity.getDir("dx", Context.MODE_PRIVATE))
                        .handler(new InstrumentationInvoker(instrumentation))
                        .build();
                        /*替换原来的Instrumentation对象.*/
                instrumentationField.set(activityThread, minInstrumentation);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mProgressDialog.dismiss();
        }
    }

    private static class InstrumentationInvoker implements InvocationHandler {

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
                PluginInfo pluginInfo = getPluginInfo(activity.getClass().getName());
                if (pluginInfo != null)
                    PluginManager.replaceResources(activity, pluginInfo.mDexPath);
                return method.invoke(mInstrumentation, args);
            } else {
                return method.invoke(mInstrumentation, args);
            }
        }

        private Activity newActivity(ClassLoader cl, String className, Intent intent) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
            PluginInfo pluginInfo = getPluginInfo(className);
            if (pluginInfo != null)
                return (Activity) pluginInfo.mClassLoader.loadClass(className).newInstance();
            else
                return (Activity) cl.loadClass(className).newInstance();
        }
    }
}

