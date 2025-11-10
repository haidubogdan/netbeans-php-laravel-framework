/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.preferences;

import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import org.netbeans.modules.nativeexecution.api.ExecutionEnvironment;
import org.netbeans.modules.php.api.phpmodule.PhpModule;
import org.netbeans.modules.php.laravel.LaravelPhpFrameworkProvider;
import org.netbeans.modules.php.laravel.commands.DlightTerminalEnvironment;

/**
 *
 * @author bogdan
 */
public final class LaravelPreferences {

    private static final String APP_DIR = "appDir-path"; // NOI18N
    private static final String ENABLED = "laravel_enabled"; // NOI18N
    private static final String DEFAULT_APP_DIR = "app"; // NOI18N
    private static final String USE_REMOTE_CONNECTION = "use_remote_connection"; // NOI18N
    private static final String USE_DOCKER = "use_docker"; // NOI18N
    private static final String DOCKER_CONTAINER_NAME = "docker_container_name"; // NOI18N
    private static final String DOCKER_BASH_PATH = "docker_bash_path"; // NOI18N
    private static final String DOCKER_WORKDIR = "docker_workdir"; // NOI18N
    private static final String DOCKER_USE_TTY = "docker_use_tty"; // NOI18N
    private static final String DOCKER_USE_INTERACTIVE = "docker_use_interactive"; // NOI18N
    private static final String DOCKER_USER = "docker_user"; // NOI18N
    
    private static final boolean DEFAULT_DOCKER_TTY = true;
    private static final boolean DEFAULT_DOCKER_INTERACTIVE = true;

    private LaravelPreferences() {
    }

    public static void setEnabled(PhpModule module, boolean useDocker) {
        getPreferences(module).putBoolean(ENABLED, useDocker);
    }
    
    public static void setUseDocker(PhpModule module, boolean useDocker) {
        getPreferences(module).putBoolean(USE_DOCKER, useDocker);
    }
    
    public static void setDockerContainerName(PhpModule module, String dockerContainerName) {
        getPreferences(module).put(DOCKER_CONTAINER_NAME, dockerContainerName);
    }

    public static void setDockerBashPath(PhpModule module, String bashPath) {
        getPreferences(module).put(DOCKER_BASH_PATH, bashPath);
    }

    public static void setDockerWorkdir(PhpModule module, String text) {
        getPreferences(module).put(DOCKER_WORKDIR, text);
    }

    public static void setRemoteConnectionFlag(PhpModule module, boolean remoteConnFlag) {
        getPreferences(module).putBoolean(USE_REMOTE_CONNECTION, remoteConnFlag);
    }
    
    public static void setDockerUseTty(PhpModule module, boolean bool) {
        getPreferences(module).putBoolean(DOCKER_USE_TTY, bool);
    }
    
    public static void setDockerUseInteractive(PhpModule module, boolean bool) {
        getPreferences(module).putBoolean(DOCKER_USE_INTERACTIVE, bool);
    }
    
    public static void setDockerUser(PhpModule module, String text) {
        getPreferences(module).put(DOCKER_USER, text);
    }
    
    private static Preferences getPreferences(PhpModule module) {
        return module.getPreferences(LaravelPhpFrameworkProvider.class, true);
    }

    public static String getAppDir(PhpModule module) {
        return getPreferences(module).get(APP_DIR, DEFAULT_APP_DIR);
    }

    public static boolean hasEnabledConfigured(PhpModule module) {
        Preferences pref = getPreferences(module);
        return pref.get(ENABLED, null) != null;
    }

    public static boolean isEnabled(PhpModule module) {
        return getPreferences(module).getBoolean(ENABLED, false);
    }

    public static String getDockerContainerName(PhpModule module) {
        return getPreferences(module).get(DOCKER_CONTAINER_NAME, null);
    }

    public static String getDockerBashPath(PhpModule module) {
        return getPreferences(module).get(DOCKER_BASH_PATH, null);
    }

    public static boolean getRemoteConnectionFlag(PhpModule module) {
        return getPreferences(module).getBoolean(USE_REMOTE_CONNECTION, false);
    }

    public static boolean getUseDocker(PhpModule module) {
        return getPreferences(module).getBoolean(USE_DOCKER, false);
    }

    public static String geDockerWorkdir(PhpModule module) {
        return getPreferences(module).get(DOCKER_WORKDIR, null);
    }
    
    public static boolean getDockerUseTTy(PhpModule module) {
        return getPreferences(module).getBoolean(DOCKER_USE_TTY, DEFAULT_DOCKER_TTY);
    }
        
    public static boolean getDockerUseInteractive(PhpModule module) {
        return getPreferences(module).getBoolean(DOCKER_USE_INTERACTIVE, DEFAULT_DOCKER_INTERACTIVE);
    }
    
    public static String getDockerUser(PhpModule module) {
        return getPreferences(module).get(DOCKER_USER, null);
    }

    public static TerminalComboBoxModel getTerminalEnvAsModel() {
        TerminalComboBoxModel model = new TerminalComboBoxModel();
        model.addElement("No terminal"); // NOI18N
        ExecutionEnvironment env = DlightTerminalEnvironment.getRemoteConfig();
        if (env == null) {
            return model;
        }
        String name = env.getDisplayName();
        model.addElement(name);
        return model;
    }

    public static class TerminalComboBoxModel extends DefaultComboBoxModel<String> {

        /**
         * serializable
         */
        private static final long serialVersionUID = -158789765465878745L;

    }
}
