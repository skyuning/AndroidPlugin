package org.skyun.aplugin;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.ContextThemeWrapper;

import com.example.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import dalvik.system.DexClassLoader;

/**
 * Created by linyun on 14-6-23.
 */
public class PluginManager {

    private static ArrayList<PluginInfo> mPluginInfoList;

    public static ArrayList<PluginInfo> getPluginInfoList() {
        return mPluginInfoList;
    }

    public static PluginInfo testInstallPlugin(Context context, String filename) {
        try {
            File externalFile = new File(getExternalPluginDir(context), filename);
            FileUtils.copyToFile(context.getAssets().open(filename), externalFile);
            return installPlugin(context, filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static PluginInfo installPlugin(Context context, String filename) {
        File externalFile = new File(getExternalPluginDir(context), filename);
        if (externalFile.exists())
            return installPluginFromExternal(context, filename);
        else
            return installPluginFromAssets(context, filename);
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
}
