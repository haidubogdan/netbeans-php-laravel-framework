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

    private static final String APP_DIR = "appDir-path"; // NOI18N
    private static final String DEFAULT_APP_DIR = "app"; // NOI18N
    private static final String DOCKER_CONTAINER_NAME = "docker_container_name"; // NOI18N
    private static final String DOCKER_BASH_PATH = "docker_bash_path"; // NOI18N

    private LaravelPreferences() {
    }

    public static void setAppDir(PhpModule module, String appDir) {
        if (appDir.equals(DEFAULT_APP_DIR)) {
            getPreferences(module).remove(APP_DIR);
        } else {
            getPreferences(module).put(APP_DIR, appDir);
        }
    }

    public static void setDockerContainerName(PhpModule module, String dockerContainerName) {
        getPreferences(module).put(DOCKER_CONTAINER_NAME, dockerContainerName);
    }

    public static void setDockerBashPath(PhpModule module, String bashPath) {
        getPreferences(module).put(DOCKER_BASH_PATH, bashPath);
    }

    private static Preferences getPreferences(PhpModule module) {
        return module.getPreferences(LaravelPhpFrameworkProvider.class, true);
    }

    public static String getAppDir(PhpModule module) {
        return getPreferences(module).get(APP_DIR, DEFAULT_APP_DIR);
    }

    public static String getDockerContainerName(PhpModule module) {
        return getPreferences(module).get(DOCKER_CONTAINER_NAME, null);
    }
    
    public static String getDockerBashPath(PhpModule module) {
        return getPreferences(module).get(DOCKER_BASH_PATH, null);
    }
}
