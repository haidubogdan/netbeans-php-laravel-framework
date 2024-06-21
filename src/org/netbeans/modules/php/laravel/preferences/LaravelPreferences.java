/*
Licensed to the Apache Software Foundation (ASF)
 */
package org.netbeans.modules.php.laravel.preferences;

import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import org.netbeans.api.annotations.common.CheckForNull;
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
    private static final String USE_REMOTE_CONNECTION = "use_remote_connection";
    private static final String USE_DOCKER = "use_docker";
    private static final String DOCKER_CONTAINER_NAME = "docker_container_name"; // NOI18N
    private static final String DOCKER_BASH_PATH = "docker_bash_path"; // NOI18N
    private static final String PRESCRIPT = "prescript"; // NOI18N
    private static DefaultListModel<String> terminalEnvModel = new DefaultListModel();

    private LaravelPreferences() {
    }

    public static void setDockerContainerName(PhpModule module, String dockerContainerName) {
        getPreferences(module).put(DOCKER_CONTAINER_NAME, dockerContainerName);
    }

    public static void setDockerBashPath(PhpModule module, String bashPath) {
        getPreferences(module).put(DOCKER_BASH_PATH, bashPath);
    }

    public static void setPrescript(PhpModule module, String text) {
        getPreferences(module).put(PRESCRIPT, text);
    }

    public static void setRemoteConnectionFlag(PhpModule module, boolean remoteConnFlag) {
        getPreferences(module).putBoolean(USE_REMOTE_CONNECTION, remoteConnFlag);
    }

    public static void setUseDocker(PhpModule module, boolean useDocker) {
        getPreferences(module).putBoolean(USE_DOCKER, useDocker);
    }

    public static void setEnabled(PhpModule module, boolean useDocker) {
        getPreferences(module).putBoolean(ENABLED, useDocker);
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

    public static String getPreScript(PhpModule module) {
        return getPreferences(module).get(PRESCRIPT, null);
    }

    public static TerminalComboBoxModel getTerminalEnvAsModel() {
        TerminalComboBoxModel model = new TerminalComboBoxModel();
        model.addElement("No terminal");
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
