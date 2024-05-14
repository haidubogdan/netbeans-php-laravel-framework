/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.preferences;

import java.util.prefs.Preferences;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.LaravelPhpFrameworkProvider;

/**
 *
 * @author bogdan
 */
public final class LaravelPreferences {

    private static final String ENABLED = "enabled"; // NOI18N
    private static final String APP_DIR = "appDir-path"; // NOI18N
    private static final String IGNORE_CACHE_DIRECTORY = "cacheDir-ignored"; // NOI18N

    private static final String DEFAULT_APP_DIR = "app"; // NOI18N

    private LaravelPreferences() {
    }

    public static void setAppDir(PhpModule module, String appDir) {
        if (appDir.equals(DEFAULT_APP_DIR)) {
            getPreferences(module).remove(APP_DIR);
        } else {
            getPreferences(module).put(APP_DIR, appDir);
        }
    }

    private static Preferences getPreferences(PhpModule module) {
        return module.getPreferences(LaravelPhpFrameworkProvider.class, true);
    }

    public static String getAppDir(PhpModule module) {
        return getPreferences(module).get(APP_DIR, DEFAULT_APP_DIR);
    }
}
