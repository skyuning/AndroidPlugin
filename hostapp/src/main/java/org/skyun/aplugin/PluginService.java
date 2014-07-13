package org.skyun.aplugin;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.IBinder;

/**
 * Created by linyun on 14-7-10.
 */
public class PluginService extends Service {

    public static final String EXTRA_PLUGIN_PACKAGE = "plugin_package";
    public static final String EXTRA_REAL_INTENT = "real_intent";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        PluginManager.ensureHackInstrumentation(this);
        String pluginPackage = intent.getStringExtra(EXTRA_PLUGIN_PACKAGE);
//        PluginManager.ensureInstallPlugin(this, pluginPackage + ".apk");

        Intent realIntent = intent.getParcelableExtra(EXTRA_REAL_INTENT);
        realIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(realIntent);
        return super.onStartCommand(intent, flags, startId);
    }
}
