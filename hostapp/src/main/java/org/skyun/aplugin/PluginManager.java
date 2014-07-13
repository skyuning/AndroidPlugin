package org.skyun.aplugin;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;

import com.google.dexmaker.stock.ProxyBuilder;

import org.skyun.aplugin.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

import dalvik.system.DexClassLoader;

/**
 * Created by linyun on 14-6-23.
 */
public class PluginManager {

    public static final String PLUGIN_SUFFIX = ".apk";

    private static HashMap<String, PluginInfo> sPluginInfoMap;

    public static HashMap<String, PluginInfo> getPluginInfoMap() {
        if (sPluginInfoMap == null)
            sPluginInfoMap = new HashMap<String, PluginInfo>();
        return sPluginInfoMap;
    }

    public static PluginInfo testInstallPlugin(Context context, String packageName) {
        String filename = packageName + PLUGIN_SUFFIX;
        try {
            File externalFile = new File(getExternalPluginDir(context), filename);
            FileUtils.copyToFile(context.getAssets().open(filename), externalFile);
            return installPlugin(context, packageName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PluginInfo installPlugin(Context context, String packageName) {
        String filename = packageName + PLUGIN_SUFFIX;
        PluginInfo pluginInfo = getPluginInfoMap().get(filename);
        if (pluginInfo != null)
            return pluginInfo;

        File externalFile = new File(getExternalPluginDir(context), filename);
        if (externalFile.exists())
            pluginInfo = installPluginFromExternal(context, filename);
        else
            pluginInfo = installPluginFromAssets(context, filename);

        getPluginInfoMap().put(packageName, pluginInfo);
        return pluginInfo;
    }

    private static PluginInfo installPluginFromExternal(Context context, String filename) {
        File externalFile = new File(getExternalPluginDir(context), filename);
        File innerFile = new File(getInnerPluginDir(context), filename);
        if (FileUtils.copyToFile(externalFile, innerFile))
            return installPluginFromInner(context, innerFile.getAbsolutePath());
        else
            return null;
    }

    private static PluginInfo installPluginFromAssets(Context context, String filename) {
        try {
            File innerFile = new File(getInnerPluginDir(context), filename);
            FileUtils.copyToFile(context.getAssets().open(filename), innerFile);
            return installPluginFromInner(context, innerFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static PluginInfo installPluginFromInner(Context context, String path) {
        PluginInfo pluginInfo = new PluginInfo();

        String dexPath = path;
        File dexOutputDir = getInnerPluginDir(context);
        pluginInfo.mClassLoader = new DexClassLoader(dexPath, dexOutputDir.getAbsolutePath(), null, context.getClassLoader());
        String filename = (new File(dexPath)).getName();
        pluginInfo.mDexPath = dexOutputDir + "/" + filename;

        PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(dexPath, 0);
        try {
            Class pluginEntryClass = pluginInfo.mClassLoader.loadClass(packageInfo.packageName + "Entry");
            pluginInfo.mEntryClass = pluginEntryClass;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return pluginInfo;
    }

    private static File getExternalPluginDir(Context context) {
        String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Android/data/" + context.getPackageName() + "/plugins";
        File pluginApkDir = new File(dirPath);
        pluginApkDir.mkdirs();
        return pluginApkDir;
    }

    private static File getInnerPluginDir(Context context) {
        File pluginDexDir = context.getDir("plugins", Context.MODE_PRIVATE);
        return pluginDexDir;
    }

    /*
     * replace resources of activity
     */
    public static void replaceResources(Activity activity, String dexPath) {
        try {
            Resources newResources = createNewResources(activity.getResources(), dexPath);
            Context baseContextInContextThemeWrapper = (Context) getField(activity, ContextThemeWrapper.class, "mBase");
            _replaceResources(baseContextInContextThemeWrapper, null, newResources);
            _replaceResources(activity.getBaseContext(), null, newResources);
            _replaceResources(activity, ContextThemeWrapper.class, newResources);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Resources createNewResources(Resources oldResources, String dexPath) {
        AssetManager assetManager = null;
        try {
            assetManager = AssetManager.class.newInstance();
            invoke(assetManager, "addAssetPath", new Class[]{String.class}, dexPath);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        try {
            Constructor<?> constructor = oldResources.getClass().getConstructor(AssetManager.class, DisplayMetrics.class, Configuration.class);
            return (Resources) constructor.newInstance(assetManager, oldResources.getDisplayMetrics(), oldResources.getConfiguration());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return new Resources(assetManager, oldResources.getDisplayMetrics(), oldResources.getConfiguration());
    }

    private static void _replaceResources(Context context, Class<? extends Context> contextClz, Resources newResources) {
        try {
            setField(context, contextClz, "mResources", newResources);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        try {
            setField(context, contextClz, "mTheme", null);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /*
     * reflect utils
     */
    private static void invoke(Object obj, String methodName, Class[] paramsClz, Object... params) {
        try {
            Method method = obj.getClass().getDeclaredMethod(methodName, paramsClz);
            method.setAccessible(true);
            method.invoke(obj, params);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static Object getField(Object obj, Class clz, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = clz.getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(obj);
        return value;
    }

    private static void setField(Object obj, Class clz, String fieldName, Object value) throws NoSuchFieldException, IllegalAccessException {
        if (clz == null)
            clz = obj.getClass();
        Field field = clz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
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
